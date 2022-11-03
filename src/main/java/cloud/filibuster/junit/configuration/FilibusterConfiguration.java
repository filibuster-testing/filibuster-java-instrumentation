package cloud.filibuster.junit.configuration;

import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.server.backends.FilibusterLocalProcessServerBackend;
import cloud.filibuster.junit.server.FilibusterServerBackend;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.micrometer.core.instrument.util.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class FilibusterConfiguration {
    private static final String filibusterExecutable = "/usr/local/bin/filibuster";

    private final boolean dynamicReduction;

    private final boolean suppressCombinations;

    private final String filibusterHost;

    private final int filibusterPort;

    private final boolean dataNondeterminism;

    private final String analysisFile;

    private final FilibusterServerBackend filibusterServerBackend;

    private FilibusterConfiguration(Builder builder) {
        this.dynamicReduction = builder.dynamicReduction;
        this.suppressCombinations = builder.suppressCombinations;
        this.filibusterHost = builder.filibusterHost;
        this.filibusterPort = builder.filibusterPort;
        this.dataNondeterminism = builder.dataNondeterminism;
        this.analysisFile = builder.analysisFile;
        this.filibusterServerBackend = builder.filibusterServerBackend;
    }

    public FilibusterServerBackend getFilibusterServerBackend() {
        return this.filibusterServerBackend;
    }

    /**
     * Does the current configuration contain data nondeterminism?
     *
     * @return boolean
     */
    public boolean getDataNondeterminism() {
        return this.dataNondeterminism;
    }

    public boolean getDynamicReduction() {
        return this.dynamicReduction;
    }

    public boolean getSuppressCombinations() {
        return this.suppressCombinations;
    }

    /**
     * Return URI for Filibuster server given current configuration.
     *
     * @return URI (as String)
     */
    public String getFilibusterBaseUri() {
        return "http://" + filibusterHost + ":" + filibusterPort + "/";
    }

    public JSONObject readAnalysisFile() throws FileNotFoundException {
        if (analysisFile != null) {
            File f = new File(analysisFile);

            if (f.exists()) {
                InputStream is = new FileInputStream(f);
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

        private String filibusterHost = Networking.getFilibusterHost();
        private int filibusterPort = Networking.getFilibusterPort();

        private String analysisFile;

        private FilibusterServerBackend filibusterServerBackend = new FilibusterLocalProcessServerBackend();

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
         * Specify the host name that the Filibuster server is running on.
         *
         * @param filibusterHost host name of the Filibuster server.
         * @return builder
         */
        @SuppressWarnings("unused")
        @CanIgnoreReturnValue
        public Builder filibusterHost(String filibusterHost) {
            this.filibusterHost = filibusterHost;
            return this;
        }

        /**
         * Specify the port that the Filibuster server is running on.
         *
         * @param filibusterPort port number of Filibuster server.
         * @return builder
         */
        @SuppressWarnings("unused")
        @CanIgnoreReturnValue
        public Builder filibusterPort(int filibusterPort) {
            this.filibusterPort = filibusterPort;
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

        @CanIgnoreReturnValue
        public Builder filibusterServerBackend(FilibusterServerBackend filibusterServerBackend) {
            this.filibusterServerBackend = filibusterServerBackend;
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
