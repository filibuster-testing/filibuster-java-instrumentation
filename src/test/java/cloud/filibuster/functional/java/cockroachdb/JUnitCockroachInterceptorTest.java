package cloud.filibuster.functional.java.cockroachdb;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.cockroachdb.CockroachInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.cockroachdb.CockroachClientService;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitCockroachInterceptorTest extends JUnitAnnotationBaseTest {

    @Test
    @DisplayName("Tests whether CockroachDB connection is intercepted")
    @Order(1)
    public void testRedisSync() throws SQLException {

        PGSimpleDataSource cockroachClient = CockroachClientService.getInstance().cockroachClient;
        Connection connection = cockroachClient.getConnection();
        Connection interceptedConnection = CockroachInterceptor.createInterceptor(connection, Connection.class);

        assertEquals("InterceptedData", interceptedConnection.getSchema());
    }

}
