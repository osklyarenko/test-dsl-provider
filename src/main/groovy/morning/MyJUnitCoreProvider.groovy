package morning

import morning.dsl.Dsl
import org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker
import org.apache.maven.surefire.common.junit48.JUnit48Reflector
import org.apache.maven.surefire.junitcore.NonConcurrentRunListener
import org.apache.maven.surefire.providerapi.AbstractProvider
import org.apache.maven.surefire.providerapi.ProviderParameters
import org.apache.maven.surefire.report.ConsoleLogger
import org.apache.maven.surefire.report.ConsoleOutputCapture
import org.apache.maven.surefire.report.ReporterFactory
import org.apache.maven.surefire.suite.RunResult
import org.apache.maven.surefire.testset.TestSetFailedException
import org.apache.maven.surefire.util.RunOrderCalculator
import org.apache.maven.surefire.util.ScanResult
import org.apache.maven.surefire.util.ScannerFilter
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * Created by osklyarenko on 3/1/15.
 */
class MyJUnitCoreProvider extends AbstractProvider {
    private final ClassLoader testClassLoader;

    private final ScannerFilter defaultFilter;

    private final List<org.junit.runner.notification.RunListener> customRunListeners;

    private final ProviderParameters providerParameters;

    private JUnit48Reflector jUnit48Reflector;

    private RunOrderCalculator runOrderCalculator;

    private String requestedTestMethod;

    private final ScanResult scanResult;

    private Script dslScript;
    private boolean filterIsProvided
    private String filterFile

    MyJUnitCoreProvider(ProviderParameters providerParameters) {
        this.providerParameters = providerParameters;
        this.testClassLoader = providerParameters.getTestClassLoader();
        this.scanResult = providerParameters.getScanResult();
        this.runOrderCalculator = providerParameters.getRunOrderCalculator();
        this.defaultFilter = new JUnit4TestChecker(testClassLoader);
        this.requestedTestMethod = providerParameters.getTestRequest().getRequestedTestMethod();

        filterFile = System.getProperty('testFilterDsl')
        filterIsProvided = null != filterFile

        if (filterIsProvided) {
            customRunListeners = JUnit4RunListenerFactory.
                    createCustomListeners(providerParameters.getProviderProperties().getProperty("listener"));
            jUnit48Reflector = new JUnit48Reflector(testClassLoader);

            def config = new CompilerConfiguration()
            def imports = new ImportCustomizer()
            imports.addStarImports("morning.dsl")

            config.addCompilationCustomizers(imports)


            def dslText = new File(System.getProperty('testFilterDsl')).text
            def shell = new GroovyShell(config)


            this.dslScript = shell.parse("""
Dsl.extendString(dsl)

$dslText
""")
        }
    }

    @Override
    Iterator getSuites() {
        return [].iterator()
    }

    @Override
    RunResult invoke(Object o) {
        final ConsoleLogger consoleLogger = providerParameters.getConsoleLogger();
        final ReporterFactory reporterFactory = providerParameters.getReporterFactory();

        def junitTests = scanResult.applyFilter(defaultFilter, testClassLoader);

        if (filterIsProvided) {
            consoleLogger.info 'Hello DSL Morning\n'
            consoleLogger.info "In module ${currentModule()}\n"


            dslScript.binding = new Binding([
                    dsl             : new Dsl(junitTests.iterator()),
                    junitTestClasses: junitTests.iterator(),
            ])


            Dsl.Suite testSuite = dslScript.run()

            org.junit.runner.notification.RunListener jUnit4RunListener = this.getRunListener(reporterFactory);
            customRunListeners.add(0, jUnit4RunListener);

            def module = currentModule()
            def customFilter = new MyScannerFilter(defaultFilter, { Class testClass ->
                boolean owns = testSuite.ownsTest(module, testClass)

                if (owns) {
                    consoleLogger.info "[FILTER] Included test ${testClass.name}\n"
                } else {
                    consoleLogger.info "[FILTER] Skipped test ${testClass.name}\n"
                }

                return owns
            }, consoleLogger)

            junitTests = scanResult.applyFilter(customFilter, testClassLoader);
        }

        MyJUnitCoreWrapper.execute(junitTests, customRunListeners, null);
        return reporterFactory.close();
    }

    private org.junit.runner.notification.RunListener getRunListener(ReporterFactory reporterFactory) throws TestSetFailedException {
        Object jUnit4RunListener;

        NonConcurrentRunListener testSetMap = new NonConcurrentRunListener(reporterFactory.createReporter());
        ConsoleOutputCapture.startCapture(testSetMap);
        jUnit4RunListener = testSetMap;

        return (org.junit.runner.notification.RunListener)jUnit4RunListener;
    }

    private String currentModule() {

        File moduleDir = new File(providerParameters.providerProperties.testClassesDirectory).parentFile.parentFile

        return moduleDir.name
    }

    static class MyScannerFilter implements ScannerFilter {

        @Delegate
        ScannerFilter junitFilter

        Closure customFilterStrategy

        ConsoleLogger logger;

        MyScannerFilter(ScannerFilter junitFilter, Closure customFilterStrategy, ConsoleLogger logger) {
            this.junitFilter = junitFilter
            this.customFilterStrategy = customFilterStrategy
            this.logger = logger
        }

        @Override
        boolean accept(Class aClass) {
            def junitOk = junitFilter.accept(aClass)
            def extensionOk = customFilterStrategy(aClass)

            return junitOk && extensionOk
        }
    }
}
