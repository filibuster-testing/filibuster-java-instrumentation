package cloud.filibuster.junit.server.core.reports;

import brave.internal.Nullable;
import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import cloud.filibuster.instrumentation.helpers.Property;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TestSuiteReport {

    static class Keys {
        private static final String REPORTS_KEY = "reports";

        static class TestReportKeys {
            private static final String STATUS = "status";
            private static final String TEST_PATH = "path";
            private static final String TEST_NAME = "test_name";
            private static final String CLASS_NAME = "class_name";
        }

    }

    private static class FilibusterTestReportSummary {
        private final String testName;
        private final String className;
        private final File testPath;
        private final boolean status;


        public FilibusterTestReportSummary(String testName, File testPath, boolean status, String className) {
            this.testName = testName;
            this.testPath = testPath;
            this.status = status;
            this.className = className;
        }
    }

    private static final Workbook workbook = new XSSFWorkbook();

    @Nullable
    private static Sheet workbookSheet;

    @Nullable
    private static CellStyle workbookCellStyle;

    private static int workbookRowNumber = 0;

    private static TestSuiteReport instance;

    private static final Logger logger = Logger.getLogger(TestSuiteReport.class.getName());
    /**
     * A map where the keys are the display name of the tests and the value is a list of the UUIDs associated with each execution
     */
    private final ArrayList<FilibusterTestReportSummary> testReportSummaries = new ArrayList<>();

    public static synchronized TestSuiteReport getInstance() {
        if (instance == null) {
            instance = new TestSuiteReport();
        }
        return instance;
    }

    private TestSuiteReport() {
        if(Property.getReportsTestSuiteReportEnabledProperty()) {
            Thread testSuiteCompleteHook = new Thread(this::testSuiteCompleted);
            Runtime.getRuntime().addShutdownHook(testSuiteCompleteHook);
            startTestSuite();
            initializeWorkbookAndSheet();
        }
    }

    private void startTestSuite() {
        try (Stream<Path> filesInDirectoryStream = Files.walk(ReportUtilities.getBaseDirectoryPath().toPath())) {
            //noinspection ResultOfMethodCallIgnored
            filesInDirectoryStream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .filter(file -> file.toString().contains("filibuster-test-"))
                    .forEach(File::delete);
        } catch (NoSuchFileException e) {
            //Ignore since it's not there
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to delete content in the /tmp/filibuster/ directory ", e);
        }

        writeOutPlaceholder();
    }

    private void testSuiteCompleted() {

        ServerInvocationAndResponseReport.writeServerInvocationReport();

        ServerInvocationAndResponseReport.writeServiceProfile();

        writeOutReports();
        writeExcelFile();
    }

    private static JSONObject getTestReportSummaryJSON(FilibusterTestReportSummary testReportSummary) {
        JSONObject reportJSON = new JSONObject();
        reportJSON.put(Keys.TestReportKeys.TEST_PATH, testReportSummary.testPath);
        reportJSON.put(Keys.TestReportKeys.TEST_NAME, testReportSummary.testName);
        reportJSON.put(Keys.TestReportKeys.STATUS, testReportSummary.status);
        reportJSON.put(Keys.TestReportKeys.CLASS_NAME, testReportSummary.className);
        return reportJSON;
    }

    private JSONObject getReportsJSON() {
        JSONObject reportsJSON = new JSONObject();
        List<JSONObject> jsonReports = testReportSummaries.stream()
                .map(TestSuiteReport::getTestReportSummaryJSON).collect(Collectors.toList());
        reportsJSON.put(Keys.REPORTS_KEY, jsonReports);
        return reportsJSON;
    }

    private void writeOutPlaceholder() {
        File directory = ReportUtilities.getBaseDirectoryPath();
        File indexPath = new File(directory, "index.html");

        try {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        } catch (SecurityException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test suite report placeholder: ", e);
        }

        try {
            Path constructionGifPath = Paths.get(directory + "/construction.gif");
            byte[] constructionGifBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(), "html/test_suite_report/construction.gif");
            Files.write(constructionGifPath, constructionGifBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test suite report: ", e);
        }

        try {
            byte[] indexBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(), "html/test_suite_report/waiting.html");
            Files.write(indexPath.toPath(), indexBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test suite report: ", e);
        }
    }

    private void writeOutReports() {
        File directory = ReportUtilities.getBaseDirectoryPath();
        File scriptFile = new File(directory, "summary.js");
        try {
            Files.write(scriptFile.toPath(), ("var summary = " + getReportsJSON().toString(4) + ";")
                    .getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test suite report: ", e);
        }
        // Write out index file.
        Path indexPath = Paths.get(directory + "/index.html");
        try {
            byte[] indexBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(), "html/test_suite_report/index.html");
            Files.write(indexPath, indexBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution report: ", e);
        }

        logger.info( "\n" +
                        "[FILIBUSTER-CORE]: Test Suite Report written to file://" + indexPath + "\n");

    }

    public void addTestReport(TestReport testReport) {
        String testName = testReport.getTestName();
        String className = testReport.getClassName();
        File testPath = testReport.getReportPath();
        ArrayList<TestExecutionReport> testExecutionReports = testReport.getTestExecutionReports();
        boolean hasNoFailures = testExecutionReports.stream().map(TestExecutionReport::isTestExecutionPassed)
                .reduce(true, (curr, next) -> curr && next);
        testReportSummaries.add(new FilibusterTestReportSummary(testName, testPath, hasNoFailures,className));
        addToWorkbook(testExecutionReports);
    }

    private static void addToWorkbook(List<TestExecutionReport> testExecutionReports) {
        for (TestExecutionReport ter : testExecutionReports) {
            if (! ter.isTestExecutionPassed()) {
                workbookRowNumber++;

                Row row = workbookSheet.createRow(workbookRowNumber);

                Cell cell = row.createCell(0);
                cell.setCellValue(ter.getClassName());
                cell.setCellStyle(workbookCellStyle);

                cell = row.createCell(1);
                cell.setCellValue(ter.getTestName());
                cell.setCellStyle(workbookCellStyle);

                cell = row.createCell(2);
                cell.setCellValue(ter.getFailures().get(0).getAssertionFailureMessage());
                cell.setCellStyle(workbookCellStyle);

                cell = row.createCell(3);
                cell.setCellValue(ter.getFailures().get(0).getAssertionFailureStackTrace());
                cell.setCellStyle(workbookCellStyle);
            }
        }
    }

    private static void initializeWorkbookAndSheet() {
        workbookSheet = workbook.createSheet("Failures");
        workbookSheet.setColumnWidth(0, 20000);
        workbookSheet.setColumnWidth(1, 20000);
        workbookSheet.setColumnWidth(2, 20000);
        workbookSheet.setColumnWidth(3, 40000);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header = workbookSheet.createRow(0);

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Class");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(1);
        headerCell.setCellValue("Test");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(2);
        headerCell.setCellValue("Failure Message");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(3);
        headerCell.setCellValue("Failure Stack Trace");
        headerCell.setCellStyle(headerStyle);

        workbookCellStyle = workbook.createCellStyle();
        workbookCellStyle.setWrapText(true);
    }

    private static void writeExcelFile() {
        String fileLocation = "/tmp/filibuster/failures.xlsx";
        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(fileLocation);
        } catch (FileNotFoundException e) {
            throw new FilibusterTestReportWriterException("failed to open excel file for writing", e);
        }

        if (outputStream != null) {
            try {
                workbook.write(outputStream);
            } catch (IOException e) {
                throw new FilibusterTestReportWriterException("failed to write excel file", e);
            }
        }

        try {
            workbook.close();
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("failed to close workbook while writing excel file", e);
        }
    }
}
