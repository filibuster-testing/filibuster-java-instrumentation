package cloud.filibuster.instrumentation.datatypes;

import cloud.filibuster.exceptions.filibuster.FilibusterCallsiteGenerationException;
import cloud.filibuster.exceptions.filibuster.FilibusterUnknownCallsiteException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cloud.filibuster.instrumentation.helpers.Property.getCallsiteLineNumberProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getCallsiteRemoveImportsFromStackTraceProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getCallsiteStackTraceWhitelistProperty;

/**
 * Generate a callsite that is used to in the generation of a distributed execution index.
 */
public class Callsite {
    private static final Logger logger = Logger.getLogger(Callsite.class.getName());

    private final static ArrayList<String> standardImportedLibraries = new ArrayList<>();
    private final static ArrayList<String> importedLibrariesFromGradle = new ArrayList<>();

    private final String serviceName;
    private final String classOrModuleName;
    private final String methodOrFunctionName;
    private final CallsiteArguments serializedArguments;
    private final String serializedStackTrace;
    private final String fileName;
    private final String lineNumber;

    private final ArrayList<Map.Entry<String, String>> filteredStackTrace = new ArrayList<>();

    static {
        standardImportedLibraries.add("jdk.internal");
        standardImportedLibraries.add("java.base");
        standardImportedLibraries.add("org.junit");
        standardImportedLibraries.add("org.gradle");
        standardImportedLibraries.add("com.linecorp.armeria");
        standardImportedLibraries.add("com.sun");
        standardImportedLibraries.add("io.netty");
        standardImportedLibraries.add("io.grpc");

        generateImportedLibrariesListFromGradle();
    }

    /**
     * Generate a callsite that is used to in the generation of a distributed execution index.
     *
     * @param serviceName service name that is being invoked.
     * @param classOrModuleName class or module name of the stub that is being used during the invocation.
     * @param methodOrFunctionName remote RPC method that is being invoked.
     * @param serializedArguments serialized arguments supplied to the RPC method.
     */
    public Callsite(
        String serviceName,
        String classOrModuleName,
        String methodOrFunctionName,
        CallsiteArguments serializedArguments
    ) {
        this.serviceName = serviceName;
        this.classOrModuleName = classOrModuleName;
        this.methodOrFunctionName = methodOrFunctionName;
        this.serializedArguments = serializedArguments;

        computeFilteredStackTrace();

        // If we have no frames, just abort everything.
        if (filteredStackTrace.size() == 0) {
            throw new FilibusterUnknownCallsiteException("Filibuster cannot determine the callsite of the remote request.");
        }

        // When we serialize, should we use hash codes instead?
        ArrayList<String> arraySerializedArguments = new ArrayList<>();
        for (Map.Entry<String, String> filteredStackTraceElement : filteredStackTrace) {
            arraySerializedArguments.add(filteredStackTraceElement.getValue());
        }
        this.serializedStackTrace = String.join("", arraySerializedArguments);

        // Ensure we reverse frames so 0 is the first frame.
        Collections.reverse(filteredStackTrace);

        // Get last element and compute callsite file name and line number.
        Map.Entry<String, String> lastStackTraceElement = filteredStackTrace.get(0);
        String lastStackTraceElementString = lastStackTraceElement.getValue();

        try {
            this.fileName = lastStackTraceElementString.substring(lastStackTraceElementString.indexOf('(') + 1, lastStackTraceElementString.indexOf(':'));
        } catch (StringIndexOutOfBoundsException e) {
            for (Map.Entry<String, String> filteredStackTraceElement : filteredStackTrace) {
                logger.log(Level.WARNING, filteredStackTraceElement.getValue());
            }

            logger.log(Level.SEVERE, "lastStackTraceElementString: " + lastStackTraceElementString);

            throw e;
        }


        if (getCallsiteLineNumberProperty()) {
            this.lineNumber = lastStackTraceElementString.substring(lastStackTraceElementString.indexOf(':') + 1, lastStackTraceElementString.indexOf(')'));
        } else {
            this.lineNumber = "0";
        }
    }

    /**
     * Return the service name.
     *
     * @return service name.
     */
    public String getServiceName() {
        return serviceName;
    }

    public String getParameterList() {
        return "[]";
    }

    /**
     * Return class or module name.
     *
     * @return class or module name.
     */
    public String getClassOrModuleName() {
        return classOrModuleName;
    }

    /**
     * Return method or function name.
     *
     * @return method or function name.
     */
    public String getMethodOrFunctionName() {
        return methodOrFunctionName;
    }

    /**
     * Return the serialized arguments.
     *
     * TODO: Rename me.
     *
     * @return string of serialized arguments.
     */
    public CallsiteArguments getSerializedArguments() {
        return serializedArguments;
    }

    /**
     * Return the serialized stack trace.
     *
     * @return string of serialized stack trace.
     */
    public String getSerializedStackTrace() {
        return serializedStackTrace;
    }

    /**
     * Return filename of the RPC invocation site.
     *
     * @return return the filename of the RPC invocation site.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Return line number of the RPC invocation site.
     *
     * @return return the line number of the RPC invocation site.
     */
    public String getLineNumber() {
        return lineNumber;
    }

    private void computeFilteredStackTrace() {
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        for (StackTraceElement stackTraceElement : ste) {
            int hashCode = stackTraceElement.hashCode();
            String stringStackTraceElement = stackTraceElement.toString();

            boolean foundInStandardImportedLibraries = false;

            for (String i : standardImportedLibraries) {
                if (stringStackTraceElement.contains(i)) {
                    foundInStandardImportedLibraries = true;
                    break;
                }
            }

            boolean foundInImportedLibrariesFromGradle = false;

            if (getCallsiteRemoveImportsFromStackTraceProperty()) {
                for (String i : importedLibrariesFromGradle) {
                    if (stringStackTraceElement.contains(i)) {
                        foundInImportedLibrariesFromGradle = true;
                        break;
                    }
                }
            }

            boolean notFilibusterOrFilibusterTest =
                    !stringStackTraceElement.contains("cloud.filibuster") ||
                    (stringStackTraceElement.contains("cloud.filibuster") && (stringStackTraceElement.contains("test") || stringStackTraceElement.contains("tutorial")));

            if (! foundInStandardImportedLibraries && ! foundInImportedLibrariesFromGradle && notFilibusterOrFilibusterTest) {
                filteredStackTrace.add(Pair.of(String.valueOf(hashCode), stringStackTraceElement));
            }
        }
    }

    @SuppressWarnings({"DefaultCharset", "CatchAndPrintStackTrace", "SystemOut"})
    private static void generateImportedLibrariesListFromGradle() {
        Pattern pattern;
        Matcher matcher;
        String line;

        ArrayList<String> regexps = new ArrayList<>();
        regexps.add("\"(.*):.*:.*\"");
        regexps.add("'(.*):.*:.*'");

        ArrayList<String> files = new ArrayList<>();
        files.add("build.gradle");
        files.add("build.gradle.kt");

        for (String file : files) {
            for (String regex : regexps) {
                pattern = Pattern.compile(regex, Pattern.MULTILINE);

                try {
                    Path currentPath = Paths.get(System.getProperty("user.dir"));
                    Path filePath = Paths.get(currentPath.toString(), file);

                    if (Files.exists(filePath)) {
                        FileReader fileReader = new FileReader(filePath.toString());
                        BufferedReader bufferedReader = new BufferedReader(fileReader);

                        while ((line = bufferedReader.readLine()) != null) {
                            if (line.contains("implementation") && ! line.contains("testImplementation")) {
                                matcher = pattern.matcher(line);

                                if (matcher.find()) {
                                    if (! importedLibrariesFromGradle.contains(matcher.group(1)) ) {
                                        String whitelistedLibrary = getCallsiteStackTraceWhitelistProperty();

                                        if (whitelistedLibrary == null) {
                                            importedLibrariesFromGradle.add(matcher.group(1));
                                        } else {
                                            if (! matcher.group(1).contains(whitelistedLibrary)) {
                                                importedLibrariesFromGradle.add(matcher.group(1));
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        bufferedReader.close();
                    }
                } catch (IOException e) {
                    throw new FilibusterCallsiteGenerationException(e);
                }
            }
        }
    }
}
