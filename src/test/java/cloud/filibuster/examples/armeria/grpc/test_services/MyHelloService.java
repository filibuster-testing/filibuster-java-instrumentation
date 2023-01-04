package cloud.filibuster.examples.armeria.grpc.test_services;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.WorldServiceGrpc;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.opentelemetry.OpenTelemetryFilibusterClientInterceptor;
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyHelloService extends HelloServiceGrpc.HelloServiceImplBase {
    private static final Logger logger = Logger.getLogger(MyHelloService.class.getName());

    public static boolean shouldReturnRuntimeExceptionWithDescription = false;
    public static boolean shouldReturnRuntimeExceptionWithCause = false;
    public static boolean shouldReturnExceptionWithDescription = false;
    public static boolean shouldReturnExceptionWithCause = false;

    public static boolean useOtelClientInterceptor = false;
    public static boolean shouldUseDecorator = false;

    @Override
    public void partialHello(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {

        // build stub with decorator or interceptor
        if (!shouldUseDecorator) {
            ManagedChannel originalChannel = ManagedChannelBuilder
                    .forAddress(Networking.getHost("world"), Networking.getPort("world"))
                    .usePlaintext()
                    .build();

            ClientInterceptor clientInterceptor;

            if (useOtelClientInterceptor) {
                clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("hello", null, null);
            } else {
                clientInterceptor = new FilibusterClientInterceptor("hello");
            }

            Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

            try {
                WorldServiceGrpc.WorldServiceBlockingStub blockingStub = WorldServiceGrpc.newBlockingStub(channel);
                Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
                Hello.WorldReply worldReply = blockingStub.world(request);

                Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                        .setMessage("Hello, " + worldReply.getMessage())
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();

                originalChannel.shutdownNow();
                try {
                    while (! originalChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
                        Thread.sleep(4000);
                    }
                } catch (InterruptedException ie) {
                    logger.log(Level.SEVERE, "Failed to terminate channel: " + ie);
                }

            } catch (StatusRuntimeException e) {
                Status status = Status.DATA_LOSS.withDescription(e.toString());
                responseObserver.onError(status.asRuntimeException());

                originalChannel.shutdownNow();
                try {
                    while (! originalChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
                        Thread.sleep(4000);
                    }
                } catch (InterruptedException ie) {
                    logger.log(Level.SEVERE, "Failed to terminate channel: " + ie);
                }

                return;
            }
        } else {    // build stub with decorator
            String serviceName = "test";
            String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
            GrpcClientBuilder grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, serviceName);
            try {
                WorldServiceGrpc.WorldServiceBlockingStub blockingStub =
                        grpcClientBuilder.build(WorldServiceGrpc.WorldServiceBlockingStub.class);
                Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
                Hello.WorldReply worldReply = blockingStub.world(request);

                Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                        .setMessage("Hello, " + worldReply.getMessage())
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (StatusRuntimeException e) {
                Status status = Status.DATA_LOSS.withDescription(e.toString());
                responseObserver.onError(status.asRuntimeException());
            }

        }
    }

    @Override
    public void hello(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        if (shouldReturnRuntimeExceptionWithDescription) {
            Status status = Status.FAILED_PRECONDITION.withDescription("Something went wrong!");
            responseObserver.onError(status.asRuntimeException());
            return;
        }

        if (shouldReturnExceptionWithDescription) {
            Status status = Status.FAILED_PRECONDITION.withDescription("Something went wrong!");
            responseObserver.onError(status.asException());
            return;
        }

        if (shouldReturnRuntimeExceptionWithCause) {
            Status status = Status.FAILED_PRECONDITION.withCause(new UnsupportedOperationException());
            responseObserver.onError(status.asRuntimeException());
            return;
        }

        if (shouldReturnExceptionWithCause) {
            Status status = Status.FAILED_PRECONDITION.withCause(new UnsupportedOperationException());
            responseObserver.onError(status.asException());
            return;
        }

        Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                .setMessage("Hello, " + req.getName() + "!!")
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @SuppressWarnings("MethodCanBeStatic")
    private CompletableFuture<String> performAsyncWorldRequest(Channel channel, String name) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            WorldServiceGrpc.WorldServiceBlockingStub blockingStub = WorldServiceGrpc.newBlockingStub(channel);
            Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(name).build();
            Hello.WorldReply worldReply = blockingStub.world(request);
            return worldReply.getMessage();
        });

        return future;
    }

    @Override
    public void parallelPartialHello(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("world"), Networking.getPort("world"))
                .usePlaintext()
                .build();

        ClientInterceptor clientInterceptor;

        if (useOtelClientInterceptor) {
            clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("hello", null, null);
        } else {
            clientInterceptor = new FilibusterClientInterceptor("hello");
        }

        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        // Perform two async requests.

        ArrayList<CompletableFuture<String>> futureList = new ArrayList<>();
        futureList.add(performAsyncWorldRequest(channel, req.getName()));
        futureList.add(performAsyncWorldRequest(channel, "Parallel"));

        // Wait for them to complete.

        ArrayList<String> resultList = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            try {
                String result = futureList.get(i).get();
                resultList.add(result);
            } catch (InterruptedException e) {
                // Do nothing.
            } catch (ExecutionException e) {
                // Do nothing.
            }
        }

        // Assemble the final response.
        String result = String.join(" Hello, ", resultList);

        // Respond to the client.

        Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                .setMessage("Hello, " + result)
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();

        originalChannel.shutdownNow();
        try {
            originalChannel.awaitTermination(1000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to terminate channel: " + e);
        }
    }

    @Override
    public void partialHelloExternalHttp(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("world"), Networking.getPort("world"))
                .usePlaintext()
                .build();

        ClientInterceptor clientInterceptor;

        if (useOtelClientInterceptor) {
            clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("hello", null, null);
        } else {
            clientInterceptor = new FilibusterClientInterceptor("hello");
        }

        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        try {
            WorldServiceGrpc.WorldServiceBlockingStub blockingStub = WorldServiceGrpc.newBlockingStub(channel);
            Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
            Hello.WorldReply worldReply = blockingStub.worldExternalHttp(request);

            Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                    .setMessage("Hello, " + worldReply.getMessage())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            Status status = Status.DATA_LOSS.withDescription(e.toString());
            responseObserver.onError(status.asRuntimeException());
            return;
        }

        originalChannel.shutdownNow();
        try {
            originalChannel.awaitTermination(1000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to terminate channel: " + e);
        }
    }

    @Override
    public void partialHelloExternalGrpc(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("world"), Networking.getPort("world"))
                .usePlaintext()
                .build();

        ClientInterceptor clientInterceptor;

        if (useOtelClientInterceptor) {
            clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("hello", null, null);
        } else {
            clientInterceptor = new FilibusterClientInterceptor("hello");
        }

        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        try {
            WorldServiceGrpc.WorldServiceBlockingStub blockingStub = WorldServiceGrpc.newBlockingStub(channel);
            Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
            Hello.WorldReply worldReply = blockingStub.worldExternalGrpc(request);

            Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                    .setMessage("Hello, " + worldReply.getMessage())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            Status status = Status.DATA_LOSS.withDescription(e.toString());
            responseObserver.onError(status.asRuntimeException());
            return;
        }

        originalChannel.shutdownNow();
        try {
            originalChannel.awaitTermination(1000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to terminate channel: " + e);
        }
    }

    @Override
    public void partialHelloWithErrorHandling(Hello.HelloRequest req,
                                              StreamObserver<Hello.HelloReply> responseObserver) {
        String serviceName = "test";
        String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
        GrpcClientBuilder grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, serviceName);

        try {
            WorldServiceGrpc.WorldServiceBlockingStub blockingStub =
                    grpcClientBuilder.build(WorldServiceGrpc.WorldServiceBlockingStub.class);
            Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
            Hello.WorldReply worldReply = blockingStub.world(request);

            Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                    .setMessage("Hello, " + worldReply.getMessage())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException re) {
            // Coverage automatically generated by Filibuster for this error handling case.
            Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                    .setMessage("Hello, Armerian World!!")
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void unavailable(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        // build stub with decorator or interceptor
        if (!shouldUseDecorator) {
            ManagedChannel originalChannel = ManagedChannelBuilder
                    .forAddress(Networking.getHost("world"), Networking.getPort("world"))
                    .usePlaintext()
                    .build();

            ClientInterceptor clientInterceptor;

            if (useOtelClientInterceptor) {
                clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("hello", null, null);
            } else {
                clientInterceptor = new FilibusterClientInterceptor("hello");
            }

            Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

            try {
                WorldServiceGrpc.WorldServiceBlockingStub blockingStub = WorldServiceGrpc.newBlockingStub(channel);
                Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
                Hello.WorldReply worldReply = blockingStub.worldUnavailable(request);

                Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                        .setMessage("Hello, " + worldReply.getMessage())
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();

                originalChannel.shutdownNow();
                try {
                    while (! originalChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
                        Thread.sleep(4000);
                    }
                } catch (InterruptedException ie) {
                    logger.log(Level.SEVERE, "Failed to terminate channel: " + ie);
                }

            } catch (StatusRuntimeException e) {
                Status status = Status.DATA_LOSS.withDescription(e.toString());
                responseObserver.onError(status.asRuntimeException());

                originalChannel.shutdownNow();
                try {
                    while (! originalChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
                        Thread.sleep(4000);
                    }
                } catch (InterruptedException ie) {
                    logger.log(Level.SEVERE, "Failed to terminate channel: " + ie);
                }

                return;
            }
        } else {    // build stub with decorator
            String serviceName = "test";
            String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
            GrpcClientBuilder grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, serviceName);
            try {
                WorldServiceGrpc.WorldServiceBlockingStub blockingStub =
                        grpcClientBuilder.build(WorldServiceGrpc.WorldServiceBlockingStub.class);
                Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
                Hello.WorldReply worldReply = blockingStub.worldUnavailable(request);

                Hello.HelloReply reply = Hello.HelloReply.newBuilder()
                        .setMessage("Hello, " + worldReply.getMessage())
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (StatusRuntimeException e) {
                Status status = Status.DATA_LOSS.withDescription(e.toString());
                responseObserver.onError(status.asRuntimeException());
            }
        }
    }
}
