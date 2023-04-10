package cloud.filibuster.junit;

import cloud.filibuster.exceptions.filibuster.FilibusterGlobalReportWriterException;
import cloud.filibuster.junit.server.core.reports.ServerInvocationAndResponseReport;
import cloud.filibuster.junit.server.core.reports.TestReport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class GlobalFilibusterInterceptor {

    private static GlobalFilibusterInterceptor instance;

    private static final Logger logger = Logger.getLogger(GlobalFilibusterInterceptor.class.getName());
    /**
     * A map where the keys are the display name of the tests and the value is a list of the UUIDs associated with each execution
     */
    private final Hashtable<String, ArrayList<String>> testMap = new Hashtable<>();

    private GlobalFilibusterInterceptor() {
        Thread printingHook = new Thread(this::testSuiteCompleted);
        Runtime.getRuntime().addShutdownHook(printingHook);
        startTestSuite();
    }

    private void startTestSuite() {
        Path directory = Paths.get("/tmp/filibuster");
        try (Stream<Path> filesInDirectoryStream = Files.walk(directory)) {
            //noinspection ResultOfMethodCallIgnored
            filesInDirectoryStream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .filter(file -> file.toString().contains("filibuster-test-"))
                    .forEach(File::delete);
        } catch (NoSuchFileException e){
            //Ignore since it's not there
        }
        catch (IOException e) {
            System.out.println(e.toString());
            throw new FilibusterGlobalReportWriterException("Filibuster failed to delete content in the /tmp/filibuster/ directory ", e);
        }
    }

    private void testSuiteCompleted() {
        logger.info("Test Suite is Completed");

        ServerInvocationAndResponseReport.writeServerInvocationReport();

        ServerInvocationAndResponseReport.writeServiceProfile();
    }

    public static GlobalFilibusterInterceptor getInstance() {
        if (instance == null) {
            instance = new GlobalFilibusterInterceptor();
        }
        return instance;
    }

    public final void addTestReport(TestReport testReport) {
//        String testname =
    }

}
