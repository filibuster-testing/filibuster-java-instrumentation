package cloud.filibuster.integration.examples.armeria.grpc.test_services;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.WorldServiceGrpc;
import cloud.filibuster.exceptions.CircuitBreakerException;
import cloud.filibuster.integration.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.integration.instrumentation.libraries.opentelemetry.OpenTelemetryFilibusterClientInterceptor;
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.lettuce.core.api.StatefulRedisConnection;

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
            ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

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

                Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + worldReply.getMessage()).build();
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
                } else {
                    Status status = Status.DATA_LOSS.withDescription(e.toString());
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

                return;
            }
        } else {    // build stub with decorator
            String serviceName = "test";
            String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
            GrpcClientBuilder grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, serviceName);
            try {
                WorldServiceGrpc.WorldServiceBlockingStub blockingStub = grpcClientBuilder.build(WorldServiceGrpc.WorldServiceBlockingStub.class);
                Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
                Hello.WorldReply worldReply = blockingStub.world(request);

                Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + worldReply.getMessage()).build();
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

        Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + req.getName() + "!!").build();
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
        ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

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

        Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + result).build();
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
        ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

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

            Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + worldReply.getMessage()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            Status status = Status.DATA_LOSS.withDescription(e.toString());
            responseObserver.onError(status.asRuntimeException());
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
        ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

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

            Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + worldReply.getMessage()).build();
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
    public void partialHelloWithErrorHandling(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        String serviceName = "test";
        String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
        GrpcClientBuilder grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, serviceName);

        try {
            WorldServiceGrpc.WorldServiceBlockingStub blockingStub = grpcClientBuilder.build(WorldServiceGrpc.WorldServiceBlockingStub.class);
            Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
            Hello.WorldReply worldReply = blockingStub.world(request);

            Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + worldReply.getMessage()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException re) {
            // Coverage automatically generated by Filibuster for this error handling case.
            Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, Armerian World!!").build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void unimplemented(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        // build stub with decorator or interceptor
        if (!shouldUseDecorator) {
            ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

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
                Hello.WorldReply worldReply = blockingStub.worldUnimplemented(request);

                Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + worldReply.getMessage()).build();
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
                Status status = Status.DATA_LOSS.withDescription(e.toString());
                responseObserver.onError(status.asRuntimeException());

                originalChannel.shutdownNow();
                try {
                    while (!originalChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
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
                WorldServiceGrpc.WorldServiceBlockingStub blockingStub = grpcClientBuilder.build(WorldServiceGrpc.WorldServiceBlockingStub.class);
                Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
                Hello.WorldReply worldReply = blockingStub.worldUnimplemented(request);

                Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + worldReply.getMessage()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (StatusRuntimeException e) {
                Status status = Status.DATA_LOSS.withDescription(e.toString());
                responseObserver.onError(status.asRuntimeException());
            }
        }
    }

    @Override
    public void parallelSynchronousPartialHello(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

        ClientInterceptor clientInterceptor;

        if (useOtelClientInterceptor) {
            clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("hello", null, null);
        } else {
            clientInterceptor = new FilibusterClientInterceptor("hello");
        }

        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        // Perform two async requests but synchronously, in order.
        ArrayList<String> resultList = new ArrayList<>();

        CompletableFuture<String> firstRequest = performAsyncWorldRequest(channel, req.getName());

        try {
            String firstResult = firstRequest.get();
            resultList.add(firstResult);
        } catch (InterruptedException | ExecutionException e) {
            // Nothing.
        }

        CompletableFuture<String> secondRequest = performAsyncWorldRequest(channel, "Parallel");

        try {
            String secondResult = secondRequest.get();
            resultList.add(secondResult);
        } catch (InterruptedException | ExecutionException e) {
            // Nothing.
        }

        // Assemble the final response.
        String result = String.join(" Hello, ", resultList);

        // Respond to the client.

        Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + result).build();
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
    public void smellyRedundantRPC(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

        // Setup interceptor.

        ClientInterceptor clientInterceptor;

        if (useOtelClientInterceptor) {
            clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("hello", null, null);
        } else {
            clientInterceptor = new FilibusterClientInterceptor("hello");
        }

        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        // Smell 1, RedundantRPC.

        for (int i = 0; i < 4; i++) {
            try {
                performAsyncWorldRequest(channel, req.getName()).get();
            } catch (InterruptedException | ExecutionException e) {
                // Do nothing.
            }
        }

        // Response.

        Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, Smelly!").build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();

        originalChannel.shutdownNow();

        try {
            originalChannel.awaitTermination(1000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to terminate channel: " + e);
        }
    }

    private static CompletableFuture<String> performAsyncWorldUnimplementedRequest(Channel channel, String name) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            WorldServiceGrpc.WorldServiceBlockingStub blockingStub = WorldServiceGrpc.newBlockingStub(channel);
            Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(name).build();
            Hello.WorldReply worldReply = blockingStub.worldUnimplemented(request);
            return worldReply.getMessage();
        });

        return future;
    }

    @Override
    public void smellyUnimplementedFailures(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

        // Setup interceptor.

        ClientInterceptor clientInterceptor;

        if (useOtelClientInterceptor) {
            clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("hello", null, null);
        } else {
            clientInterceptor = new FilibusterClientInterceptor("hello");
        }

        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        // Smell 2, UnimplementedFailures.

        try {
            performAsyncWorldUnimplementedRequest(channel, req.getName()).get();
        } catch (InterruptedException | ExecutionException e) {
            // Do nothing.
        }

        // Response.

        Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, Smelly!").build();
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
    public void smellyResponseBecomesRequest(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

        // Setup interceptor.

        ClientInterceptor clientInterceptor;

        if (useOtelClientInterceptor) {
            clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("hello", null, null);
        } else {
            clientInterceptor = new FilibusterClientInterceptor("hello");
        }

        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        // Smell 3, ResponseBecomesRequest.
        try {
            String response = performAsyncWorldRequest(channel, req.getName()).get();
            performAsyncWorldRequest(channel, "Some random stuff to prevent field + prefix response match, " + response).get();
        } catch (InterruptedException | ExecutionException e) {
            // Do nothing.
        }

        // Response.

        Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, Smelly!").build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();

        originalChannel.shutdownNow();

        try {
            originalChannel.awaitTermination(1000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to terminate channel: " + e);
        }
    }

    private static CompletableFuture<String> performAsyncWorldRandomRequest(Channel channel, String name) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            WorldServiceGrpc.WorldServiceBlockingStub blockingStub = WorldServiceGrpc.newBlockingStub(channel);
            Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(name).build();
            Hello.WorldReply worldReply = blockingStub.worldRandom(request);
            return worldReply.getMessage();
        });

        return future;
    }

    @Override
    public void smellyMultipleInvocationsForIndividualMutations(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

        // Setup interceptor.

        ClientInterceptor clientInterceptor;

        if (useOtelClientInterceptor) {
            clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("hello", null, null);
        } else {
            clientInterceptor = new FilibusterClientInterceptor("hello");
        }

        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        // Smell 4, MultipleInvocationsForIndividualMutations.
        try {
            performAsyncWorldRandomRequest(channel, "Some really big shared component of the request, 1").get();
            performAsyncWorldRandomRequest(channel, "Some really big shared component of the request, 2").get();
        } catch (InterruptedException | ExecutionException e) {
            // Do nothing.
        }

        // Response.

        Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, Smelly!").build();
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
    public void compositionalHello(Hello.HelloExtendedRequest req, StreamObserver<Hello.HelloExtendedReply> responseObserver) {
        // build stub with decorator or interceptor
        if (!shouldUseDecorator) {
            ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();

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

                Hello.HelloExtendedReply reply = Hello.HelloExtendedReply.newBuilder().setName(req.getName()).setFirstMessage("Hello, " + worldReply.getMessage()).setSecondMessage(String.valueOf(Math.random())).setCreatedAt("2023-01-01 01:02:03").build();
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
                } else {
                    Status status = Status.DATA_LOSS.withDescription(e.toString());
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

                return;
            }
        } else {    // build stub with decorator
            String serviceName = "test";
            String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
            GrpcClientBuilder grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, serviceName);
            try {
                WorldServiceGrpc.WorldServiceBlockingStub blockingStub = grpcClientBuilder.build(WorldServiceGrpc.WorldServiceBlockingStub.class);
                Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
                Hello.WorldReply worldReply = blockingStub.world(request);

                Hello.HelloExtendedReply reply = Hello.HelloExtendedReply.newBuilder().setName(req.getName()).setFirstMessage("Hello, " + worldReply.getMessage()).setSecondMessage(String.valueOf(Math.random())).setCreatedAt("2023-01-01 01:02:03").build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (StatusRuntimeException e) {
                Status status = Status.DATA_LOSS.withDescription(e.toString());
                responseObserver.onError(status.asRuntimeException());
            }
        }
    }

    @Override
    public void simplePartialHello(Hello.HelloRequest req, StreamObserver<Hello.HelloReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder.forAddress(Networking.getHost("world"), Networking.getPort("world")).usePlaintext().build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("hello");
        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        try {
            WorldServiceGrpc.WorldServiceBlockingStub blockingStub = WorldServiceGrpc.newBlockingStub(channel);
            Hello.WorldRequest request = Hello.WorldRequest.newBuilder().setName(req.getName()).build();
            Hello.WorldReply worldReply = blockingStub.world(request);

            Hello.HelloReply reply = Hello.HelloReply.newBuilder().setMessage("Hello, " + worldReply.getMessage()).build();
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
                Status status = Status.FAILED_PRECONDITION.withDescription(e.getCause().toString());
                responseObserver.onError(status.asRuntimeException());
            } else {
                Status status = Status.ABORTED.withDescription(e.toString());
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
    public void redisWrite(Hello.RedisWriteRequest req, StreamObserver<Hello.RedisReply> responseObserver) {
        Hello.RedisReply reply = null;
        try {
            StatefulRedisConnection<String, String> connection = RedisConnection.getInstance().connection;
            connection.sync().set(req.getKey(), req.getValue());
            reply = Hello.RedisReply.newBuilder().setValue("1").build();
        } catch (Exception e) {
            reply = Hello.RedisReply.newBuilder().setValue(e.toString()).build();
        }
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void redisRead(Hello.RedisReadRequest req, StreamObserver<Hello.RedisReply> responseObserver) {
        Hello.RedisReply reply = null;
        try {
            StatefulRedisConnection<String, String> connection = RedisConnection.getInstance().connection;
            String value = connection.sync().get(req.getKey());
            reply = Hello.RedisReply.newBuilder().setValue(value).build();
        } catch (Exception e) {
            reply = Hello.RedisReply.newBuilder().setValue(e.toString()).build();
        }
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
