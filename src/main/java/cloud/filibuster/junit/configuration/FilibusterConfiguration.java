package cloud.filibuster.junit.configuration;

import cloud.filibuster.instrumentation.helpers.Networking;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

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

    private FilibusterConfiguration(Builder builder) {
        this.dynamicReduction = builder.dynamicReduction;
        this.suppressCombinations = builder.suppressCombinations;
        this.filibusterHost = builder.filibusterHost;
        this.filibusterPort = builder.filibusterPort;
        this.dataNondeterminism = builder.dataNondeterminism;
        this.analysisFile = builder.analysisFile;
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
     * Return URI for Filibuster server given current configuration.
     *
     * @return URI (as String)
     */
    public String getFilibusterBaseUri() {
        return "http://" + filibusterHost + ":" + filibusterPort + "/";
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

        if (analysisFile != null) {
            commands.add("--analysis-file");
            commands.add(analysisFile);
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
