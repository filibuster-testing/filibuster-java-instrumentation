package cloud.filibuster.unit.databases;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.BasicDAO;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.CockroachClientService;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.PostgreSQLClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgreSQLAccessTest extends JUnitAnnotationBaseTest {

    final int initBalance1 = 1000;
    final int initBalance2 = 250;
    final int transferAmount = 50;

    @Test
    @DisplayName("Tests whether CockroachDB/PostgreSQL connection can read and write")
    @Order(1)
    public void testPostgresAndCockroachAccess() {
        BasicDAO cockroachDAO = CockroachClientService.getInstance().dao;
        BasicDAO postgresDAO = PostgreSQLClientService.getInstance().dao;

        ImmutableList.of(cockroachDAO, postgresDAO).forEach(this::executeTest);
    }

    private void executeTest(BasicDAO dao) {
        // Manually insert two accounts
        Map<UUID, Integer> balances = new HashMap<>();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        balances.put(id1, initBalance1);
        balances.put(id2, initBalance2);
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
