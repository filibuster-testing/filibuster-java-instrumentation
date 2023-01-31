package cloud.filibuster.integration.examples.armeria.grpc.test_services;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.WorldServiceGrpc;

import cloud.filibuster.integration.instrumentation.TestHelper;

import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.integration.instrumentation.libraries.opentelemetry.OpenTelemetryFilibusterClientInterceptor;
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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyWorldService extends WorldServiceGrpc.WorldServiceImplBase {
    private static final Logger logger = Logger.getLogger(MyWorldService.class.getName());

    public static boolean useOtelClientInterceptor = false;

    private final String serviceName;

    public MyWorldService(String serviceName) {
        super();
        this.serviceName = serviceName;
    }

    @Override
    public void world(Hello.WorldRequest req, StreamObserver<Hello.WorldReply> responseObserver) {
        Hello.WorldReply reply = Hello.WorldReply.newBuilder()
                .setMessage(req.getName() + " World!!")
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void worldExternalHttp(Hello.WorldRequest req, StreamObserver<Hello.WorldReply> responseObserver) {
        String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
        WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
        RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse aggregatedHttpResponse = webClient1.execute(getHeaders1).aggregate().join();
        ResponseHeaders headers = aggregatedHttpResponse.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);

        if (Objects.equals(statusCode, "200")) {
            Hello.WorldReply reply = Hello.WorldReply.newBuilder()
                    .setMessage(req.getName() + " World!!")
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } else {
            Status status = Status.FAILED_PRECONDITION.withDescription("HTTP RPC returned: " + statusCode);
            responseObserver.onError(status.asException());
        }
    }

    @Override
    public void worldExternalGrpc(Hello.WorldRequest req, StreamObserver<Hello.WorldReply> responseObserver) {
        ManagedChannel originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        ClientInterceptor clientInterceptor;

        if (useOtelClientInterceptor) {
            clientInterceptor = new OpenTelemetryFilibusterClientInterceptor("world", null, null);
        } else {
            clientInterceptor = new FilibusterClientInterceptor("world");
        }

        Channel channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName(req.getName()).build();
            Hello.HelloReply helloReply = blockingStub.hello(request);

            Hello.WorldReply reply = Hello.WorldReply.newBuilder()
                    .setMessage("Hello, " + helloReply.getMessage())
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
}
