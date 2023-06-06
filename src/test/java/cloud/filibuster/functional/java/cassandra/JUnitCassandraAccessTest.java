package cloud.filibuster.functional.java.cassandra;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.CassandraClientService;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitCassandraAccessTest extends JUnitAnnotationBaseTest {

    @Test
    @DisplayName("Tests whether Cassandra connection can create keyspace")
    @Order(1)
    public void testCassandraAccess() {
        CqlSession cassandraSession = CassandraClientService.getInstance().cassandraClient;

        cassandraSession.execute(
                "CREATE KEYSPACE IF NOT EXISTS test WITH replication = \n" +
                        "{'class':'SimpleStrategy','replication_factor':'1'};"
        );

        KeyspaceMetadata keyspace = cassandraSession.getMetadata().getKeyspaces().get(CqlIdentifier.fromCql("test"));

        assertNotNull(keyspace, "Keyspace was not created");
    }
}
