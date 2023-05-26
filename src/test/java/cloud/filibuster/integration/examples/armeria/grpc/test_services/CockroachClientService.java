package cloud.filibuster.integration.examples.armeria.grpc.test_services;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nullable;
import org.postgresql.ds.PGSimpleDataSource;

public class CockroachClientService {
    private static final int DB_PORT = 26257;
    public PGSimpleDataSource cockroachClient;
    public String connectionString;

    @Nullable
    private static CockroachClientService single_instance = null;

    @SuppressWarnings("resource")
    private CockroachClientService() {
        try {
            GenericContainer<?> cockroachContainer = new GenericContainer<>(DockerImageName
                    .parse("cockroachdb/cockroach:latest-v23.1"))
                    .withExposedPorts(DB_PORT, 8080)
                    .withCommand("start-single-node --insecure");
            cockroachContainer.start();
            connectionString = getJdbcUrl(cockroachContainer);
            cockroachClient = new PGSimpleDataSource();
            cockroachClient.setUrl(connectionString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Static method to create instance of Singleton class
    public static synchronized CockroachClientService getInstance() {
        if (single_instance == null) {
            single_instance = new CockroachClientService();
        }

        return single_instance;
    }

    private String getJdbcUrl(GenericContainer<?> cockroachContainer) {
        return ("jdbc:postgresql" +
                "://" +
                cockroachContainer.getHost() +
                ":" +
                cockroachContainer.getMappedPort(DB_PORT) +
                "/postgres?sslmode=disable&user=root");
    }
}
