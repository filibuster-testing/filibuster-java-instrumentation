package cloud.filibuster.integration.examples.armeria.grpc.test_services;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.exceptions.CircuitBreakerException;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
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
    public void redisHello(Hello.RedisRequest req, StreamObserver<Hello.RedisReply> responseObserver) {
        Hello.RedisReply reply;
        try {
            StatefulRedisConnection<String, String> connection = RedisClientService.getInstance().redisClient.connect();
            String retrievedValue = connection.sync().get(req.getKey());

            if (retrievedValue != null) {  // Return cache value if there is a hit
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
        } catch (RuntimeException e) {
            reply = Hello.RedisReply.newBuilder().setValue(e.toString()).build();
        }
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void purchase(Hello.PurchaseRequest req, StreamObserver<Hello.PurchaseResponse> responseObserver) {
        // Open channel to the user service.
        ManagedChannel originalUserServiceChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("user-mock"), Networking.getPort("user-mock"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("api_server");
        Channel userServiceChannel = ClientInterceptors.intercept(originalUserServiceChannel, clientInterceptor);

        // Make call to get the user.
        getUserFromSession(userServiceChannel, req.getSessionId());

        // Make call to get the user, again.
        getUserFromSession(userServiceChannel, req.getSessionId());

        // Make call to get the user, again.
        getUserFromSession(userServiceChannel, req.getSessionId());

        // Assemble response.
        Hello.PurchaseResponse purchaseResponse = Hello.PurchaseResponse.newBuilder().setSuccess(true).build();
        responseObserver.onNext(purchaseResponse);
        responseObserver.onCompleted();

        originalUserServiceChannel.shutdownNow();
        try {
            while (!originalUserServiceChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
                Thread.sleep(4000);
            }
        } catch (InterruptedException ie) {
            logger.log(Level.SEVERE, "Failed to terminate channel: " + ie);
        }
    }

    private static void getUserFromSession(Channel userServiceChannel, String sessionId) {
        UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub = UserServiceGrpc.newBlockingStub(userServiceChannel);
        Hello.GetUserRequest firstGetUserRequest = Hello.GetUserRequest.newBuilder().setSessionId(sessionId).build();
        userServiceBlockingStub.getUserFromSession(firstGetUserRequest);
    }
}
