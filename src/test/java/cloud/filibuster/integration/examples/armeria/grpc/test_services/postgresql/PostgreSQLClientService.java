package cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql;

import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nullable;

public class PostgreSQLClientService {
    private static final int DB_PORT = 5432;
    private static final String DB_USER = "root";
    private static final String DB_NAME = "postgres";
    public PGSimpleDataSource postgresqlClient;
    public String connectionString;
    public BasicDAO dao;
    @Nullable
    private static PostgreSQLClientService single_instance = null;

    @SuppressWarnings("resource")
    private PostgreSQLClientService() {
        // Start a PostgreSQL container
        GenericContainer<?> postgresContainer = new GenericContainer<>(DockerImageName
                .parse("postgres:13.11"))
                .withExposedPorts(DB_PORT)
                .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
                .withEnv("POSTGRES_DB", DB_NAME)
                .withEnv("POSTGRES_USER", DB_USER);

        // Turn off "force synchronization" as persisting data to disk is not necessary in a test container
        postgresContainer.setCommand("postgres", "-c", "fsync=off");

        postgresContainer.start();

        // Get the JDBC URL
        connectionString = getJdbcUrl(postgresContainer);

        // Configure the database connection
        postgresqlClient = new PGSimpleDataSource();
        postgresqlClient.setUrl(connectionString);
        postgresqlClient.setApplicationName("NativePostgreSQL");

        // Create a basic DAO
        dao = new BasicDAO(postgresqlClient);
    }

    // Static method to create instance of Singleton class
    public static synchronized PostgreSQLClientService getInstance() {
        if (single_instance == null) {
            single_instance = new PostgreSQLClientService();
        }

        return single_instance;
    }

    private static String getJdbcUrl(GenericContainer<?> postgresContainer) {
        return ("jdbc:postgresql" +
                "://" +
                postgresContainer.getHost() +
                ":" +
                postgresContainer.getMappedPort(DB_PORT) +
                "/" +
                DB_NAME +
                "?sslmode=disable&user=" +
                DB_USER);
    }
}
