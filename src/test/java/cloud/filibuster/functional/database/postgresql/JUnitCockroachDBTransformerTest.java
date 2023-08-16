package cloud.filibuster.functional.database.postgresql;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.BasicDAO;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.CockroachClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.postgresql.PostgresTransformStringAnalysisConfigurationFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitCockroachDBTransformerTest extends JUnitAnnotationBaseTest {

    private static int numberOfTestExecutions = 0;
    private final static Set<String> testExceptionsThrown = new HashSet<>();
    private static BasicDAO cockroachDAO;
    final int initBalance1 = 1000;
    final int initBalance2 = 250;

    @BeforeAll
    public static void getCockroachConnection() {
        cockroachDAO = CockroachClientService.getInstance().dao;
        cockroachDAO.isIntercepted(true);
    }

    @DisplayName("Inject String transformer faults in CockroachDB.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = PostgresTransformStringAnalysisConfigurationFile.class)
    public void testCockroachConnection() {
        try {
            numberOfTestExecutions++;

            // Manually insert two accounts
            Map<UUID, Integer> balances = new HashMap<>();
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            balances.put(id1, initBalance1);
            balances.put(id2, initBalance2);
            cockroachDAO.updateAccounts(balances);

            // Assert account balances were set correctly
            assertEquals(initBalance1, cockroachDAO.getAccountBalance(id1));
            assertEquals(initBalance2, cockroachDAO.getAccountBalance(id2));
//
//            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
        }
    }
//
//    @DisplayName("Verify correct number of test executions.")
//    @Test
//    @Order(2)
//    public void testNumExecutions() {
//        // 1 fault free execution + 4 execution with injected faults
//        assertEquals(5, numberOfTestExecutions);
//    }
//
//    @DisplayName("Verify correct number of generated Filibuster tests.")
//    @Test
//    @Order(3)
//    public void testNumExceptions() {
//        assertEquals(2, testExceptionsThrown.size());
//    }
}
