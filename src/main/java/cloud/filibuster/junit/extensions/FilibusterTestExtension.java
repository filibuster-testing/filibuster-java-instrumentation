package cloud.filibuster.junit.extensions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import cloud.filibuster.exceptions.filibuster.FilibusterAnalysisFileResourcePathException;
import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedCustomAnalysisFileException;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.formatters.FilibusterTestDisplayNameFormatter;
import cloud.filibuster.junit.interceptors.FilibusterTestInvocationContext;
import cloud.filibuster.junit.server.FilibusterServerBackend;
import cloud.filibuster.junit.server.latency.FilibusterLatencyProfile;
import cloud.filibuster.junit.server.latency.FilibusterNoLatencyProfile;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.Preconditions;

import static cloud.filibuster.instrumentation.helpers.Property.DATA_NONDETERMINISM_DEFAULT;
import static cloud.filibuster.instrumentation.helpers.Property.MAX_ITERATIONS_DEFAULT;
import static cloud.filibuster.instrumentation.helpers.Property.SUPPRESS_COMBINATIONS_DEFAULT;
import static cloud.filibuster.instrumentation.helpers.Property.getEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendDockerImageNameProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getTestAnalysisResourceFileProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getTestDataNondeterminismProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getTestMaxIterationsProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getTestSuppressCombinationsProperty;

@SuppressWarnings("JavaDoc")
public class FilibusterTestExtension implements TestTemplateInvocationContextProvider {
    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return AnnotationUtils.isAnnotated(context.getTestMethod(), TestWithFilibuster.class);
    }

    @Override
    @SuppressWarnings("Java8ApiChecker")
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        Method testMethod = context.getRequiredTestMethod();
        String className = context.getTestClass().map(Class::getName).orElse("");
        String displayName = context.getDisplayName();
        String analysisFile;

        Preconditions.condition(AnnotationUtils.findAnnotation(testMethod, TestWithFilibuster.class).isPresent(), () ->
                "Configuration error: @FilibusterTest must be used on any methods extended with FilibusterTestExtension.'.");
        TestWithFilibuster testWithFilibuster = AnnotationUtils.findAnnotation(testMethod, TestWithFilibuster.class).get();

        // Increase iterations by 1.
        // Last iteration doesn't actually run and is used only for teardown of the Filibuster server process.
        int specifiedMaxIterations = maxIterations(testWithFilibuster, testMethod);
        int maxIterations = specifiedMaxIterations + 1;

        FilibusterTestDisplayNameFormatter formatter = displayNameFormatter(testWithFilibuster, testMethod, displayName);

        if (! testWithFilibuster.analysisFile().isEmpty()) {
            // Annotation analysisFile always takes precedence.
            analysisFile = testWithFilibuster.analysisFile();
        } else if (! getTestAnalysisResourceFileProperty().isEmpty()) {
            // Property next.
            String analysisResourceFile = getTestAnalysisResourceFileProperty();
            URL analysisFileResourcePath = FilibusterTestExtension.class.getClassLoader().getResource(analysisResourceFile);

            if (analysisFileResourcePath == null) {
                throw new FilibusterAnalysisFileResourcePathException("Analysis resource file property is set, but file does not exist.");
            }

            analysisFile = analysisFileResourcePath.getPath();
        } else {
            // ...then analysis configuration file annotation.
            analysisFile = "/tmp/filibuster-analysis-file";
            classToCustomAnalysisConfigurationFile(testWithFilibuster, analysisFile);
        }

        // If the docker image has been specified as part of the annotation, we use it.
        // Otherwise, we use the one taken from the system property.
        String dockerImageName;

        if (! testWithFilibuster.dockerImageName().isEmpty()) {
            dockerImageName = testWithFilibuster.dockerImageName();
        } else {
            dockerImageName = getServerBackendDockerImageNameProperty();
        }

        boolean dataNondeterminism = testWithFilibuster.dataNondeterminism();

        if (dataNondeterminism == DATA_NONDETERMINISM_DEFAULT) {
            // Check the property to see if it was set.
            dataNondeterminism = getTestDataNondeterminismProperty();
        }

        boolean suppressCombinations = testWithFilibuster.suppressCombinations();

        if (suppressCombinations == SUPPRESS_COMBINATIONS_DEFAULT) {
            // Check the property to see if it was set.
            suppressCombinations = getTestSuppressCombinationsProperty();
        }

        FilibusterConfiguration filibusterConfiguration = new FilibusterConfiguration.Builder()
                .dynamicReduction(testWithFilibuster.dynamicReduction())
                .suppressCombinations(suppressCombinations)
                .dataNondeterminism(dataNondeterminism)
                .serverBackend(testWithFilibuster.serverBackend())
                .searchStrategy(testWithFilibuster.searchStrategy())
                .dockerImageName(dockerImageName)
                .analysisFile(analysisFile)
                .degradeWhenServerInitializationFails(testWithFilibuster.degradeWhenServerInitializationFails())
                .expected(testWithFilibuster.expected())
                .latencyProfile(testWithFilibuster.latencyProfile())
                .serviceProfilesPath(testWithFilibuster.serviceProfilesPath())
                .serviceProfileBehavior(testWithFilibuster.serviceProfileBehavior())
                .testName(displayName)
                .className(className)
                .build();

        validateSearchBackend(testWithFilibuster, filibusterConfiguration);
        validateBackendSelection(testWithFilibuster, filibusterConfiguration);

        HashMap<Integer, Boolean> invocationCompletionMap = new HashMap<>();

        if (getEnabledProperty()) {
            // @formatter:off
            return IntStream
                    .rangeClosed(1, maxIterations)
                    .mapToObj(iteration -> new FilibusterTestInvocationContext(
                            iteration,
                            maxIterations,
                            formatter,
                            filibusterConfiguration,
                            invocationCompletionMap));
            // @formatter:on
        } else {
            // @formatter:off
            return IntStream
                    .rangeClosed(1, 1)
                    .mapToObj(iteration -> new FilibusterTestInvocationContext(
                            iteration,
                            maxIterations,
                            formatter,
                            filibusterConfiguration,
                            invocationCompletionMap));
            // @formatter:on
        }
    }

    private static void classToCustomAnalysisConfigurationFile(TestWithFilibuster testWithFilibuster, String analysisFile) {
        Class<? extends FilibusterAnalysisConfigurationFile> clazz = testWithFilibuster.analysisConfigurationFile();

        FilibusterAnalysisConfigurationFile analysisConfigurationFile;

        try {
            analysisConfigurationFile = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new FilibusterUnsupportedCustomAnalysisFileException("Class doesn't match expected contract: " + e);
        }

        FilibusterCustomAnalysisConfigurationFile filibusterAnalysisConfigurationFile = analysisConfigurationFile.toFilibusterCustomAnalysisConfigurationFile();
        filibusterAnalysisConfigurationFile.writeToDisk(analysisFile);
    }

    private static int maxIterations(TestWithFilibuster testWithFilibuster, Method method) {
        int repetitions = testWithFilibuster.maxIterations();

        if (repetitions == MAX_ITERATIONS_DEFAULT) {
            // Check the property to see if it was set.
            repetitions = getTestMaxIterationsProperty();
        }

        Preconditions.condition(repetitions > 0, () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared with a positive 'maxIterations'.", method));
        return repetitions;
    }

    private static void validateSearchBackend(TestWithFilibuster testWithFilibuster, FilibusterConfiguration filibusterConfiguration) {
        FilibusterServerBackend filibusterServerBackend = filibusterConfiguration.getServerBackend();
        FilibusterSearchStrategy filibusterSearchStrategy = testWithFilibuster.searchStrategy();
        List<FilibusterSearchStrategy> supportedSearchStrategies = filibusterServerBackend.supportedSearchStrategies();

        Preconditions.condition(supportedSearchStrategies.contains(filibusterSearchStrategy), () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared a supported search strategy by the chosen backend.", filibusterServerBackend));
    }

    private static void validateBackendSelection(TestWithFilibuster testWithFilibuster, FilibusterConfiguration filibusterConfiguration) {
        FilibusterServerBackend filibusterServerBackend = filibusterConfiguration.getServerBackend();
        FilibusterLatencyProfile filibusterLatencyProfile = filibusterConfiguration.getLatencyProfile();

        if (!(filibusterLatencyProfile instanceof FilibusterNoLatencyProfile)) {
            Preconditions.condition(filibusterServerBackend.latencyProfileSupported(), () -> String.format(
                    "Configuration error: @FilibusterTest on method [%s] is using a custom latency profile but the chosen backend does not support it.", filibusterServerBackend));
        }

    }

    private static FilibusterTestDisplayNameFormatter displayNameFormatter(TestWithFilibuster testWithFilibuster, Method method, String displayName) {
        String initialName = Preconditions.notBlank(testWithFilibuster.initialName().trim(), () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared with a non-empty name.", method));
        String generatedName = Preconditions.notBlank(testWithFilibuster.name().trim(), () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared with a non-empty name.", method));
        return new FilibusterTestDisplayNameFormatter(initialName, generatedName, displayName);
    }
}