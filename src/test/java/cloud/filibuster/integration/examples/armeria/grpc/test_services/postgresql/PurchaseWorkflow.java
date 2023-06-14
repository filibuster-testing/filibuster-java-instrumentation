package cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql;

import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.lettuce.core.api.StatefulRedisConnection;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;

public class PurchaseWorkflow {
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
        return "las_purchase_for_user_" + consumer;
    }

    private final String sessionId;

    private final Channel channel;

    private final StatefulRedisConnection<String, String> connection;

    private final BasicDAO dao;

    public PurchaseWorkflow(String sessionId) {
        this.sessionId = sessionId;
        this.channel = getRpcChannel();
        this.connection = getRedisConnection();
        this.dao = getCockroachDAO();
    }

    public int execute() {
        // Make call to get the user.
        getUserFromSession(channel, sessionId);

        // Make call to get the user, again.
        String userId = getUserFromSession(channel, sessionId);

        // Get cart
        Hello.GetCartResponse getCartResponse = getCartFromSession(channel, sessionId);
        String cartId = getCartResponse.getCartId();
        String merchantId = getCartResponse.getMerchantId();
        int cartTotal = Integer.parseInt(getCartResponse.getTotal());

        // Make call to get the user, again.
        getUserFromSession(channel, sessionId);

        // Set discount.
        try {
            Hello.GetDiscountResponse getDiscountResponse = getDiscountOnCart(channel, cartId);
            int discountPercentage = Integer.parseInt(getDiscountResponse.getPercent());
            float discountPct = discountPercentage / 100.00F;
            float discountAmount = cartTotal * discountPct;
            cartTotal = cartTotal - (int) discountAmount;
        } catch (StatusRuntimeException e) {
            // Nothing, ignore discount failure.
        }

        // Make call to get the user, again.
        getUserFromSession(channel, sessionId);

        // Write cache record to Redis with information on last purchase.
        JSONObject redisRecord = new JSONObject();
        redisRecord.put("purchased", true);
        redisRecord.put("user_id", userId);
        redisRecord.put("cart_id", cartId);
        redisRecord.put("total", String.valueOf(cartTotal));
        connection.sync().set(cacheKeyForConsumer(userId), redisRecord.toString(4));

        // Write record to CRDB.
        dao.transferFunds(UUID.fromString(userId), UUID.fromString(merchantId), cartTotal);

        return cartTotal;
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

    @SuppressWarnings("unchecked")
    private static StatefulRedisConnection<String, String> getRedisConnection() {
        RedisClientService redisClient = RedisClientService.getInstance();

        if (getInstrumentationServerCommunicationEnabledProperty()) {
            return new RedisInterceptorFactory<>(redisClient.redisClient.connect(), redisClient.connectionString)
                    .getProxy(StatefulRedisConnection.class);
        } else {
            return RedisClientService.getInstance().redisClient.connect();
        }
    }

    private static BasicDAO getCockroachDAO() {
        CockroachClientService cockroachClientService = CockroachClientService.getInstance();

        if (getInstrumentationServerCommunicationEnabledProperty()) {
            // TODO: incomplete, needs instrumentation.
            return cockroachClientService.dao;
        } else {
            return cockroachClientService.dao;
        }
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

    private static Hello.GetDiscountResponse getDiscountOnCart(Channel channel, String cartId) {
        CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub = CartServiceGrpc.newBlockingStub(channel);
        Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder().setCartId(cartId).build();
        return cartServiceBlockingStub.getDiscountOnCart(request);
    }
}
