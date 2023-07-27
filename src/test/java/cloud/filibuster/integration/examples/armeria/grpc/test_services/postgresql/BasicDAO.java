package cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access object used by 'CockroachClientService'.  Abstraction over some
 * common CockroachDB operations, including:
 * - Auto-handling transaction retries in the 'runSQL' method
 * - Example of bulk inserts in the 'bulkInsertRandomAccountData' method
 * Based on <a href="https://www.cockroachlabs.com/docs/v23.1/build-a-java-app-with-cockroachdb.html">
 * CockroachDB official documentation</a>
 */
public class BasicDAO {
    private static final int MAX_RETRY_COUNT = 3;
    private static final String RETRY_SQL_STATE = "40001";
    private static final boolean FORCE_RETRY = false;
    private DataSource ds;
    private Connection connection;
    private static final Logger logger = Logger.getLogger(BasicDAO.class.getName());

    private final Random rand = new Random();

    BasicDAO(DataSource ds) {
        this.ds = ds;
        String dbInit = "CREATE TABLE accounts (id UUID PRIMARY KEY, balance INT8)";
        runSQL(dbInit);
    }

    /**
     * Set the DataSource of the DAO.
     */
    public void setDS(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Set the connection of the DAO.
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    private Connection getConnection() throws SQLException {
        if (this.connection != null) {
            return this.connection;
        }

        return ds.getConnection();
    }

    /**
     * Run SQL code in a way that automatically handles the
     * transaction retry logic so we don't have to duplicate it in
     * various places.
     *
     * @param sqlCode a String containing the SQL code you want to
     *                execute.  Can have placeholders, e.g., "INSERT INTO accounts
     *                (id, balance) VALUES (?, ?)".
     * @param args    String Varargs to fill in the SQL code's
     *                placeholders.
     */
    public void runSQL(String sqlCode, Object... args) {

        // This block is only used to emit class and method names in
        // the program output.  It is not necessary in production
        // code.
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement elem = stacktrace[2];
        String callerClass = elem.getClassName();
        String callerMethod = elem.getMethodName();

        try (Connection connection = getConnection()) {

            // We're managing the commit lifecycle ourselves so we can
            // automatically issue transaction retries.
            connection.setAutoCommit(false);

            int retryCount = 0;

            while (retryCount <= MAX_RETRY_COUNT) {

                if (retryCount == MAX_RETRY_COUNT) {
                    String err = String.format("hit max of %s retries, aborting", MAX_RETRY_COUNT);
                    throw new RuntimeException(err);
                }

                // This block is only used to test the retry logic.
                // It is not necessary in production code.  See also
                // the method 'testRetryHandling()'.
                if (FORCE_RETRY) {
                    forceRetry(connection); // SELECT 1
                }

                try (PreparedStatement pstmt = connection.prepareStatement(sqlCode)) {

                    // Loop over the args and insert them into the
                    // prepared statement based on their types.  In
                    // this simple example we classify the argument
                    // types as "integers" and "everything else"
                    // (a.k.a. strings or uuid).
                    for (int i = 0; i < args.length; i++) {
                        int place = i + 1;

                        try {
                            int val = Integer.parseInt(args[i].toString());
                            pstmt.setInt(place, val);
                        } catch (NumberFormatException e) {
                            pstmt.setObject(place, args[i]);
                        }
                    }

                    if (pstmt.execute()) {
                        // We know that `pstmt.getResultSet()` will
                        // not return `null` if `pstmt.execute()` was
                        // true
                        ResultSet rs = pstmt.getResultSet();
                        ResultSetMetaData rsmeta = rs.getMetaData();
                        int colCount = rsmeta.getColumnCount();

                        // This printed output is for debugging and/or demonstration
                        // purposes only.  It would not be necessary in production code.
                        logger.log(Level.INFO, String.format("\n%s.%s:\n    '%s'\n", callerClass, callerMethod, pstmt));

                        while (rs.next()) {
                            for (int i = 1; i <= colCount; i++) {
                                String name = rsmeta.getColumnName(i);
                                String type = rsmeta.getColumnTypeName(i);

                                // In this "bank account" example we know we are only handling
                                // integer values (technically 64-bit INT8s, the CockroachDB
                                // default).  This code could be made into a switch statement
                                // to handle the various SQL types needed by the application.
                                if (type.equals("int8")) {
                                    int val = rs.getInt(name);

                                    // This printed output is for debugging and/or demonstration
                                    // purposes only.  It would not be necessary in production code.
                                    logger.log(Level.INFO, String.format("    %-8s => %10s\n", name, val));
                                }
                            }
                        }
                    } else {
                        // This printed output is for debugging and/or demonstration
                        // purposes only.  It would not be necessary in production code.
                        logger.log(Level.INFO, String.format("\n%s.%s:\n    '%s'\n", callerClass, callerMethod, pstmt));
                    }

                    connection.commit();
                    break;

                } catch (SQLException e) {

                    if (e.getSQLState().equals(RETRY_SQL_STATE)) {
                        // Since this is a transaction retry error, we
                        // roll back the transaction and sleep a
                        // little before trying again.  Each time
                        // through the loop we sleep for a little
                        // longer than the last time
                        // (A.K.A. exponential backoff).
                        logger.log(Level.INFO, String.format("retryable exception occurred:\n    sql state = [%s]\n    message = [%s]\n    retry counter = %s\n", e.getSQLState(), e.getMessage(), retryCount));
                        connection.rollback();
                        retryCount++;
                        int sleepMillis = (int) (Math.pow(2, retryCount) * 100) + rand.nextInt(100);
                        logger.log(Level.INFO, "Hit 40001 transaction retry error, sleeping %s milliseconds\n", sleepMillis);
                        try {
                            Thread.sleep(sleepMillis);
                        } catch (InterruptedException ignored) {
                            // Necessary to allow the Thread.sleep()
                            // above so the retry loop can continue.
                        }

                    } else {
                        throw e;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.INFO, String.format("BasicDAO.runSQL ERROR: { state => %s, cause => %s, message => %s }\n",
                    e.getSQLState(), e.getCause(), e.getMessage()));
        }
    }

    /**
     * Helper method called by 'testRetryHandling'.  It simply issues
     * a "SELECT 1" inside the transaction to force a retry.  This is
     * necessary to take the connection's session out of the AutoRetry
     * state, since otherwise the other statements in the session will
     * be retried automatically, and the client (us) will not see a
     * retry error. Note that this information is taken from the
     * following test:
     * <a href="https://github.com/cockroachdb/cockroach/blob/master/pkg/sql/logictest/testdata/logic_test/manual_retry">...</a>
     *
     * @param connection Connection
     */
    private static void forceRetry(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
            statement.executeQuery();
        }
    }

    /**
     * Update accounts by passing in a Map of (ID, Balance) pairs.
     *
     * @param accounts (Map)
     */
    public void updateAccounts(Map<UUID, Integer> accounts) {
        for (Map.Entry<UUID, Integer> account : accounts.entrySet()) {
            UUID k = account.getKey();
            String v = account.getValue().toString();

            Object[] removeArgs = {k};
            runSQL("DELETE FROM accounts WHERE id = ?", removeArgs);

            Object[] args = {k, v};
            runSQL("INSERT INTO accounts (id, balance) VALUES (?, ?)", args);
        }
    }

    /**
     * Delete account by UUID.
     *
     * @param account (UUID)
     */
    public void deleteAccount(UUID account) {
        Object[] removeArgs = {account};
        runSQL("DELETE FROM accounts WHERE id = ?", removeArgs);
    }

    /**
     * Transfer funds between one account and another.  Handles
     * transaction retries in case of conflict automatically on the
     * backend.
     *
     * @param fromId (UUID)
     * @param toId   (UUID)
     * @param amount (int)
     */
    public void transferFunds(UUID fromId, UUID toId, int amount) {

        // We have omitted explicit BEGIN/COMMIT statements for
        // brevity.  Individual statements are treated as implicit
        // transactions by CockroachDB (see
        // https://www.cockroachlabs.com/docs/stable/transactions.html#individual-statements).

        String sqlCode = "INSERT INTO accounts (id, balance) VALUES" +
                "(?, ((SELECT balance FROM accounts WHERE id = ?) - ?))," +
                "(?, ((SELECT balance FROM accounts WHERE id = ?) + ?))" +
                "ON CONFLICT (id) DO UPDATE SET balance = excluded.balance";

        runSQL(sqlCode, fromId, fromId, amount, toId, toId, amount);
    }

    /**
     * Get the account balance for one account.
     * <p>
     * We skip using the retry logic in 'runSQL()' here for the
     * following reasons:
     * <p>
     * 1. Since this is a single read ("SELECT"), we don't expect any
     * transaction conflicts to handle
     * <p>
     * 2. We need to return the balance as an integer
     *
     * @param id (UUID)
     * @return balance (int)
     */
    public int getAccountBalance(UUID id) {
        int balance = 0;

        try (Connection connection = ds.getConnection()) {

            // Check the current balance.
            ResultSet res = connection.createStatement()
                    .executeQuery(String.format("SELECT balance FROM accounts WHERE id = '%s'", id.toString()));
            if (!res.next()) {
                logger.log(Level.INFO, "No users in the table with id " + id);
            } else {
                balance = res.getInt("balance");
            }
        } catch (SQLException e) {
            logger.log(Level.INFO, String.format("BasicDAO.getAccountBalance ERROR: { state => %s, cause => %s, message => %s }\n",
                    e.getSQLState(), e.getCause(), e.getMessage()));
        }

        return balance;
    }

}
