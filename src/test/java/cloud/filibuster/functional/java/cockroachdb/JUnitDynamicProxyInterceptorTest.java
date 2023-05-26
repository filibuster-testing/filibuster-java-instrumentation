package cloud.filibuster.functional.java.cockroachdb;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.cockroachdb.CockroachClientService;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import static cloud.filibuster.instrumentation.Constants.COCKROACH_MODULE_NAME;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitDynamicProxyInterceptorTest extends JUnitAnnotationBaseTest {

    private static Connection connection;
    private static String connectionString;

    @BeforeAll
    public static void beforeAll() throws SQLException {
        PGSimpleDataSource cockroachClient = CockroachClientService.getInstance().cockroachClient;
        connection = cockroachClient.getConnection();
        connectionString = CockroachClientService.getInstance().connectionString;
    }

    @Test
    @DisplayName("Tests whether CockroachDB connection is intercepted")
    @Order(1)
    public void testInterceptConnection() throws SQLException {
        Connection interceptedConnection = DynamicProxyInterceptor.createInterceptor(connection, connectionString, COCKROACH_MODULE_NAME);
        interceptedConnection.getSchema();
    }

}
