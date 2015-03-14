package morning.dsl

import com.google.common.collect.HashBiMap
import groovy.transform.ToString

/**
 * Created by osklyarenko on 3/1/15.
 */
public class Dsl {

    private Map<String, List<String>> includedTestsByModule = [:].withDefault { key ->
        return []
    }

    private Map<String, List<String>> excludedTestsByModule = [:].withDefault { key ->
        return []
    }

    private Map<String, Module> modulesByName = [:].withDefault {
        return []
    }

    private Map<String, Module> includedModulesByName = [:].withDefault {
        return []
    }

    private Map<String, Module> excludedModulesByName = [:].withDefault {
        return []
    }

    private Map<String, List<Module>> modulesBySuite = [:].withDefault { key ->
        return []
    }

    private Map<String, Pkg> packagesByName = [:]

    private Map<String, Pkg> includedPackagesByName = [:]
    private Map<String, Pkg> excludedPackagesByName = [:]

    private Map<String, Suite> suitesByName = [:]

    private Suite activeSuite
    private Module activeModule

    private Iterator<Class> junitTestClasses
    private Expando sharedCtx = new Expando()


    Dsl(Iterator<Class> junitTestClasses) {
        this.junitTestClasses = junitTestClasses

        def map = HashBiMap.create()

        junitTestClasses.each { Class testClass ->
            map[testClass.name] = testClass.simpleName
        }

        sharedCtx.testClassMap = map
    }

    def includeModule(String moduleName) {
        includedModulesByName[moduleName] = modulesByName[moduleName]
    }

    boolean isModuleIncluded(String moduleName) {
        return includedModulesByName.containsKey(moduleName)

    }

    def excludeModule(String moduleName) {
        excludedModulesByName[moduleName] = modulesByName[moduleName]
    }

    boolean anyModulesIncluded() {
        return !includedModulesByName.isEmpty()
    }

    boolean anyModulesExcluded() {
        return !excludedModulesByName.isEmpty()
    }

    boolean isModuleExcluded(String moduleName) {
        return excludedModulesByName.containsKey(moduleName)
    }

    def includePackage(String packageName) {
        includedPackagesByName[packageName] = packagesByName[packageName]
    }

    def excludePackage(String packageName) {
        excludedPackagesByName[packageName] = packagesByName[packageName]
    }

    boolean isPackageIncluded(Class testClass) {
        Package classPackage = testClass.package

        if (null == classPackage) {
            return !anyPackagesIncluded()
        }

        def testClassPackage = testClass.package.name
        def matchedPkgs = includedPackagesByName.keySet().findAll { pkg ->
            testClassPackage.startsWith pkg
        }

        return !matchedPkgs.isEmpty()
    }

    boolean anyPackagesIncluded() {
        return !includedPackagesByName.isEmpty()
    }

    boolean anyPackagesExcluded() {
        return !excludedPackagesByName.isEmpty()
    }

    boolean isPackageExcluded(Class testClass) {
        Package classPackage = testClass.package
        if (null == classPackage) {
            return false
        }

        def testClassPackage = testClass.package.name

        def matchedPkgs = excludedPackagesByName.keySet().findAll { pkg ->
            testClassPackage.startsWith pkg
        }

        matchedPkgs.each { pkg ->
            println "Test '$testClass.name' belongs to excluded package '$pkg'"
        }

        return !matchedPkgs.isEmpty()
    }

    def includeTest(String test) {
        if (null == activeModule) {
            println 'ERROR active module is not set'
        }
        includedTestsByModule[activeModule.name] << test

        println "\t\tAdded test '$test' to suite '$activeSuite.name'"
    }

    def excludeTest(String test) {
        if (null == activeModule) {
            println 'ERROR active module is not set'
        }

        excludedTestsByModule[activeModule.name] << test

        println "\t\tExcluded test '$test' from suite '$activeSuite.name'"
    }

    Suite compileSuite(Suite suite) {

        def modules = modulesBySuite[suite.name]

        modules.each { module ->

            suite.addModule module

            includedTestsByModule[module.name].each { test ->
                module.includeTest test
            }

            excludedTestsByModule[module.name].each { test ->
                module.excludeTest test
            }
        }

        return suite
    }

    def registerSuite(Suite suite) {
        suitesByName[suite.name] = suite

        activeSuite = suite

        println "Registered suite '$suite.name'"
    }

    def registerPackage(Pkg pkg) {

        packagesByName[pkg.name] = pkg

        println "\tRegistered package '$pkg.name'"
    }

    def registerModule(Module module) {
        modulesByName[module.name] = module
        modulesBySuite[activeSuite.name] << module

        activeModule = module

        println "\tRegistered module '$module.name'"
    }

    @ToString(includes = ['name'])
    public static class Suite {

        String name

        Map<String, Module> modulesByName = [:]

        Dsl dsl

        public Suite(String name, Dsl dsl) {
            this.name = name
            this.dsl = dsl
        }

        def addModule(Module module) {
            this.modulesByName[module.name] = module
        }

        boolean ownsTest(String moduleName, Class testClass) {

            if (dsl.anyPackagesExcluded()) {
                if (dsl.isPackageExcluded(testClass)) {
                    return false
                }
            }

            if (dsl.anyPackagesIncluded()) {
                if (!dsl.isPackageIncluded(testClass)) {
                    return false
                }
            }

            if (dsl.anyModulesExcluded()) {
                if (dsl.isModuleExcluded(moduleName)) {
                    return false
                }
            }

            if (dsl.anyModulesIncluded()) {
                if (!dsl.isModuleIncluded(moduleName)) {
                    return false
                }
            }

            if (dsl.isModuleIncluded(moduleName)) {
                def module = modulesByName[moduleName]

                if (null == module) {
                    return true
                }

                return module.ownsTest(testClass)
            }

            return true
        }
    }

    @ToString(includes = ['name'])
    public static class Pkg {
        String name

        Dsl dsl

        Pkg(String name, Dsl dsl) {
            this.name = name
            this.dsl = dsl
        }

        Pkg positive() {
            dsl.includePackage this.name
        }

        Pkg negative() {
            dsl.excludePackage this.name
        }
    }

    @ToString(includes = ['name'])
    public static class Module {

        String name
        List<String> tests = []
        List<String> excludedTests = []

        Dsl dsl

        public Module(String name, Dsl dsl) {
            this.name = name
            this.dsl = dsl
        }

        def includeTest(String name) {
            tests << name
        }

        def excludeTest(String name) {
            excludedTests << name
        }

        boolean ownsTest(Class testClass) {

            def excluded = excludedTests.any {
                it == testClass.simpleName || it == testClass.name
            }

            if (excluded) {
                return false
            }

            if (tests.empty) {
                return true
            }

            return tests.any {
                it == testClass.simpleName || it == testClass.name
            }
        }

        Module positive() {
            dsl.includeModule this.name
        }

        Module negative() {
            dsl.excludeModule this.name
        }
    }

    static extendString(Dsl dsl) {
        String.metaClass.with {

            suite = { Closure action ->
                def suite = new Suite(delegate, dsl);

                action.delegate = suite
                action.resolveStrategy = Closure.DELEGATE_FIRST

                dsl.registerSuite(suite)
                action()

                return dsl.compileSuite(suite)
            }

            pkg = { Closure action ->
                def pkg = new Pkg(delegate, dsl)

                dsl.registerPackage(pkg)
                action()

                return pkg
            }

            module = { Closure action ->
                def module = new Module(delegate, dsl);

                action.delegate = module;
                action.resolveStrategy = Closure.DELEGATE_FIRST;

                dsl.registerModule(module)
                action()

                return module;
            }

            positive = {
                dsl.includeTest(delegate)
            }

            negative = {
                dsl.excludeTest(delegate)
            }

            propertyMissing = { String name ->
                if (name in ['module', 'suite', 'pkg']) {

                    return delegate."$name"({})
                }

                throw new MissingPropertyException(name, String)

            }
        }
    }

}
