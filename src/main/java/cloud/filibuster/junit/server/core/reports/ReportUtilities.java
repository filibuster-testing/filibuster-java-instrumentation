package cloud.filibuster.junit.server.core.reports;

import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ReportUtilities {
    public static File GetBaseDirectoryPath() {
        return new File("/tmp/filibuster/");
    }

    public static byte[] getResourceAsBytes(ClassLoader classLoader, String fileName) {
        byte[] targetArray;
        try (InputStream resource = classLoader.getResourceAsStream(fileName)) {

            if (resource == null) {
                throw new FilibusterTestReportWriterException("Filibuster failed to open resource file because it is null; this is possibly a file not found for file: " + fileName);
            }

            targetArray = new byte[resource.available()];
            resource.read(targetArray);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to open resource file because of exception; this is possibly a file not found for file: " + fileName, e);
        }
        return targetArray;
    }
}
