package cloud.filibuster.functional.java.cockroachdb;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.CockroachClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitCockroachAccessTest extends JUnitAnnotationBaseTest {
    @Test
    @DisplayName("Tests whether CockroachDB connection can read and write")
    @Order(1)
    public void testRedisSync() {
        CockroachClientService cockroachClientService = CockroachClientService.getInstance();
    }

}
