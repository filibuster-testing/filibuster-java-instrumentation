package cloud.filibuster.junit.configuration;

import cloud.filibuster.junit.server.backends.FilibusterDockerServerBackend;
import cloud.filibuster.junit.server.FilibusterServerBackend;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.micrometer.core.instrument.util.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class FilibusterConfiguration {
    private static final String filibusterExecutable = "/usr/local/bin/filibuster";

    private final boolean dynamicReduction;

    private final boolean suppressCombinations;

    private final boolean dataNondeterminism;

    private final String analysisFile;

    private final FilibusterServerBackend filibusterServerBackend;

    private final String dockerImageName;

    private final boolean degradeWhenServerInitializationFails;

    private FilibusterConfiguration(Builder builder) {
        this.dynamicReduction = builder.dynamicReduction;
        this.suppressCombinations = builder.suppressCombinations;
        this.dataNondeterminism = builder.dataNondeterminism;
        this.analysisFile = builder.analysisFile;
        this.filibusterServerBackend = builder.filibusterServerBackend;
        this.dockerImageName = builder.dockerImageName;
        this.degradeWhenServerInitializationFails = builder.degradeWhenServerInitializationFails;
    }

    /**
     * Return the backend implementation that should be used for running the Filibuster server.
     *
     * @return server backend.
     */
    public FilibusterServerBackend getFilibusterServerBackend() {
        return this.filibusterServerBackend;
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
     * Should dynamic reduction be used?
     *
     * @return boolean
     */
    public boolean getDynamicReduction() {
        return this.dynamicReduction;
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

    public static class Builder {
        private boolean dynamicReduction = false;
        private boolean suppressCombinations = false;
        private boolean dataNondeterminism = false;

        private String analysisFile;

        private FilibusterServerBackend filibusterServerBackend = new FilibusterDockerServerBackend();

        private String dockerImageName;

        private boolean degradeWhenServerInitializationFails = false;

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
        public Builder filibusterServerBackend(Class<? extends FilibusterServerBackend> clazz) {
            FilibusterServerBackend serverBackend;

            try {
                serverBackend = clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // TODO: something better.
                throw new UnsupportedOperationException(e);
            }

            this.filibusterServerBackend = serverBackend;
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
         * Build configuration.
         *
         * @return builder
         */
        public FilibusterConfiguration build() {
            return new FilibusterConfiguration(this);
        }
    }
}
