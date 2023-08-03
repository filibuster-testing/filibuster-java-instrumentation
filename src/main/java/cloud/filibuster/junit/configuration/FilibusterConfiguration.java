package cloud.filibuster.junit.configuration;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedServerBackendException;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.filters.FilibusterFaultInjectionFilter;
import cloud.filibuster.junit.server.backends.FilibusterDockerServerBackend;
import cloud.filibuster.junit.server.FilibusterServerBackend;
import cloud.filibuster.junit.server.core.profiles.ServiceProfile;
import cloud.filibuster.junit.server.core.profiles.ServiceProfileBehavior;
import cloud.filibuster.junit.server.latency.FilibusterLatencyProfile;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.micrometer.core.instrument.util.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FilibusterConfiguration {
    public static FilibusterServerBackend backendToBackendClass(Class<? extends FilibusterServerBackend> clazz) {
        FilibusterServerBackend serverBackend;

        try {
            serverBackend = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new FilibusterUnsupportedServerBackendException("Backend " + clazz + " is not supported.", e);
        }

        return serverBackend;
    }

    private static final String filibusterExecutable = "/usr/local/bin/filibuster";

    private final boolean dynamicReduction;
    private final boolean suppressCombinations;
    private final boolean dataNondeterminism;
    private final boolean avoidRedundantInjections;
    private final boolean avoidInjectionsOnOrganicFailures;
    private final boolean failOnOrganicFailures;
    private final FilibusterSearchStrategy searchStrategy;
    private final String analysisFile;
    private final FilibusterServerBackend serverBackend;
    private final String dockerImageName;
    private final boolean degradeWhenServerInitializationFails;
    private final boolean abortOnFirstFailure;
    private final Class<? extends Throwable> expected;
    private final Class<? extends FilibusterFaultInjectionFilter> faultInjectionFilter;
    private final FilibusterLatencyProfile latencyProfile;
    private final String testName;
    private final String className;
    private final List<ServiceProfile> serviceProfiles;
    private final ServiceProfileBehavior serviceProfileBehavior;

    private final boolean failIfFaultNotInjected;

    private FilibusterConfiguration(Builder builder) {
        this.abortOnFirstFailure = builder.abortOnFirstFailure;
        this.dynamicReduction = builder.dynamicReduction;
        this.suppressCombinations = builder.suppressCombinations;
        this.dataNondeterminism = builder.dataNondeterminism;
        this.avoidRedundantInjections = builder.avoidRedundantInjections;
        this.avoidInjectionsOnOrganicFailures = builder.avoidInjectionsOnOrganicFailures;
        this.failOnOrganicFailures = builder.failOnOrganicFailures;
        this.searchStrategy = builder.searchStrategy;
        this.analysisFile = builder.analysisFile;
        this.serverBackend = builder.serverBackend;
        this.dockerImageName = builder.dockerImageName;
        this.degradeWhenServerInitializationFails = builder.degradeWhenServerInitializationFails;
        this.expected = builder.expected;
        this.faultInjectionFilter = builder.faultInjectionFilter;
        this.latencyProfile = builder.latencyProfile;
        this.testName = builder.testName;
        this.serviceProfiles = builder.serviceProfiles;
        this.serviceProfileBehavior = builder.serviceProfileBehavior;
        this.className = builder.className;
        this.failIfFaultNotInjected = builder.failIfFaultNotInjected;
    }

    /**
     * Return the backend implementation that should be used for running the Filibuster server.
     *
     * @return server backend.
     */
    public FilibusterServerBackend getServerBackend() {
        return this.serverBackend;
    }

    /**
     * Return expected exception.
     *
     * @return throwable
     */
    public Class<? extends Throwable> getExpected() {
        return this.expected;
    }

    /**
     * Return fault injection filter.
     *
     * @return fault injection filter
     */
    public Class<? extends FilibusterFaultInjectionFilter> getFaultInjectionFilter() {
        return this.faultInjectionFilter;
    }

    /**
     * Name of the docker image containing the Filibuster server.
     *
     * @return string
     */
    public String getDockerImageName() {
        return this.dockerImageName;
    }

    /**
     * Does the current configuration contain data nondeterminism?
     *
     * @return boolean
     */
    public boolean getDataNondeterminism() {
        return this.dataNondeterminism;
    }

    /**
     * Do we avoid redundant injections?
     *
     * @return boolean
     */
    public boolean getAvoidRedundantInjections() {
        return this.avoidRedundantInjections;
    }

    /**
     * Do we avoid injections on failing RPCs?
     *
     * @return boolean
     */
    public boolean getAvoidInjectionsOnOrganicFailures() {
        return this.avoidInjectionsOnOrganicFailures;
    }

    /**
     * Do we fail on failing RPCs?
     *
     * @return boolean
     */
    public boolean getFailOnOrganicFailures() {
        return this.failOnOrganicFailures;
    }

    /**
     * Should dynamic reduction be used?
     *
     * @return boolean
     */
    public boolean getDynamicReduction() {
        return this.dynamicReduction;
    }

    /**
     * Should abort on first failure?
     *
     * @return boolean
     */
    public boolean getAbortOnFirstFailure() {
        return this.abortOnFirstFailure;
    }

    /**
     * Should combinations of faults be suppressed?
     *
     * @return boolean
     */
    public boolean getSuppressCombinations() {
        return this.suppressCombinations;
    }

    /**
     * Which search strategy should Filibuster use?
     *
     * @return search strategy.
     */
    public FilibusterSearchStrategy getSearchStrategy() {
        return this.searchStrategy;
    }

    /**
     * Which latency profile should Filibuster use?
     *
     * @return latency profile
     */
    public FilibusterLatencyProfile getLatencyProfile() {
        return this.latencyProfile;
    }

    public String getTestName() {
        return this.testName;
    }

    /**
     * What service profiles should Filibuster use?
     *
     * @return service profiles
     */
    public List<ServiceProfile> getServiceProfiles() {
        return this.serviceProfiles;
    }

    /**
     * Fail the test if a fault is not injected.
     *
     * @return whether service should fail.
     */
    public boolean getFailIfFaultNotInjected() {
        return this.failIfFaultNotInjected;
    }

    /**
     * How should service profiles be used?
     *
     * @return service profile behavior
     */
    public ServiceProfileBehavior getServiceProfileBehavior() {
        return this.serviceProfileBehavior;
    }

    /**
     * Should the jUnit suite degrade and run the tests without faults only when the server is unavailable?
     *
     * @return boolean
     */
    public boolean getDegradeWhenServerInitializationFails() {
        return this.degradeWhenServerInitializationFails;
    }

    /**
     * Returns the analysis file content as JSON, read from either file or annotation-based configuration.
     *
     * @return json object.
     * @throws FileNotFoundException when the analysis file cannot be found.
     */
    public JSONObject readAnalysisFile() throws IOException {
        if (analysisFile != null) {
            File f = new File(analysisFile);

            if (f.exists()) {
                InputStream is = Files.newInputStream(f.toPath());
                String jsonTxt = IOUtils.toString(is, Charset.defaultCharset());
                return new JSONObject(jsonTxt);
            } else {
                return new JSONObject();
            }

        } else {
            return new JSONObject();
        }
    }

    /**
     * Generate executable command to run Filibuster given the current configuration.
     *
     * @return executable command with options.
     */
    public List<String> toExecutableCommand() {
        List<String> commands = new ArrayList<>();
        commands.add(filibusterExecutable);
        commands.add("--server-only");

        if (!dynamicReduction) {
            commands.add("--disable-dynamic-reduction");
        }

        if (suppressCombinations) {
            commands.add("--should-suppress-combinations");
        }

        return commands;
    }

    public String getClassName() {
        return className;
    }

    public static class Builder {
        private boolean dynamicReduction = false;
        private boolean suppressCombinations = false;
        private boolean dataNondeterminism = false;
        private boolean avoidRedundantInjections = false;
        private boolean avoidInjectionsOnOrganicFailures = false;
        private boolean failOnOrganicFailures = false;
        private boolean abortOnFirstFailure = false;

        private FilibusterSearchStrategy searchStrategy;

        private String analysisFile;

        private FilibusterServerBackend serverBackend = new FilibusterDockerServerBackend();

        private String dockerImageName;

        private boolean degradeWhenServerInitializationFails = false;

        private Class<? extends Throwable> expected;

        private Class<? extends FilibusterFaultInjectionFilter> faultInjectionFilter;

        private FilibusterLatencyProfile latencyProfile;

        private String testName;

        private String className;
        private List<ServiceProfile> serviceProfiles;

        private ServiceProfileBehavior serviceProfileBehavior;

        private boolean failIfFaultNotInjected;

        /**
         * Should this configuration use dynamic reduction?
         *
         * @param dynamicReduction should dynamic reduction be used?
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder dynamicReduction(boolean dynamicReduction) {
            this.dynamicReduction = dynamicReduction;
            return this;
        }

        /**
         * Should this configuration avoid exploring combinations of faults?
         *
         * @param suppressCombinations avoid multiple simultaneous faults
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder suppressCombinations(boolean suppressCombinations) {
            this.suppressCombinations = suppressCombinations;
            return this;
        }

        /**
         * Should we abort on first failure?
         *
         * @param abortOnFirstFailure should we abort on first failure
         * @return boolean
         */
        @CanIgnoreReturnValue
        public Builder abortOnFirstFailure(boolean abortOnFirstFailure) {
            this.abortOnFirstFailure = abortOnFirstFailure;
            return this;
        }

        /**
         * Does this test configuration contain data nondeterminism?
         *
         * @param dataNondeterminism whether the test configuration contains data nondeterminism in RPCs.
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder dataNondeterminism(boolean dataNondeterminism) {
            this.dataNondeterminism = dataNondeterminism;
            return this;
        }

        /**
         * Do we avoid redundant fault injections?
         *
         * @param avoidRedundantInjections whether the avoids redundant fault injections
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder avoidRedundantInjections(boolean avoidRedundantInjections) {
            this.avoidRedundantInjections = avoidRedundantInjections;
            return this;
        }

        /**
         * Do we fail the test on organic failures?
         *
         * @param failOnOrganicFailures whether the test fails on failing RPCs
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder failOnOrganicFailures(boolean failOnOrganicFailures) {
            this.failOnOrganicFailures = failOnOrganicFailures;
            return this;
        }

        /**
         * Do we avoid fault injections on failing RPCs?
         *
         * @param avoidInjectionsOnOrganicFailures whether the avoids fault injections on failing RPCs
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder avoidInjectionsOnOrganicFailures(boolean avoidInjectionsOnOrganicFailures) {
            this.avoidInjectionsOnOrganicFailures = avoidInjectionsOnOrganicFailures;
            return this;
        }

        /**
         * Analysis file that should be used for this configuration of Filibuster.
         *
         * @param analysisFile absolute path to the analysis file.
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder analysisFile(String analysisFile) {
            this.analysisFile = analysisFile;
            return this;
        }

        /**
         * Server backend to use.
         *
         * @param clazz class.
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder serverBackend(Class<? extends FilibusterServerBackend> clazz) {
            this.serverBackend = backendToBackendClass(clazz);
            return this;
        }

        /**
         * Expected exception thrown.
         *
         * @param clazz class of the exception thrown
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder expected(Class<? extends Throwable> clazz) {
            this.expected = clazz;
            return this;
        }

        /**
         * Fault injection filter.
         *
         * @param clazz class of the fault injection filter.
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder faultInjectionFilter(Class<? extends FilibusterFaultInjectionFilter> clazz) {
            this.faultInjectionFilter = clazz;
            return this;
        }

        /**
         * Docker image to use.
         *
         * @param dockerImageName string of the fully qualified docker image name.
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder dockerImageName(String dockerImageName) {
            this.dockerImageName = dockerImageName;
            return this;
        }

        /**
         * Should the test suite degrade when the Filibuster server is unavailable (rather than fail the test completely?)
         *
         * @param degradeWhenServerInitializationFails boolean
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder degradeWhenServerInitializationFails(boolean degradeWhenServerInitializationFails) {
            this.degradeWhenServerInitializationFails = degradeWhenServerInitializationFails;
            return this;
        }

        /**
         * Which search strategy should Filibuster use?
         *
         * @param searchStrategy search strategy
         * @return builder
         */
        @CanIgnoreReturnValue
        public Builder searchStrategy(FilibusterSearchStrategy searchStrategy) {
            this.searchStrategy = searchStrategy;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder testName(String testName) {
            this.testName = testName;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder latencyProfile(Class<? extends FilibusterLatencyProfile> clazz) {
            FilibusterLatencyProfile latencyProfile;

            try {
                latencyProfile = clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | NoSuchMethodException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new FilibusterUnsupportedServerBackendException("Backend " + clazz + " is not supported.", e);
            }

            this.latencyProfile = latencyProfile;
            return this;
        }

        @CanIgnoreReturnValue
        @SuppressWarnings("Java8ApiChecker")
        public Builder serviceProfilesPath(String serviceProfilesPath) {
            if (!serviceProfilesPath.isEmpty()) {
                Path path = Path.of(serviceProfilesPath);
                List<ServiceProfile> serviceProfiles = ServiceProfile.loadFromDirectory(path);
                this.serviceProfiles = serviceProfiles;
            }

            return this;
        }

        @CanIgnoreReturnValue
        @SuppressWarnings("Java8ApiChecker")
        public Builder serviceProfileBehavior(ServiceProfileBehavior serviceProfileBehavior) {
            this.serviceProfileBehavior = serviceProfileBehavior;
            return this;
        }


        @CanIgnoreReturnValue
        public Builder className(String className) {
            this.className = className;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder failIfFaultNotInjected(boolean failIfFaultNotInjected) {
            this.failIfFaultNotInjected = failIfFaultNotInjected;
            return this;
        }

        /**
         * Build configuration.
         *
         * @return builder
         */
        public FilibusterConfiguration build() {
            return new FilibusterConfiguration(this);
        }

    }
}
