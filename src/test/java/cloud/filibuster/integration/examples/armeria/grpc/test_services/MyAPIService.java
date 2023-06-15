package cloud.filibuster.integration.examples.armeria.grpc.test_services;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.exceptions.CircuitBreakerException;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
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

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
    public void purchase(Hello.PurchaseRequest req, StreamObserver<Hello.PurchaseResponse> responseObserver) {
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
        String cartId = getCartFromSession(channel, req.getSessionId());

        // Make call to get the user, again.
        getUserFromSession(channel, req.getSessionId());

        // Set discount.
        try {
            setDiscountOnCart(channel, cartId);
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

    private static String getUserFromSession(Channel channel, String sessionId) {
        UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub = UserServiceGrpc.newBlockingStub(channel);
        Hello.GetUserRequest request = Hello.GetUserRequest.newBuilder().setSessionId(sessionId).build();
        Hello.GetUserResponse response = userServiceBlockingStub.getUserFromSession(request);
        return response.getUserId();
    }

    private static String getCartFromSession(Channel channel, String sessionId) {
        CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub = CartServiceGrpc.newBlockingStub(channel);
        Hello.GetCartRequest request = Hello.GetCartRequest.newBuilder().setSessionId(sessionId).build();
        Hello.GetCartResponse response = cartServiceBlockingStub.getCartForSession(request);
        return response.getCartId();
    }

    private static void setDiscountOnCart(Channel channel, String cartId) {
        CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub = CartServiceGrpc.newBlockingStub(channel);
        Hello.SetDiscountRequest request = Hello.SetDiscountRequest.newBuilder().setCartId(cartId).build();
        Hello.SetDiscountResponse response = cartServiceBlockingStub.setDiscountOnCart(request);
        return;
    }

}
