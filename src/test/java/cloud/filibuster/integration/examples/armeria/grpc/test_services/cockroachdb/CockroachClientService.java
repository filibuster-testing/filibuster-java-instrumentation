package cloud.filibuster.integration.examples.armeria.grpc.test_services.cockroachdb;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nullable;

import org.postgresql.ds.PGSimpleDataSource;

public class CockroachClientService {
    private static final int DB_PORT = 26257;
    public PGSimpleDataSource cockroachClient;
    public String connectionString;
    public BasicDAO dao;
    @Nullable
    private static CockroachClientService single_instance = null;

    @SuppressWarnings("resource")
    private CockroachClientService() {
        // Start a cockroach container
        GenericContainer<?> cockroachContainer = new GenericContainer<>(DockerImageName
                .parse("cockroachdb/cockroach:latest-v23.1"))
                .withExposedPorts(DB_PORT, 8080)
                .withCommand("start-single-node --insecure");
        cockroachContainer.start();

        // Get the JDBC URL
        connectionString = getJdbcUrl(cockroachContainer);

        // Configure the database connection
        cockroachClient = new PGSimpleDataSource();
        cockroachClient.setUrl(connectionString);
        cockroachClient.setApplicationName("SimpleCockroachApp");

        // Create a basic DAO
        dao = new BasicDAO(cockroachClient);
    }

    // Static method to create instance of Singleton class
    public static synchronized CockroachClientService getInstance() {
        if (single_instance == null) {
            single_instance = new CockroachClientService();
        }

        return single_instance;
    }

    private static String getJdbcUrl(GenericContainer<?> cockroachContainer) {
        return ("jdbc:postgresql" +
                "://" +
                cockroachContainer.getHost() +
                ":" +
                cockroachContainer.getMappedPort(DB_PORT) +
                "/postgres?sslmode=disable&user=root");
    }
}
