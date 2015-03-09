package morning

import org.apache.maven.surefire.common.junit4.JUnit4RunListener
import org.apache.maven.surefire.testset.TestSetFailedException
import org.apache.maven.surefire.util.TestsToRun
import org.junit.runner.JUnitCore
import org.junit.runner.Request
import org.junit.runner.manipulation.Filter
import org.junit.runner.notification.RunListener

/**
 * Created by osklyarenko on 3/1/15.
 */
class MyJUnitCoreWrapper {
    public static void execute(TestsToRun testsToRun, List<RunListener> listeners, Filter filter) throws TestSetFailedException {
        JUnitCore junitCore = new JUnitCore();

        listeners.each {
            junitCore.addListener(it);
        }

        Request req1 = Request.classes(testsToRun.getLocatedClasses());
        if (filter != null) {
            req1 = req1.filterWith(filter)
        }


        try {
            JUnit4RunListener.rethrowAnyTestMechanismFailures(junitCore.run(req1));
        } catch (TestSetFailedException junitError) {

        }
        finally {
            listeners.each {
                junitCore.removeListener(it);
            }
        }
    }
}
