package cloud.filibuster.functional.java.postgresql;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.PostgreSQLClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.postgresql.PostgreSQLSingleFaultPSQLExceptionAnalysisConfigurationFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PSQLException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitPostgreSQLInterceptorTest extends JUnitAnnotationBaseTest {

    private static int numberOfTestExecutions = 0;
    private final static Set<String> testExceptionsThrown = new HashSet<>();
    private static Connection pgConnection;
    private static String pgString;

    @BeforeAll
    public static void getPGConnection() throws SQLException {
        PGSimpleDataSource postgresqlClient = PostgreSQLClientService.getInstance().postgresqlClient;
        pgConnection = postgresqlClient.getConnection();
        pgString = PostgreSQLClientService.getInstance().connectionString;
    }

    @DisplayName("Inject basic PSQLException in native PostgreSQL.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = PostgreSQLSingleFaultPSQLExceptionAnalysisConfigurationFile.class)
    public void testPostgresConnection() {
        try {
            numberOfTestExecutions++;

            Connection interceptedConnection = DynamicProxyInterceptor.createInterceptor(pgConnection, pgString);
            interceptedConnection.getSchema();
            assertFalse(wasFaultInjected());
        } catch (Exception t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertThrows(FilibusterUnsupportedAPIException.class, () -> wasFaultInjectedOnService("java.sql.Connection"), "Expected FilibusterUnsupportedAPIException to be thrown: " + t);
            assertTrue(wasFaultInjectedOnMethod("java.sql.Connection/getSchema"), "Fault was not injected on the expected method: " + t);
            assertTrue(t instanceof PSQLException, "Fault was not of the correct type: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        // 1 fault free execution + 1 fault injected execution
        assertEquals(2, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(1, testExceptionsThrown.size());
    }
}
