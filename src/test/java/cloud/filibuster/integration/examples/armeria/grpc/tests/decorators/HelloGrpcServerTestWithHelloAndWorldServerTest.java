package cloud.filibuster.integration.examples.armeria.grpc.tests.decorators;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.instrumentation.FilibusterServerFake;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloGrpcServerTestWithHelloAndWorldServerTest extends HelloGrpcServerTest {
    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        startHello();
        startWorld();
        startExternalServer();
    }

    @AfterEach
    public void stopServices() throws InterruptedException {
        stopWorld();
        stopHello();
        stopExternalServer();
    }

    @BeforeEach
    public void resetMyHelloServiceState() {
        MyHelloService.shouldReturnRuntimeExceptionWithCause = false;
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithCause = false;
    }

    @BeforeEach
    public void disableFilibuster() {
        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();
    }

    @AfterEach
    public void enableFilibuster() {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
    }

    private GrpcClientBuilder grpcClientBuilder;
    private static final String serviceName = "test";

    @BeforeEach
    public void setupGrpcClientBuilder() {
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, serviceName);
    }


    @Test
    @DisplayName("Test partial hello server grpc route. (MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldService() {
        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = grpcClientBuilder
                .build(HelloServiceGrpc.HelloServiceBlockingStub.class);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.partialHello(request);
        assertEquals("Hello, Armerian World!!", reply.getMessage());
    }

    @Test
    @DisplayName("Test parallel partial hello server grpc route. (MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceParallelPartialHello() {
        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = grpcClientBuilder
                .build(HelloServiceGrpc.HelloServiceBlockingStub.class);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.parallelPartialHello(request);
        assertEquals("Hello, Armerian World!! Hello, Parallel World!!", reply.getMessage());
    }

    @Test
    @DisplayName("Test partial hello external http server grpc route. (MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServicePartialHelloExternalHttp() {
        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = grpcClientBuilder
                .build(HelloServiceGrpc.HelloServiceBlockingStub.class);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.partialHelloExternalHttp(request);
        assertEquals("Hello, Armerian World!!", reply.getMessage());
    }

    @Test
    @DisplayName("Test partial hello external grpc server grpc route. (MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServicePartialHelloExternalGrpc() {
        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = grpcClientBuilder
                .build(HelloServiceGrpc.HelloServiceBlockingStub.class);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.partialHelloExternalGrpc(request);
        assertEquals("Hello, Hello, Hello, Armerian!!", reply.getMessage());
    }
}