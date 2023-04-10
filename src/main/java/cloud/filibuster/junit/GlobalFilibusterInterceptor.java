package cloud.filibuster.junit;

import cloud.filibuster.junit.server.core.reports.TestReport;

import java.util.ArrayList;
import java.util.Hashtable;

public class GlobalFilibusterInterceptor {

    private static final GlobalFilibusterInterceptor instance = new GlobalFilibusterInterceptor();

    /**
     * A map where the keys are the display name of the tests and the value is a list of the UUIDs associated with each execution
     */
    private final Hashtable<String, ArrayList<String>> testMap  = new Hashtable<>();

    private GlobalFilibusterInterceptor(){
    }

    public static GlobalFilibusterInterceptor getInstance() {
        return instance;
    }

    public final void addTestReport(TestReport testReport)
    {
//        String testname =
    }

}
