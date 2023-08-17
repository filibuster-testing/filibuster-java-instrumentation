package cloud.filibuster.integration.examples.armeria.grpc.test_services;

import com.datastax.oss.driver.api.core.CqlSession;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

public class CassandraClientService {
    public static final Integer CQL_PORT = 9042;

    private static final String DEFAULT_LOCAL_DATACENTER = "datacenter1";

    public CqlSession cassandraClient;
    public String connectionString;

    @Nullable
    private static CassandraClientService single_instance = null;

    @SuppressWarnings("resource")
    private CassandraClientService() {
        GenericContainer<?> cassandraContainer = new GenericContainer<>(DockerImageName.parse("cassandra:4.1.2"))
                .withExposedPorts(CQL_PORT)
                .withEnv("CASSANDRA_SNITCH", "GossipingPropertyFileSnitch")
                .withEnv("JVM_OPTS", "-Dcassandra.skip_wait_for_gossip_to_settle=0 -Dcassandra.initial_token=0")
                .withEnv("HEAP_NEWSIZE", "128M")
                .withEnv("MAX_HEAP_SIZE", "1024M")
                .withEnv("CASSANDRA_ENDPOINT_SNITCH", "GossipingPropertyFileSnitch")
                .withEnv("CASSANDRA_DC", DEFAULT_LOCAL_DATACENTER);

        cassandraContainer.start();

        connectionString = getContactPoint(cassandraContainer).toString();
        cassandraClient = CqlSession
                .builder()
                .addContactPoint(getContactPoint(cassandraContainer))
                .withLocalDatacenter(DEFAULT_LOCAL_DATACENTER)
                .build();
    }

    // Static method to create instance of Singleton class
    public static synchronized CassandraClientService getInstance() {
        if (single_instance == null) {
            single_instance = new CassandraClientService();
        }

        return single_instance;
    }

    @SuppressWarnings("AddressSelection")
    private static InetSocketAddress getContactPoint(GenericContainer<?> container) {
        return new InetSocketAddress(container.getHost(), container.getMappedPort(CQL_PORT));
    }
}
