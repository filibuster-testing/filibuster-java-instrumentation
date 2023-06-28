package cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql;

import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.lettuce.core.api.StatefulRedisConnection;
import org.json.JSONObject;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;

public class PurchaseWorkflow {
    public enum PurchaseWorkflowResponse {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        UNPROCESSED,
        UNAVAILABLE
    }

    public static void depositFundsToAccount(UUID account, int amount) {
        Map<UUID, Integer> balances = new HashMap<>();
        balances.put(account, amount);
        BasicDAO dao = getCockroachDAO();
        dao.updateAccounts(balances);
    }

    public static int getAccountBalance(UUID account) {
        BasicDAO dao = getCockroachDAO();
        return dao.getAccountBalance(account);
    }

    public static void deleteAccount(UUID account) {
        BasicDAO dao = getCockroachDAO();
        dao.deleteAccount(account);
    }

    public static JSONObject getCacheObjectForUser(UUID consumer) {
        StatefulRedisConnection<String, String> connection = getRedisConnection();
        String redisValue = connection.sync().get(cacheKeyForConsumer(consumer.toString()));
        return new JSONObject(redisValue);
    }

    public static void resetCacheObjectForUser(UUID consumer) {
        StatefulRedisConnection<String, String> connection = getRedisConnection();
        connection.sync().set(cacheKeyForConsumer(consumer.toString()), null);
    }

    private static String cacheKeyForConsumer(String consumer) {
        return "last_purchase_for_user_" + consumer;
    }

    public static List<Map.Entry<String, String>> getDiscountCodes() {
        ArrayList<Map.Entry<String, String>> discountCodes = new ArrayList<>();
        discountCodes.add(Pair.of("FIRST-TIME", "10"));
        discountCodes.add(Pair.of("RETURNING", "5"));
        return discountCodes;
    }

    private final String sessionId;

    private final Channel channel;

    private final StatefulRedisConnection<String, String> connection;

    private final BasicDAO dao;

    private int purchaseTotal = 0;

    private PurchaseWorkflowResponse purchaseWorkflowResponse = PurchaseWorkflowResponse.UNPROCESSED;

    public PurchaseWorkflow(String sessionId) {
        this.sessionId = sessionId;
        this.channel = getRpcChannel();
        this.connection = getRedisConnection();
        this.dao = getCockroachDAO();
    }

    public PurchaseWorkflowResponse execute() {
        String userId;
        String cartId;
        String merchantId;
        int cartTotal;

        // Make call to get the user.
        try {
            userId = getUserFromSession(channel, sessionId);
        } catch (StatusRuntimeException e) {
            return PurchaseWorkflowResponse.UNAVAILABLE;
        }

        // Get cart.
        try {
            Hello.GetCartResponse getCartResponse = getCartFromSession(channel, sessionId);
            cartId = getCartResponse.getCartId();
            merchantId = getCartResponse.getMerchantId();
            cartTotal = Integer.parseInt(getCartResponse.getTotal());
        } catch (StatusRuntimeException e) {
            return PurchaseWorkflowResponse.UNAVAILABLE;
        }

        // Get the maximum discount.
        int maxDiscountPercentage = 0;

        for (Map.Entry<String, String> discountCode : PurchaseWorkflow.getDiscountCodes()) {
            try {
                Hello.GetDiscountResponse getDiscountResponse = getDiscountOnCart(channel, discountCode.getKey());
                int discountPercentage = Integer.parseInt(getDiscountResponse.getPercent());
                maxDiscountPercentage = Integer.max(maxDiscountPercentage, discountPercentage);
            } catch (StatusRuntimeException e) {
                // Nothing, ignore discount failure.
            }
        }

        // Apply discount.
        float discountPct = maxDiscountPercentage / 100.00F;
        float discountAmount = cartTotal * discountPct;
        cartTotal = cartTotal - (int) discountAmount;

        // Verify the user has sufficient funds.
        int userAccountBalance = dao.getAccountBalance(UUID.fromString(userId));

        if (userAccountBalance < cartTotal) {
            return PurchaseWorkflowResponse.INSUFFICIENT_FUNDS;
        }

        // Write cache record to Redis with information on last purchase.
        JSONObject redisRecord = new JSONObject();
        redisRecord.put("purchased", true);
        redisRecord.put("user_id", userId);
        redisRecord.put("cart_id", cartId);
        redisRecord.put("total", String.valueOf(cartTotal));
        connection.sync().set(cacheKeyForConsumer(userId), redisRecord.toString(4));

        // Write record to CRDB.
        dao.transferFunds(UUID.fromString(userId), UUID.fromString(merchantId), cartTotal);

        // Update purchase total and response.
        purchaseTotal = cartTotal;
        purchaseWorkflowResponse = PurchaseWorkflowResponse.SUCCESS;

        // Return success.
        return purchaseWorkflowResponse;
    }

    public static class PurchaseRuntimeException extends RuntimeException {
        public PurchaseRuntimeException(String message) {
            super(message);
        }
    }

    public int getPurchaseTotal() {
        if (purchaseWorkflowResponse == PurchaseWorkflowResponse.SUCCESS) {
            return purchaseTotal;
        }

        throw new PurchaseRuntimeException("Purchase was not completed successfully.");
    }

    private static Channel getRpcChannel() {
        ManagedChannel originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("mock"), Networking.getPort("mock"))
                .usePlaintext()
                .build();

        if (getInstrumentationServerCommunicationEnabledProperty()) {
            ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("api_server");
            return ClientInterceptors.intercept(originalChannel, clientInterceptor);
        } else {
            return originalChannel;
        }
    }

    private static StatefulRedisConnection<String, String> getRedisConnection() {
        RedisClientService redisClient = RedisClientService.getInstance();

        if (getInstrumentationServerCommunicationEnabledProperty()) {
            return DynamicProxyInterceptor.createInterceptor(redisClient.redisClient.connect(), redisClient.connectionString);
        } else {
            return RedisClientService.getInstance().redisClient.connect();
        }
    }

    private static BasicDAO getCockroachDAO() {
        CockroachClientService cockroachClientService = CockroachClientService.getInstance();

        if (getInstrumentationServerCommunicationEnabledProperty()) {
            DataSource interceptedDS = DynamicProxyInterceptor.createInterceptor(cockroachClientService.dataSource,
                    cockroachClientService.connectionString);
            cockroachClientService.dao.setDS(interceptedDS);
        }
        return cockroachClientService.dao;
    }

    private static String getUserFromSession(Channel channel, String sessionId) {
        UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub = UserServiceGrpc.newBlockingStub(channel);
        Hello.GetUserRequest request = Hello.GetUserRequest.newBuilder().setSessionId(sessionId).build();
        Hello.GetUserResponse response = userServiceBlockingStub.getUserFromSession(request);
        return response.getUserId();
    }

    private static Hello.GetCartResponse getCartFromSession(Channel channel, String sessionId) {
        CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub = CartServiceGrpc.newBlockingStub(channel);
        Hello.GetCartRequest request = Hello.GetCartRequest.newBuilder().setSessionId(sessionId).build();
        return cartServiceBlockingStub.getCartForSession(request);
    }

    private static Hello.GetDiscountResponse getDiscountOnCart(Channel channel, String discountCode) {
        CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub = CartServiceGrpc.newBlockingStub(channel);
        Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder().setCode(discountCode).build();
        return cartServiceBlockingStub.getDiscountOnCart(request);
    }
}
