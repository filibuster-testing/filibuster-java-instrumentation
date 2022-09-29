package cloud.filibuster.examples.armeria.grpc.tests.interceptors;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.helpers.Response;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;

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
import io.grpc.StatusRuntimeException;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class HelloGrpcServerTestWithHelloServerTest extends HelloGrpcServerTest {
    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        startHello();
    }

    @AfterEach
    public void stopServices() throws InterruptedException {
        stopHello();
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
    }

    @AfterEach
    public void enableFilibuster() {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
    }

    private Channel channel;
    private ManagedChannel originalChannel;

    @BeforeEach
    public void setupChannel() {
        originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("test");
        channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);
    }

    @AfterEach
    public void teardownChannel() throws InterruptedException {
        originalChannel.shutdownNow();
        originalChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Test hello server health-check route.")
    public void testHealthCheck() throws IOException, InterruptedException {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 OK response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Get body and verify the proper response.
        JSONObject jsonObject = Response.aggregatedHttpResponseToJsonObject(response);
        assertEquals("OK", jsonObject.getString("status"));
    }

    @Test
    @DisplayName("Test hello server grpc route. (MyHelloService)")
    public void testMyHelloService() {
        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
        Hello.HelloReply reply = blockingStub.hello(request);
        assertEquals("Hello, Armerian World!!", reply.getMessage());
    }

    @Test
    @DisplayName("Test hello server grpc route with runtime exception from description. (MyHelloService)")
    public void testMyHelloServiceWithRuntimeExceptionFromDescription() {
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = true;

        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
        });

        RuntimeException re;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
            throw new AssertionError("We shouldn't ever get here!");
        } catch (RuntimeException e) {
            re = e;
        }

        assertNull(re.getCause());
        assertEquals("FAILED_PRECONDITION: Something went wrong!", re.getMessage());
        assertEquals("io.grpc.StatusRuntimeException", re.getClass().getName());

        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
    }

    @Test
    @DisplayName("Test hello server grpc route with runtime exception from cause. (MyHelloService)")
    public void testMyHelloServiceWithRuntimeExceptionFromCause() {
        MyHelloService.shouldReturnRuntimeExceptionWithCause = true;

        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
        });

        RuntimeException re;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
            throw new AssertionError("We shouldn't ever get here!");
        } catch (RuntimeException e) {
            re = e;
        }

        assertNull(re.getCause()); // even if generated from cause, cause isn't transmitted from server to client.
        assertEquals("FAILED_PRECONDITION", re.getMessage());
        assertEquals("io.grpc.StatusRuntimeException", re.getClass().getName());

        MyHelloService.shouldReturnRuntimeExceptionWithCause = false;
    }

    @Test
    @DisplayName("Test hello server grpc route with exception from description. (MyHelloService)")
    public void testMyHelloServiceWithExceptionFromDescription() {
        MyHelloService.shouldReturnExceptionWithDescription = true;

        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
        });

        RuntimeException re;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
            throw new AssertionError("We shouldn't ever get here!");
        } catch (RuntimeException e) {
            re = e;
        }

        assertNull(re.getCause());
        assertEquals("FAILED_PRECONDITION: Something went wrong!", re.getMessage());
        assertEquals("io.grpc.StatusRuntimeException", re.getClass().getName());

        MyHelloService.shouldReturnExceptionWithDescription = false;
    }

    @Test
    @DisplayName("Test hello server grpc route with exception from cause. (MyHelloService)")
    public void testMyHelloServiceWithExceptionFromCause() {
        MyHelloService.shouldReturnExceptionWithCause = true;

        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
        });

        RuntimeException re;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
            throw new AssertionError("We shouldn't ever get here!");
        } catch (RuntimeException e) {
            re = e;
        }

        assertNull(re.getCause()); // even if generated from cause, cause isn't transmitted from server to client.
        assertEquals("FAILED_PRECONDITION", re.getMessage());
        assertEquals("io.grpc.StatusRuntimeException", re.getClass().getName());

        MyHelloService.shouldReturnExceptionWithCause = false;
    }
}