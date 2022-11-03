package cloud.filibuster.junit.server.backends;

import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerBackend;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class FilibusterDockerServerBackend implements FilibusterServerBackend {
    private GenericContainer<?> container;

    @Override
    public synchronized boolean start(FilibusterConfiguration filibusterConfiguration) throws Throwable {
        this.container = new GenericContainer<>(DockerImageName.parse("filibuster:0.33"))
                .withExposedPorts(5005);

        if (filibusterConfiguration.getSuppressCombinations()) {
            this.container = this.container.withEnv("SHOULD_SUPPRESS_COMBINATIONS", "True");
        }

        if (!filibusterConfiguration.getDynamicReduction()) {
            this.container = this.container.withEnv("DISABLE_DYNAMIC_REDUCTION", "True");
        }

        this.container.start();

        return true;
    }

    @Override
    public synchronized boolean stop(FilibusterConfiguration filibusterConfiguration) {
        container.stop();
        return true;
    }
}
