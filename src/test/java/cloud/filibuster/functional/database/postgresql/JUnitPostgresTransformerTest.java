package cloud.filibuster.functional.database.postgresql;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.BasicDAO;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.PostgreSQLClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.postgresql.PostgresTransformStringAnalysisConfigurationFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitPostgresTransformerTest extends JUnitAnnotationBaseTest {

    private static int numberOfTestExecutions = 0;
    private final static List<String> testExceptionsThrown = new ArrayList<>();
    private static BasicDAO postgresDAO;
    final int initBalance1 = 1000;
    final int initBalance2 = 250;
    private static final UUID id1 = UUID.randomUUID();
    private static final UUID id2 = UUID.randomUUID();

    @BeforeAll
    public static void getCockroachConnection() {
        postgresDAO = PostgreSQLClientService.getInstance().dao;
        postgresDAO.isIntercepted(true);
    }

    @DisplayName("Inject String transformer faults in CockroachDB.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = PostgresTransformStringAnalysisConfigurationFile.class)
    public void testCockroachConnection() {
        try {
            numberOfTestExecutions++;

            // Manually insert two accounts
            Map<UUID, Integer> balances = new HashMap<>();
            balances.put(id1, initBalance1);
            balances.put(id2, initBalance2);
            postgresDAO.updateAccounts(balances);

            // Assert account balances were set correctly
            assertEquals(initBalance1, postgresDAO.getAccountBalance(id1));
            assertEquals(initBalance2, postgresDAO.getAccountBalance(id2));

            // Assert that the correct account IDs are returned for the given balances
            assertEquals(Collections.singletonList(id1.toString()), postgresDAO.getAccountIdByBalance(initBalance1));
            assertEquals(Collections.singletonList(id2.toString()), postgresDAO.getAccountIdByBalance(initBalance2));

            // Remove all accounts from DB
            postgresDAO.deleteAllAccounts();

            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        assertEquals(80, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(79, testExceptionsThrown.size());
    }
}
