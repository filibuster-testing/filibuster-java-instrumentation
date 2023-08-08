package cloud.filibuster.functional.java.cassandra;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.CassandraClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.cassandra.CassandraAnalysisConfigurationFile;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.datastax.oss.driver.api.core.servererrors.OverloadedException;
import com.datastax.oss.driver.api.core.servererrors.ReadFailureException;
import com.datastax.oss.driver.api.core.servererrors.ReadTimeoutException;
import com.datastax.oss.driver.api.core.servererrors.WriteFailureException;
import com.datastax.oss.driver.api.core.servererrors.WriteTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitCassandraFilibusterExecutionTest extends JUnitAnnotationBaseTest {

    private static int numberOfTestExecutions = 0;
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    @DisplayName("Tests whether Cassandra interceptor can inject basic exception")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = CassandraAnalysisConfigurationFile.class)
    @SuppressWarnings("CatchingUnchecked")
    public void testCassandraExceptionInjection() {
        try {
            numberOfTestExecutions++;

            CqlSession cassandraSession = CassandraClientService.getInstance().cassandraClient;
            String cassandraString = CassandraClientService.getInstance().connectionString;

            cassandraSession = DynamicProxyInterceptor.createInterceptor(cassandraSession, cassandraString);

            cassandraSession.execute(
                    "CREATE KEYSPACE IF NOT EXISTS test WITH replication = \n" +
                            "{'class':'SimpleStrategy','replication_factor':'1'};"
            );

            KeyspaceMetadata keyspace = cassandraSession.getMetadata().getKeyspaces().get(CqlIdentifier.fromCql("test"));

            assertNotNull(keyspace, "Keyspace was not created");
            assertFalse(wasFaultInjected());
        } catch (Exception t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertThrows(FilibusterUnsupportedAPIException.class, () -> wasFaultInjectedOnService("com.datastax.oss.driver.api.core.CqlSession"), "Expected FilibusterUnsupportedAPIException to be thrown: " + t);
            assertTrue(wasFaultInjectedOnMethod("com.datastax.oss.driver.api.core.CqlSession/execute"), "Fault was not injected on the expected method: " + t);
            assertTrue(t instanceof OverloadedException || t instanceof InvalidQueryException || t instanceof ReadFailureException || t instanceof ReadTimeoutException || t instanceof WriteFailureException || t instanceof WriteTimeoutException,
                    "Fault was not of the correct type: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        // 1 fault free execution + 6 executions with faults injected
        assertEquals(7, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(6, testExceptionsThrown.size());
    }
}
