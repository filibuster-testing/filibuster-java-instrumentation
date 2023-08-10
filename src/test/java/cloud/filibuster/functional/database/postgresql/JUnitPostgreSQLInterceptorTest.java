package cloud.filibuster.functional.database.postgresql;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.PostgreSQLClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.postgresql.PostgresAnalysisConfigurationFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PSQLException;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjectedOnJavaClassAndMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    @TestWithFilibuster(analysisConfigurationFile = PostgresAnalysisConfigurationFile.class)
    public void testPostgresConnection() {
        try {
            numberOfTestExecutions++;

            Connection interceptedConnection = DynamicProxyInterceptor.createInterceptor(pgConnection, pgString);
            interceptedConnection.getSchema();

            Driver driver = DriverManager.getDriver(pgString);
            Driver interceptedDriver = DynamicProxyInterceptor.createInterceptor(driver, pgString);

            interceptedDriver.connect(pgString, null);

            assertFalse(wasFaultInjected());
        } catch (Exception t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertTrue(wasFaultInjectedOnJavaClassAndMethod("java.sql.Connection/getSchema") || wasFaultInjectedOnJavaClassAndMethod("java.sql.Driver/connect"), "Fault was not injected on the expected method: " + t);
            assertTrue(t instanceof PSQLException, "Fault was not of the correct type: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        // 1 fault free execution + 4 execution with injected faults
        assertEquals(5, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(2, testExceptionsThrown.size());
    }
}
