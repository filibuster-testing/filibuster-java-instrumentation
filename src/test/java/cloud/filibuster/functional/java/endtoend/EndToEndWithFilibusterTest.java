package cloud.filibuster.functional.java.endtoend;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.BasicDAO;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.CockroachClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.PurchaseCustomAnalysisConfigurationFile;
import org.grpcmock.junit5.GrpcMockExtension;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.grpcmock.GrpcMock.verifyThat;
import static org.grpcmock.definitions.verification.CountMatcher.times;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndWithFilibusterTest extends BaseTest {
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    private static BasicDAO COCKROACH_DAO;

    private static final UUID SESSION_ID = UUID.randomUUID();

    private static final UUID CONSUMER_UUID = UUID.randomUUID();

    private static final UUID MERCHANT_UUID = UUID.randomUUID();

    private static final UUID CART_UUID = UUID.randomUUID();

    @BeforeEach
    public void seedBalances() {
        COCKROACH_DAO = CockroachClientService.getInstance().dao;

        Map<UUID, Integer> balances = new HashMap<>();
        balances.put(CONSUMER_UUID, 20000);
        balances.put(MERCHANT_UUID, 0);
        COCKROACH_DAO.updateAccounts(balances);

        assertEquals(0, COCKROACH_DAO.getAccountBalance(MERCHANT_UUID));
        assertEquals(20000, COCKROACH_DAO.getAccountBalance(CONSUMER_UUID));
    }

    @TestWithFilibuster(
            analysisConfigurationFile = PurchaseCustomAnalysisConfigurationFile.class,
            maxIterations = 1,
            suppressCombinations = true,
            avoidRedundantInjections = true
    )
    public void testEndToEnd() {
        // Stub dependencies.
        stubFor(unaryMethod(UserServiceGrpc.getGetUserFromSessionMethod())
                .willReturn(Hello.GetUserResponse.newBuilder()
                        .setUserId(CONSUMER_UUID.toString())
                        .build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetCartForSessionMethod())
                .willReturn(Hello.GetCartResponse.newBuilder()
                        .setCartId(CART_UUID.toString())
                        .setTotal("10000")
                        .setMerchantId(MERCHANT_UUID.toString())
                        .build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetDiscountOnCartMethod())
                .willReturn(Hello.GetDiscountResponse.newBuilder()
                        .setPercent("10")
                        .build()));

        // Issue RPC.
        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(API_CHANNEL);
        Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder().setSessionId(SESSION_ID.toString()).build();
        Hello.PurchaseResponse response = blockingStub.purchase(request);

        // Verify RPC response.
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals("9000", response.getTotal());

        // Verify cache write was performed.
        String redisRecord = RedisClientService.getInstance().redisClient.connect().sync().get("last_purchase_user_" + CONSUMER_UUID);
        JSONObject jsonObject = new JSONObject(redisRecord);
        JSONObject expectedJsonObject = new JSONObject();
        expectedJsonObject.put("cart_id", CART_UUID.toString());
        expectedJsonObject.put("user_id", CONSUMER_UUID.toString());
        expectedJsonObject.put("purchased", true);
        expectedJsonObject.put("total", "9000");
        assertTrue(jsonObject.similar(expectedJsonObject));

        // Verify database writes were performed.
        assertEquals(11000, COCKROACH_DAO.getAccountBalance(CONSUMER_UUID));
        assertEquals(9000, COCKROACH_DAO.getAccountBalance(MERCHANT_UUID));

        // Verify expected invocations.
        verifyThat(UserServiceGrpc.getGetUserFromSessionMethod(), times(4));
        verifyThat(CartServiceGrpc.getGetCartForSessionMethod(), times(1));
        verifyThat(CartServiceGrpc.getGetDiscountOnCartMethod(), times(1));
    }
}
