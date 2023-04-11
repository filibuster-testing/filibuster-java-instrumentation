package cloud.filibuster.junit.extensions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedCustomAnalysisFileException;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.FilibusterTest;
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

import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendDockerImageNameProperty;

@SuppressWarnings("JavaDoc")
public class FilibusterTestExtension implements TestTemplateInvocationContextProvider {
    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return AnnotationUtils.isAnnotated(context.getTestMethod(), FilibusterTest.class);
    }

    @Override
    @SuppressWarnings("Java8ApiChecker")
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        Method testMethod = context.getRequiredTestMethod();
        String displayName = context.getDisplayName();
        String analysisFile;

        Preconditions.condition(AnnotationUtils.findAnnotation(testMethod, FilibusterTest.class).isPresent(), () ->
                "Configuration error: @FilibusterTest must be used on any methods extended with FilibusterTestExtension.'.");
        FilibusterTest filibusterTest = AnnotationUtils.findAnnotation(testMethod, FilibusterTest.class).get();

        // Increase iterations by 1.
        // Last iteration doesn't actually run and is used only for teardown of the Filibuster server process.
        int specifiedMaxIterations = maxIterations(filibusterTest, testMethod);
        int maxIterations = specifiedMaxIterations + 1;

        FilibusterTestDisplayNameFormatter formatter = displayNameFormatter(filibusterTest, testMethod, displayName);

        if (! filibusterTest.analysisFile().isEmpty()) {
            analysisFile = filibusterTest.analysisFile();
        } else {
            analysisFile = "/tmp/filibuster-analysis-file";
            classToCustomAnalysisConfigurationFile(filibusterTest, analysisFile);
        }

        // If the docker image has been specified as part of the annotation, we use it.
        // Otherwise, we use the one taken from the system property.
        String dockerImageName;

        if (! filibusterTest.dockerImageName().isEmpty()) {
            dockerImageName = filibusterTest.dockerImageName();
        } else {
            dockerImageName = getServerBackendDockerImageNameProperty();
        }

        FilibusterConfiguration filibusterConfiguration = new FilibusterConfiguration.Builder()
                .dynamicReduction(filibusterTest.dynamicReduction())
                .suppressCombinations(filibusterTest.suppressCombinations())
                .dataNondeterminism(filibusterTest.dataNondeterminism())
                .serverBackend(filibusterTest.serverBackend())
                .searchStrategy(filibusterTest.searchStrategy())
                .dockerImageName(dockerImageName)
                .analysisFile(analysisFile)
                .degradeWhenServerInitializationFails(filibusterTest.degradeWhenServerInitializationFails())
                .expected(filibusterTest.expected())
                .latencyProfile(filibusterTest.latencyProfile())
                .serviceProfilesPath(filibusterTest.serviceProfilesPath())
                .serviceProfileBehavior(filibusterTest.serviceProfileBehavior())
                .build();

        validateSearchBackend(filibusterTest, filibusterConfiguration);
        validateBackendSelection(filibusterTest, filibusterConfiguration);

        HashMap<Integer, Boolean> invocationCompletionMap = new HashMap<>();

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
    }

    private static void classToCustomAnalysisConfigurationFile(FilibusterTest filibusterTest, String analysisFile) {
        Class<? extends FilibusterAnalysisConfigurationFile> clazz = filibusterTest.analysisConfigurationFile();

        FilibusterAnalysisConfigurationFile analysisConfigurationFile;

        try {
            analysisConfigurationFile = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new FilibusterUnsupportedCustomAnalysisFileException("Class doesn't match expected contract: " + e);
        }

        FilibusterCustomAnalysisConfigurationFile filibusterAnalysisConfigurationFile = analysisConfigurationFile.toFilibusterCustomAnalysisConfigurationFile();
        filibusterAnalysisConfigurationFile.writeToDisk(analysisFile);
    }

    private static int maxIterations(FilibusterTest filibusterTest, Method method) {
        int repetitions = filibusterTest.maxIterations();
        Preconditions.condition(repetitions > 0, () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared with a positive 'maxIterations'.", method));
        return repetitions;
    }

    private static void validateSearchBackend(FilibusterTest filibusterTest, FilibusterConfiguration filibusterConfiguration) {
        FilibusterServerBackend filibusterServerBackend = filibusterConfiguration.getServerBackend();
        FilibusterSearchStrategy filibusterSearchStrategy = filibusterTest.searchStrategy();
        List<FilibusterSearchStrategy> supportedSearchStrategies = filibusterServerBackend.supportedSearchStrategies();

        Preconditions.condition(supportedSearchStrategies.contains(filibusterSearchStrategy), () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared a supported search strategy by the chosen backend.", filibusterServerBackend));
    }

    private static void validateBackendSelection(FilibusterTest filibusterTest, FilibusterConfiguration filibusterConfiguration) {
        FilibusterServerBackend filibusterServerBackend = filibusterConfiguration.getServerBackend();
        FilibusterLatencyProfile filibusterLatencyProfile = filibusterConfiguration.getLatencyProfile();

        if (!(filibusterLatencyProfile instanceof FilibusterNoLatencyProfile)) {
            Preconditions.condition(filibusterServerBackend.latencyProfileSupported(), () -> String.format(
                    "Configuration error: @FilibusterTest on method [%s] is using a custom latency profile but the chosen backend does not support it.", filibusterServerBackend));
        }

    }

    private static FilibusterTestDisplayNameFormatter displayNameFormatter(FilibusterTest filibusterTest, Method method, String displayName) {
        String initialName = Preconditions.notBlank(filibusterTest.initialName().trim(), () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared with a non-empty name.", method));
        String generatedName = Preconditions.notBlank(filibusterTest.name().trim(), () -> String.format(
                "Configuration error: @FilibusterTest on method [%s] must be declared with a non-empty name.", method));
        return new FilibusterTestDisplayNameFormatter(initialName, generatedName, displayName);
    }
}