package cloud.filibuster.junit.extensions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedCustomAnalysisFileException;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.TestWithFaultInjection;
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
import static cloud.filibuster.instrumentation.helpers.Property.getEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendDockerImageNameProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getTestDataNondeterminismProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getTestMaxIterationsProperty;

@SuppressWarnings("JavaDoc")
public class FilibusterTestExtension implements TestTemplateInvocationContextProvider {
    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return AnnotationUtils.isAnnotated(context.getTestMethod(), TestWithFaultInjection.class);
    }

    @Override
    @SuppressWarnings("Java8ApiChecker")
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        Method testMethod = context.getRequiredTestMethod();
        String displayName = context.getDisplayName();
        String analysisFile;

        Preconditions.condition(AnnotationUtils.findAnnotation(testMethod, TestWithFaultInjection.class).isPresent(), () ->
                "Configuration error: @FilibusterTest must be used on any methods extended with FilibusterTestExtension.'.");
        TestWithFaultInjection testWithFaultInjection = AnnotationUtils.findAnnotation(testMethod, TestWithFaultInjection.class).get();

        // Increase iterations by 1.
        // Last iteration doesn't actually run and is used only for teardown of the Filibuster server process.
        int specifiedMaxIterations = maxIterations(testWithFaultInjection, testMethod);
        int maxIterations = specifiedMaxIterations + 1;

        FilibusterTestDisplayNameFormatter formatter = displayNameFormatter(testWithFaultInjection, testMethod, displayName);

        if (! testWithFaultInjection.analysisFile().isEmpty()) {
            analysisFile = testWithFaultInjection.analysisFile();
        } else {
            analysisFile = "/tmp/filibuster-analysis-file";
            classToCustomAnalysisConfigurationFile(testWithFaultInjection, analysisFile);
        }

        // If the docker image has been specified as part of the annotation, we use it.
        // Otherwise, we use the one taken from the system property.
        String dockerImageName;

        if (! testWithFaultInjection.dockerImageName().isEmpty()) {
            dockerImageName = testWithFaultInjection.dockerImageName();
        } else {
            dockerImageName = getServerBackendDockerImageNameProperty();
        }

        boolean dataNondeterminism = testWithFaultInjection.dataNondeterminism();

        if (dataNondeterminism == DATA_NONDETERMINISM_DEFAULT) {
            // Check the property to see if it was set.
            dataNondeterminism = getTestDataNondeterminismProperty();
        }

        FilibusterConfiguration filibusterConfiguration = new FilibusterConfiguration.Builder()
                .dynamicReduction(testWithFaultInjection.dynamicReduction())
                .suppressCombinations(testWithFaultInjection.suppressCombinations())
                .dataNondeterminism(dataNondeterminism)
                .serverBackend(testWithFaultInjection.serverBackend())
                .searchStrategy(testWithFaultInjection.searchStrategy())
                .dockerImageName(dockerImageName)
                .analysisFile(analysisFile)
                .degradeWhenServerInitializationFails(testWithFaultInjection.degradeWhenServerInitializationFails())
                .expected(testWithFaultInjection.expected())
                .latencyProfile(testWithFaultInjection.latencyProfile())
                .serviceProfilesPath(testWithFaultInjection.serviceProfilesPath())
                .serviceProfileBehavior(testWithFaultInjection.serviceProfileBehavior())
                .testName(displayName)
                .build();

        validateSearchBackend(testWithFaultInjection, filibusterConfiguration);
        validateBackendSelection(testWithFaultInjection, filibusterConfiguration);

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

    private static void classToCustomAnalysisConfigurationFile(TestWithFaultInjection testWithFaultInjection, String analysisFile) {
        Class<? extends FilibusterAnalysisConfigurationFile> clazz = testWithFaultInjection.analysisConfigurationFile();

        FilibusterAnalysisConfigurationFile analysisConfigurationFile;

        try {
            analysisConfigurationFile = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new FilibusterUnsupportedCustomAnalysisFileException("Class doesn't match expected contract: " + e);
        }

        FilibusterCustomAnalysisConfigurationFile filibusterAnalysisConfigurationFile = analysisConfigurationFile.toFilibusterCustomAnalysisConfigurationFile();
        filibusterAnalysisConfigurationFile.writeToDisk(analysisFile);
    }

    private static int maxIterations(TestWithFaultInjection testWithFaultInjection, Method method) {
        int repetitions = testWithFaultInjection.maxIterations();

        if (repetitions == MAX_ITERATIONS_DEFAULT) {
            // Check the property to see if it was set.
            repetitions = getTestMaxIterationsProperty();
        }

        Preconditions.condition(repetitions > 0, () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared with a positive 'maxIterations'.", method));
        return repetitions;
    }

    private static void validateSearchBackend(TestWithFaultInjection testWithFaultInjection, FilibusterConfiguration filibusterConfiguration) {
        FilibusterServerBackend filibusterServerBackend = filibusterConfiguration.getServerBackend();
        FilibusterSearchStrategy filibusterSearchStrategy = testWithFaultInjection.searchStrategy();
        List<FilibusterSearchStrategy> supportedSearchStrategies = filibusterServerBackend.supportedSearchStrategies();

        Preconditions.condition(supportedSearchStrategies.contains(filibusterSearchStrategy), () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared a supported search strategy by the chosen backend.", filibusterServerBackend));
    }

    private static void validateBackendSelection(TestWithFaultInjection testWithFaultInjection, FilibusterConfiguration filibusterConfiguration) {
        FilibusterServerBackend filibusterServerBackend = filibusterConfiguration.getServerBackend();
        FilibusterLatencyProfile filibusterLatencyProfile = filibusterConfiguration.getLatencyProfile();

        if (!(filibusterLatencyProfile instanceof FilibusterNoLatencyProfile)) {
            Preconditions.condition(filibusterServerBackend.latencyProfileSupported(), () -> String.format(
                    "Configuration error: @FilibusterTest on method [%s] is using a custom latency profile but the chosen backend does not support it.", filibusterServerBackend));
        }

    }

    private static FilibusterTestDisplayNameFormatter displayNameFormatter(TestWithFaultInjection testWithFaultInjection, Method method, String displayName) {
        String initialName = Preconditions.notBlank(testWithFaultInjection.initialName().trim(), () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared with a non-empty name.", method));
        String generatedName = Preconditions.notBlank(testWithFaultInjection.name().trim(), () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared with a non-empty name.", method));
        return new FilibusterTestDisplayNameFormatter(initialName, generatedName, displayName);
    }
}