package cloud.filibuster.junit.server.backends;

import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerBackend;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class FilibusterDockerServerBackend implements FilibusterServerBackend {
    private GenericContainer<?> container;

    @Override
    public boolean start(FilibusterConfiguration filibusterConfiguration) throws Throwable {
        this.container = new GenericContainer<>(DockerImageName.parse("redis:3.0.6"))
                .withExposedPorts(6379);
        return true;
    }

    @Override
    public boolean stop(FilibusterConfiguration filibusterConfiguration) {
        container.stop();
        return true;
    }
}
