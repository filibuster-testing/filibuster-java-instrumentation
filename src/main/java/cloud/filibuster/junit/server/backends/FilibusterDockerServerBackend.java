package cloud.filibuster.junit.server.backends;

import cloud.filibuster.instrumentation.helpers.Property;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerBackend;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static cloud.filibuster.instrumentation.helpers.Property.SERVER_HOST_DEFAULT;
import static cloud.filibuster.instrumentation.helpers.Property.SERVER_PORT_DEFAULT;

public class FilibusterDockerServerBackend implements FilibusterServerBackend {
    private GenericContainer<?> container;

    private static final int EXPOSED_PORT = 5005;

    @Override
    public synchronized boolean start(FilibusterConfiguration filibusterConfiguration) throws Throwable {
        String fullImageName = filibusterConfiguration.getDockerImageName();

        this.container = new GenericContainer<>(DockerImageName.parse(fullImageName))
                .withExposedPorts(EXPOSED_PORT)
                .waitingFor(new HttpWaitStrategy().forPort(EXPOSED_PORT))
                .withStartupTimeout(Duration.ofMinutes(5));

        if (filibusterConfiguration.getSuppressCombinations()) {
            this.container = this.container.withEnv("SHOULD_SUPPRESS_COMBINATIONS", "True");
        }

        if (!filibusterConfiguration.getDynamicReduction()) {
            this.container = this.container.withEnv("DISABLE_DYNAMIC_REDUCTION", "True");
        }

        this.container.start();

        Property.setServerHostProperty(container.getHost());
        Property.setServerPortProperty(container.getMappedPort(EXPOSED_PORT));

        return true;
    }

    @Override
    public synchronized boolean stop(FilibusterConfiguration filibusterConfiguration) {
        container.stop();

        Property.setServerHostProperty(SERVER_HOST_DEFAULT);
        Property.setServerPortProperty(SERVER_PORT_DEFAULT);

        return true;
    }

    @Override
    public List<FilibusterSearchStrategy> supportedSearchStrategies() {
        return Arrays.asList(FilibusterSearchStrategy.DFS);
    }

    @Override
    public FilibusterSearchStrategy defaultSearchStrategy() {
        return FilibusterSearchStrategy.DFS;
    }

    @Override
    public boolean latencyProfileSupported() {
        return false;
    }
}
