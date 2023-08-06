package cloud.filibuster.integration.examples.armeria.grpc.test_services;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.examples.WorldServiceGrpc;
import cloud.filibuster.exceptions.CircuitBreakerException;
import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.functional.java.purchase.PurchaseWorkflow;
import cloud.filibuster.integration.instrumentation.TestHelper;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getRandomSeedProperty;

public class MyAPIService extends APIServiceGrpc.APIServiceImplBase {
    private static final Logger logger = Logger.getLogger(MyAPIService.class.getName());

    private static void handleHelloRequest(
            Hello.HelloRequest req,
            StreamObserver<Hello.HelloReply> responseObserver,
            ManagedChannel originalChannel,
            Channel channel
    ) {
        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloExtendedRequest request = Hello.HelloExtendedRequest.newBuilder().setName(req.getName()).build();
            Hello.HelloExtendedReply helloExtendedReply = blockingStub.compositionalHello(request);

            Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                    .setMessage("Hello, " + helloExtendedReply.getFirstMessage())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

            originalChannel.shutdownNow();
            try {
                while (!originalChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
                    Thread.sleep(4000);
                }
            } catch (InterruptedException ie) {
                logger.log(Level.SEVERE, "Failed to terminate channel: " + ie);
            }

        } catch (StatusRuntimeException e) {
            if (e.getCause() instanceof CircuitBreakerException) {
                Status status = Status.INTERNAL.withDescription(e.toString());
                responseObserver.onError(status.asRuntimeException());
            } else if (e.getStatus().getCode().equals(Status.DATA_LOSS.getCode())) {
                Status status = Status.RESOURCE_EXHAUSTED.withDescription(e.toString());
                responseObserver.onError(status.asRuntimeException());
            } else {
                Status status = Status.FAILED_PRECONDITION.withDescription(e.toString());
                responseObserver.onError(status.asRuntimeException());
            }

            originalChannel.shutdownNow();
            try {
                while (!originalChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
                    Thread.sleep(4000);
                }
            } catch (InterruptedException ie) {
                logger.log(Level.SEVERE, "Failed to terminate channel: " + ie);
            }
        }
    }

    @Override
    public void hello(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("api_server");
        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);
        handleHelloRequest(req, responseObserver, originalChannel, channel);
    }

    @Override
    public void helloWithMock(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello-mock"), Networking.getPort("hello-mock"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("api_server");
        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);
        handleHelloRequest(req, responseObserver, originalChannel, channel);
    }

    @Override
    public void redisHello(Hello.RedisRequest req, StreamObserver<Hello.RedisReply> responseObserver) {
        Hello.RedisReply reply;
        RedisClientService redisService = RedisClientService.getInstance();
        StatefulRedisConnection<String, String> connection = DynamicProxyInterceptor.createInterceptor(redisService.redisClient.connect(), redisService.connectionString);

        String retrievedValue = null;

        try {
            retrievedValue = connection.sync().get(req.getKey());

            if (retrievedValue != null && !retrievedValue.isEmpty()) {  // Return cache value if there is a hit
                reply = Hello.RedisReply.newBuilder().setValue(retrievedValue).build();
            } else {  // Else make a call to the Hello service, the hello service always returns an error
                ManagedChannel helloChannel = ManagedChannelBuilder
                        .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                        .usePlaintext()
                        .build();
                ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("api_server");
                Channel channel = ClientInterceptors.intercept(helloChannel, clientInterceptor);

                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName(req.getKey()).build();
                Hello.HelloReply throwException = blockingStub.throwException(request);

                reply = Hello.RedisReply.newBuilder()
                        .setValue(throwException.getMessage())
                        .build();
            }
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            //Propagate exception back to the caller
            Status status = Status.INTERNAL.withDescription(e.getMessage())
                    .augmentDescription("MyAPIService could not process the request as an exception was thrown. " +
                            "The Redis return value is: " + retrievedValue);
            responseObserver.onError(status.asRuntimeException());
        }
    }

    @Override
    public void redisHelloRetry(Hello.RedisRequest req, StreamObserver<Hello.RedisReply> responseObserver) {
        // API service talks to Redis before making a call to Hello
        Hello.RedisReply reply;
        RedisClientService redisService = RedisClientService.getInstance();
        StatefulRedisConnection<String, String> connection = DynamicProxyInterceptor.createInterceptor(redisService.redisClient.connect(), redisService.connectionString);

        String retrievedValue = null;
        int currentTry = 0;
        int maxTries = 10;

        // Retrieve the value of the given key from Redis, retry up to 10 times in case Redis is overloaded
        while (currentTry < maxTries) {
            try {
                RedisFuture<String> redisFuture = connection.async().get(req.getKey());
                if (redisFuture.await(3, TimeUnit.SECONDS)) {
                    retrievedValue = redisFuture.get();
                    break;  // Break out of the while loop
                }
            } catch (@SuppressWarnings("InterruptedExceptionSwallowed") Exception e) {
                currentTry++;
                if (currentTry < maxTries) {  // If maxTries has not been reached, try again without throwing an exception
                    logger.log(Level.INFO, "An exception was thrown in redisHelloRetry: " + e);
                } else {  // Otherwise, throw an exception
                    Status status = Status.INTERNAL.withDescription(e.getMessage())
                            .augmentDescription("An exception occurred in APIService while retrieving a key" +
                                    "value from Redis - " + e);
                    responseObserver.onError(status.asRuntimeException());
                }
            }
        }

        try {
            ManagedChannel helloChannel = ManagedChannelBuilder
                    .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                    .usePlaintext()
                    .build();
            ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("api_server");
            Channel channel = ClientInterceptors.intercept(helloChannel, clientInterceptor);

            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName(retrievedValue).build();
            Hello.HelloReply throwException = blockingStub.hello(request);

            reply = Hello.RedisReply.newBuilder()
                    .setValue(throwException.getMessage())
                    .build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            // Propagate exception back to the caller
            Status status = Status.INTERNAL.withDescription(e.getMessage())
                    .augmentDescription("MyAPIService could not process the request as an exception was thrown. " +
                            "The Redis return value is: " + retrievedValue);
            responseObserver.onError(status.asRuntimeException());
        }
    }


    @Override
    public void getSession(Hello.GetSessionRequest req, StreamObserver<Hello.GetSessionResponse> responseObserver) {
        Hello.GetSessionResponse reply = null;
        RedisClientService redisService = RedisClientService.getInstance();
        Hello.GetSessionResponse.Builder sessionBuilder = Hello.GetSessionResponse.newBuilder();

        StatefulRedisConnection<String, byte[]> redisConnection = redisService.redisClient.connect(RedisCodec.of(new StringCodec(), new ByteArrayCodec()));

        redisConnection = DynamicProxyInterceptor.createInterceptor(redisConnection, redisService.connectionString);

        byte[] retrievedValue = redisConnection.sync().get(req.getSessionId());

        if (retrievedValue != null) {  // Check whether there is a Redis hit
            try {  // Try deserializing the retrieved value as JSONObject
                JSONObject sessionJO = new JSONObject(new String(retrievedValue, Charset.defaultCharset()));
                reply = sessionBuilder.setSession(sessionJO.toString()).build();
            } catch (JSONException e) {
                String description = "Error deserializing retrievedValue: "
                        + Arrays.toString(retrievedValue) +
                        " which reads: " + new String(retrievedValue, Charset.defaultCharset())
                        + " - " + e.getMessage();
                respondWithError(responseObserver, description);
            }
        } else {
            // If there is no hit in Redis, query the second level cache
            // However, in this scenario, the second level cache is down, so we return an error
            Status status = Status.INTERNAL.withDescription(new Exception("Redis second level cache is down.").toString());
            responseObserver.onError(status.asRuntimeException());
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void getLocationFromSession(Hello.GetSessionRequest req, StreamObserver<Hello.GetLocationFromSessionResponse> responseObserver) {
        Hello.GetLocationFromSessionResponse reply = null;
        RedisClientService redisService = RedisClientService.getInstance();
        Hello.GetLocationFromSessionResponse.Builder sessionLocationBuilder = Hello.GetLocationFromSessionResponse.newBuilder();

        StatefulRedisConnection<String, byte[]> redisConnection = redisService.redisClient.connect(RedisCodec.of(new StringCodec(), new ByteArrayCodec()));

        redisConnection = DynamicProxyInterceptor.createInterceptor(redisConnection, redisService.connectionString);

        byte[] retrievedValue = redisConnection.sync().get(req.getSessionId());

        if (retrievedValue != null) {  // Check whether there is a Redis hit
            JSONObject sessionJO = null;

            try {  // Try deserializing the retrieved value as JSONObject
                sessionJO = new JSONObject(new String(retrievedValue, Charset.defaultCharset()));
            } catch (JSONException e) {
                String description = "Error deserializing retrievedValue: "
                        + Arrays.toString(retrievedValue) +
                        " which reads: "
                        + new String(retrievedValue, Charset.defaultCharset())
                        + " - " + e.getMessage();
                respondWithError(responseObserver, description);
            }

            if (sessionJO != null && sessionJO.has("location")) {  // Check whether the sessionJO has a location field
                reply = sessionLocationBuilder.setLocation(sessionJO.getString("location")).build();
            } else {
                // Else make a call to the Hello service to retrieve the last saved location for the sessionId.
                ManagedChannel helloChannel = ManagedChannelBuilder
                        .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                        .usePlaintext()
                        .build();
                ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("api_server");
                Channel channel = ClientInterceptors.intercept(helloChannel, clientInterceptor);

                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName(req.getSessionId()).build();
                Hello.HelloReply helloReply = blockingStub.getLastSessionLocation(request);

                reply = sessionLocationBuilder.setLocation(helloReply.getMessage()).build();
                helloChannel.shutdownNow();
            }

        } else {
            String description = "Retrieved value is null. Session was not found";
            respondWithError(responseObserver, description);
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void createSession(Hello.CreateSessionRequest req, StreamObserver<Hello.CreateSessionResponse> responseObserver) {

        // Create the session JSON object
        JSONObject referenceSession = new JSONObject();
        referenceSession.put("uid", req.getUserId());
        referenceSession.put("location", req.getLocation());
        referenceSession.put("iat", "123");  // Request timestamp
        byte[] sessionBytes = referenceSession.toString().getBytes(Charset.defaultCharset());
        Random rand = new Random(getRandomSeedProperty());
        String sessionId = String.valueOf(100000 + rand.nextInt(900000));  // Generate a random 6 digit number

        // Retrieve the Redis instance
        RedisClientService redisService = RedisClientService.getInstance();
        StatefulRedisConnection<String, byte[]> redisConnection = redisService.redisClient.connect(RedisCodec.of(new StringCodec(), new ByteArrayCodec()));
        redisConnection = DynamicProxyInterceptor.createInterceptor(redisConnection, redisService.connectionString);

        // Put the session in Redis
        try {
            redisConnection.async().set(sessionId, sessionBytes).get();
        } catch (InterruptedException | ExecutionException e) {
            String description = "Could not add session to Redis: " + e.getMessage();
            respondWithError(responseObserver, description);
        }

        // Create the response
        Hello.CreateSessionResponse.Builder sessionBuilder = Hello.CreateSessionResponse.newBuilder();
        sessionBuilder.setSessionId(sessionId).setSessionSize(sessionBytes.length * 8);

        // Send the response
        responseObserver.onNext(sessionBuilder.build());
        responseObserver.onCompleted();

    }

    private static void respondWithError(StreamObserver<?> responseObserver, String message) {
        Status status = Status.INTERNAL.withDescription(message);
        responseObserver.onError(status.asException());
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

    @Override
    public void simulatePurchase(Hello.PurchaseRequest req, StreamObserver<Hello.PurchaseResponse> responseObserver) {
        // Open channel to the user service.
        ManagedChannel originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("mock"), Networking.getPort("mock"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("api_server");
        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        // Make call to get the user.
        getUserFromSession(channel, req.getSessionId());

        // Make call to get the user, again.
        getUserFromSession(channel, req.getSessionId());

        // Get cart
        Hello.GetCartResponse getCartResponse = getCartFromSession(channel, req.getSessionId());
        String cartId = getCartResponse.getCartId();

        // Make call to get the user, again.
        getUserFromSession(channel, req.getSessionId());

        // Set discount.
        try {
            getDiscountOnCart(channel, cartId);
        } catch (StatusRuntimeException e) {
            // Nothing, ignore discount failure.
        }

        // Make call to get the user, again.
        getUserFromSession(channel, req.getSessionId());

        // Assemble response.
        Hello.PurchaseResponse purchaseResponse = Hello.PurchaseResponse.newBuilder().setSuccess(true).build();
        responseObserver.onNext(purchaseResponse);
        responseObserver.onCompleted();

        // Teardown the channel.
        originalChannel.shutdownNow();
        try {
            while (!originalChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
                Thread.sleep(4000);
            }
        } catch (InterruptedException ie) {
            logger.log(Level.SEVERE, "Failed to terminate channel: " + ie);
        }
    }

    @Override
    public void purchase(Hello.PurchaseRequest req, StreamObserver<Hello.PurchaseResponse> responseObserver) {
        PurchaseWorkflow purchaseWorkflow = new PurchaseWorkflow(req.getSessionId(), req.getAbortOnNoDiscount(), req.getAbortOnLessThanDiscountAmount());
        PurchaseWorkflow.PurchaseWorkflowResponse workflowResponse = purchaseWorkflow.execute();
        Status status;

        switch (workflowResponse) {
            case SUCCESS:
                Hello.PurchaseResponse purchaseResponse = Hello.PurchaseResponse.newBuilder()
                        .setSuccess(true)
                        .setTotal(String.valueOf(purchaseWorkflow.getPurchaseTotal()))
                        .build();
                responseObserver.onNext(purchaseResponse);
                responseObserver.onCompleted();
                break;
            case NO_DISCOUNT:
                status = Status.FAILED_PRECONDITION.withDescription("Consumer did not get a discount.");
                responseObserver.onError(status.asRuntimeException());
                break;
            case INSUFFICIENT_DISCOUNT:
                status = Status.FAILED_PRECONDITION.withDescription("Consumer did not get enough of a discount.");
                responseObserver.onError(status.asRuntimeException());
                break;
            case INSUFFICIENT_FUNDS:
                status = Status.FAILED_PRECONDITION.withDescription("Consumer did not have sufficient funds to make purchase.");
                responseObserver.onError(status.asRuntimeException());
                break;
            case UNPROCESSED:
                status = Status.INTERNAL.withDescription("Purchase has not yet been completed.");
                responseObserver.onError(status.asRuntimeException());
                break;
            case USER_UNAVAILABLE:
                status = Status.UNAVAILABLE.withDescription("Purchase could not be completed at this time, please retry the request: user could not be retrieved.");
                responseObserver.onError(status.asRuntimeException());
                break;
            case CART_UNAVAILABLE:
                status = Status.UNAVAILABLE.withDescription("Purchase could not be completed at this time, please retry the request: cart could not be retrieved.");
                responseObserver.onError(status.asRuntimeException());
                break;
        }
    }

    @Override
    public void world(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        String helloBaseUri = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient helloWebClient = TestHelper.getTestWebClient(helloBaseUri, "api-service");

        // Issue GET To http://hello/world, which will issue a transitive GET to http://world.
        CompletableFuture<String> firstRequestFuture = CompletableFuture.supplyAsync(() -> {
            RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/world", HttpHeaderNames.ACCEPT, "application/json");
            AggregatedHttpResponse response = helloWebClient.execute(getHeaders).aggregate().join();
            ResponseHeaders headers = response.headers();
            return headers.get(HttpHeaderNames.STATUS);
        }, FilibusterExecutor.getExecutorService());

        try {
            firstRequestFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            // Ignore for now, we only care about executing the request.
        }

        // Issue POST to http://hello/external-post, which will issue a transitive POST to http://external.
        CompletableFuture<String> secondRequestFuture = CompletableFuture.supplyAsync(() -> {
            RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/external-post", HttpHeaderNames.ACCEPT, "application/json");
            AggregatedHttpResponse response = helloWebClient.execute(getHeaders).aggregate().join();
            ResponseHeaders headers = response.headers();
            return headers.get(HttpHeaderNames.STATUS);
        }, FilibusterExecutor.getExecutorService());

        try {
            secondRequestFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            // Ignore for now, we only care about executing the request.
        }

        // Add a GRPC in here just to mix things up.
        ManagedChannel worldManagedChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("world"), Networking.getPort("world"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("hello");
        Channel worldChannel = ClientInterceptors.intercept(worldManagedChannel, clientInterceptor);

        try {
            WorldServiceGrpc.WorldServiceBlockingStub worldServiceBlockingStub = WorldServiceGrpc.newBlockingStub(worldChannel);
            Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
            worldServiceBlockingStub.world(request);
        } catch (RuntimeException e) {
            // Ignore for now, we only care about executing the request.
        }

        worldManagedChannel.shutdownNow();
        try {
            while (!worldManagedChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
                Thread.sleep(4000);
            }
        } catch (InterruptedException ie) {
            logger.log(Level.SEVERE, "Failed to terminate channel: " + ie);
        }

        // Return stock response.
        Hello.HelloReply helloReply = Hello.HelloReply.newBuilder().setMessage("Hello!").build();
        responseObserver.onNext(helloReply);
        responseObserver.onCompleted();
    }
}
