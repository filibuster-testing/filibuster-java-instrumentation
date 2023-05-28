package cloud.filibuster.functional.java.dynamodb;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.cockroachdb.CockroachClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.cockroachdb.CockroachSingleFaultPSQLExceptionAnalysisConfigurationFile;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PSQLException;

import java.sql.Connection;
import java.sql.SQLException;

import static cloud.filibuster.instrumentation.Constants.DYNAMO_MODULE_NAME;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitDynamoDBTest extends JUnitAnnotationBaseTest {

    private static Connection connection;
    private static String connectionString;


    @BeforeAll
    public static void beforeAll() throws SQLException {
        PGSimpleDataSource cockroachClient = CockroachClientService.getInstance().cockroachClient;
        connection = cockroachClient.getConnection();
        connectionString = CockroachClientService.getInstance().connectionString;
    }

    @DisplayName("Inject basic PSQLException in DynamoDB")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = CockroachSingleFaultPSQLExceptionAnalysisConfigurationFile.class)
    public void testInterceptConnection() {
        try {
            Connection interceptedConnection = DynamicProxyInterceptor.createInterceptor(connection, connectionString, DYNAMO_MODULE_NAME);
            interceptedConnection.getSchema();
            assertFalse(wasFaultInjected());
        } catch (Exception t) {
            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertTrue(wasFaultInjectedOnService(DYNAMO_MODULE_NAME), "Fault was not injected on the cockroach module: " + t);
            assertTrue(wasFaultInjectedOnMethod(DYNAMO_MODULE_NAME, "java.sql.Connection.getSchema"), "Fault was not injected on the expected method: " + t);
            assertTrue(t instanceof PSQLException, "Fault was not of the correct type: " + t);
        }
    }

}
