package cloud.filibuster.functional.java.basic;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.junit.TestWithFilibuster;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NoRPCsTest extends JUnitAnnotationBaseTest {
    @TestWithFilibuster()
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        // Nothing.
    }
}