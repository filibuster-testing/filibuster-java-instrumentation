package cloud.filibuster.functional.java.cockroachdb;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.cockroachdb.CockroachInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.cockroachdb.CockroachClientService;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitCockroachInterceptorTest extends JUnitAnnotationBaseTest {

    private static Connection connection;

    @BeforeAll
    public static void beforeAll() throws SQLException {
        PGSimpleDataSource cockroachClient = CockroachClientService.getInstance().cockroachClient;
        connection = cockroachClient.getConnection();
    }

    @Test
    @DisplayName("Tests whether CockroachDB connection is intercepted")
    @Order(1)
    public void testInterceptConnection() throws SQLException {
        Connection interceptedConnection = CockroachInterceptor.createInterceptor(connection);
        interceptedConnection.getSchema();
    }

}
