package morning.dsl

import morning.dsl.testclasses.Test1
import morning.dsl.testclasses.Test2
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by osklyarenko on 3/8/15.
 */
class DslSpec extends Specification {

    @Shared
    GroovyShell shell

    void setup() {
        def config = new CompilerConfiguration()

        def imports = new ImportCustomizer()
        imports.addStarImports("morning.dsl")

        config.addCompilationCustomizers(imports)


        shell = new GroovyShell(config)
    }

    Script setupDslScript(String script) {
        def dslScript = shell.parse(script)

        dslScript.binding = new Binding([
                dsl: new Dsl([].iterator()),
                junitTestClasses: [].iterator(),
        ])

        return dslScript
    }

    def "test empty script"() {
        when: 'empty script is passed'

            Dsl.Suite suite = setupDslScript(script).run()
        then: 'everything suite is null'

            noExceptionThrown()

            null == suite

        where:

            script | moduleName | testClasses | _
            ''     | 'test-module' | [Test1] | _
    }

    @Shared def SCRIPT_EMPTY_SCRIPT = """
Dsl.extendString(dsl)

'EMP-1231'.suite {
}
"""

    def "test empty suite"() {
        when: 'empty suite definition is passed'

            Dsl.Suite suite = setupDslScript(script).run()
        then: 'resulting suite is empty'

            noExceptionThrown()

            null != suite
        where:

            script | moduleName | testClasses | _
            SCRIPT_EMPTY_SCRIPT | 'test-module' | [Test1] | _
    }

    @Shared def SCRIPT_EXPLICIT_MODULE_INCLUDE = """
Dsl.extendString(dsl)

'EMP-1231'.suite {
    +'test-module'.module
}
"""

    def "test single included module"() {
        when: 'suite definition is passed'
            Dsl.Suite suite = setupDslScript(script).run()

        then: 'one test is registered'
            noExceptionThrown()

            testClasses.every {
                suite.ownsTest(moduleName, it)
            }
        where:
            script | moduleName | testClasses
            SCRIPT_EXPLICIT_MODULE_INCLUDE | 'test-module' | []
            SCRIPT_EXPLICIT_MODULE_INCLUDE | 'test-module' | [Test1]
            SCRIPT_EXPLICIT_MODULE_INCLUDE | 'test-module' | [Test1, Test2]
    }

    @Shared def SCRIPT_WRONG_MODULE_INCLUDE = """
Dsl.extendString(dsl)

'EMP-1231'.suite {
    +'wrong-module'.module
}
"""

    def "test wrongly included module"() {
        when: 'suite definition is passed'
            Dsl.Suite suite = setupDslScript(script).run()

        then: 'one test is registered'
            noExceptionThrown()

            testClasses.every {
                !suite.ownsTest(moduleName, it)
            }
        where:
            script | moduleName | testClasses
            SCRIPT_WRONG_MODULE_INCLUDE | 'test-module' | []
            SCRIPT_WRONG_MODULE_INCLUDE | 'test-module' | [Test1]
            SCRIPT_WRONG_MODULE_INCLUDE | 'test-module' | [Test1, Test2]
    }

    @Shared def SCRIPT_WRONG_MODULE_EXCLUDE = """
Dsl.extendString(dsl)

'EMP-1231'.suite {
    -'wrong-module'.module
}
"""
    def "test wrongly excluded module"() {
        when: 'suite definition is passed'
            Dsl.Suite suite = setupDslScript(script).run()

        then: 'one test is registered'
            noExceptionThrown()

            testClasses.every {
                suite.ownsTest(moduleName, it)
            }
        where:
            script | moduleName | testClasses
            SCRIPT_WRONG_MODULE_EXCLUDE | 'test-module' | []
            SCRIPT_WRONG_MODULE_EXCLUDE | 'test-module' | [Test1]
            SCRIPT_WRONG_MODULE_EXCLUDE | 'test-module' | [Test1, Test2]
    }

    @Shared def SCRIPT_EXPLICIT_MODULE_EXCLUDE = """
Dsl.extendString(dsl)

'EMP-1231'.suite {
    -'ignored-module'.module
}
"""

    def "test explicit module exclude"() {
        when: 'suite definition is passed'
            Dsl.Suite suite = setupDslScript(script).run()

        then: 'one test is registered'
            noExceptionThrown()

            testClasses.every {
                !suite.ownsTest(moduleName, it)
            }
        where:
            script | moduleName | testClasses
            SCRIPT_EXPLICIT_MODULE_EXCLUDE | 'ignored-module' | []
            SCRIPT_EXPLICIT_MODULE_EXCLUDE | 'ignored-module' | [Test1]
            SCRIPT_EXPLICIT_MODULE_EXCLUDE | 'ignored-module' | [Test1, Test2]
    }

    @Shared def SCRIPT_IMPLICIT_MODULE_INCLUDE = """
Dsl.extendString(dsl)

'EMP-1231'.suite {
    -'ignored-module'.module
}
"""

    def "test implicit module include"() {
        when: 'suite definition is passed'
            Dsl.Suite suite = setupDslScript(script).run()

        then: 'one test is registered'
            noExceptionThrown()

            expectedResult == testClasses.every {
                suite.ownsTest(moduleName, it)
            }
        where:
            script | moduleName | testClasses | expectedResult
            SCRIPT_IMPLICIT_MODULE_INCLUDE | 'test-module'      | []                | true
            SCRIPT_IMPLICIT_MODULE_INCLUDE | 'test-module'      | [Test1]           | true
            SCRIPT_IMPLICIT_MODULE_INCLUDE | 'test-module'      | [Test1, Test2]    | true
    }

    @Shared def SCRIPT_MIXED_EXPLICIT_IMPLICIT_MODULE_INCLUDE = """
Dsl.extendString(dsl)

'EMP-1231'.suite {
    -'ignored-module'.module
    +'test-module'.module
}
"""

    def "test mixed explicit module includes/excludes"() {
        when: 'suite definition is passed'
            Dsl.Suite suite = setupDslScript(script).run()

        then: 'one test is registered'
            noExceptionThrown()

            expectedResult == testClasses.every {
                suite.ownsTest(moduleName, it)
            }
        where:
            script | moduleName | testClasses | expectedResult
            SCRIPT_MIXED_EXPLICIT_IMPLICIT_MODULE_INCLUDE | 'test-module'      | []                | true
            SCRIPT_MIXED_EXPLICIT_IMPLICIT_MODULE_INCLUDE | 'test-module'      | [Test1]           | true
            SCRIPT_MIXED_EXPLICIT_IMPLICIT_MODULE_INCLUDE | 'test-module'      | [Test1, Test2]    | true
            SCRIPT_MIXED_EXPLICIT_IMPLICIT_MODULE_INCLUDE | 'ignored-module'   | []                | true
            SCRIPT_MIXED_EXPLICIT_IMPLICIT_MODULE_INCLUDE | 'ignored-module'   | [Test1]           | false
            SCRIPT_MIXED_EXPLICIT_IMPLICIT_MODULE_INCLUDE | 'ignored-module'   | [Test1, Test2]    | false
    }
}
