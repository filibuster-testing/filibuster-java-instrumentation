package cloud.filibuster.functional.java.cockroachdb;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.cockroachdb.BasicDAO;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.cockroachdb.CockroachClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitCockroachAccessTest extends JUnitAnnotationBaseTest {

    final int initBalance1 = 1000;
    final int initBalance2 = 250;
    final int transferAmount = 50;

    @Test
    @DisplayName("Tests whether CockroachDB connection can read and write")
    @Order(1)
    public void testRedisSync() {
        BasicDAO dao = CockroachClientService.getInstance().dao;

        // Manually insert two accounts
        Map<String, String> balances = new HashMap<>();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        balances.put(id1.toString(), "1000");
        balances.put(id2.toString(), "250");
        dao.updateAccounts(balances);

        // Assert account balances were set correctly
        assertEquals(initBalance1, dao.getAccountBalance(id1));
        assertEquals(initBalance2, dao.getAccountBalance(id2));

        // Transfer $100 from account 1 to account 2
        dao.transferFunds(id1, id2, transferAmount);

        // Assert transfer was completed correctly
        assertEquals(initBalance1 - transferAmount, dao.getAccountBalance(id1));
        assertEquals(initBalance2 + transferAmount, dao.getAccountBalance(id2));

    }

}
