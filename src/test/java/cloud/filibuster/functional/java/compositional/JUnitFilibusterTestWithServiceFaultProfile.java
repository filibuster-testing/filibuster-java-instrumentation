package cloud.filibuster.functional.java.compositional;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.JUnitBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTestWithServiceFaultProfile extends JUnitBaseTest {

    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("hello-mock"))
            .build();

    private final static Set<String> testExceptionsThrownForHelloService = new HashSet<>();

    private final static Set<String> testExceptionsThrownForAPIService = new HashSet<>();

    private final static Set<String> testExceptionsThrownForAPIServiceWithMock = new HashSet<>();

    @BeforeAll
    public static void startAPIServer() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopAPIServer() throws InterruptedException {
        stopAPIServerAndWaitUntilUnavailable();
    }

    // Test 1
    // - Call Hello (which calls World) with a random set of arguments.
    // Produces:
    // - Server failure profile for Hello with the possible failure responses.
    // - Server profile that contains various requests and responses for none of the inputs we will provide.
    @FilibusterTest(dataNondeterminism = true)
    @Order(1)
    public void testHelloService() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        boolean expected = false;

        try {
            String name = String.valueOf(Math.random());

            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloExtendedRequest request = Hello.HelloExtendedRequest.newBuilder().setName(name).build();
            Hello.HelloExtendedReply reply = blockingStub.compositionalHello(request);
            assertEquals(name, reply.getName());
            assertEquals("Hello, " + name + " World!!", reply.getFirstMessage());
            assertThat(reply.getSecondMessage(), notNullValue());
            assertThat(reply.getCreatedAt(), notNullValue());
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            boolean wasFaultInjected = wasFaultInjected();

            if (wasFaultInjected) {
                testExceptionsThrownForHelloService.add(t.getMessage());

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: INTERNAL")) {
                    expected = true;
                }

                boolean wasFaultInjectedOnWorldService = wasFaultInjectedOnService("WorldService");
                assertTrue(wasFaultInjectedOnWorldService);

                boolean wasFaultInjectedOnWorldMethod = wasFaultInjectedOnMethod("cloud.filibuster.examples.WorldService/World");
                assertTrue(wasFaultInjectedOnWorldMethod);

                if (! expected) {
                    throw t;
                }
            } else {
                throw t;
            }
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    // Test 2
    // - Verify Filibuster generated the right number of tests for Hello Service.
    @Test
    @Order(2)
    public void testNumAssertionsForHelloService() {
        assertEqualsUnlessFilibusterDisabledByEnvironment(4, testExceptionsThrownForHelloService.size());
    }

    // Test 3
    // - Test the API Service (which, talks to Hello and World.)
    @FilibusterTest(dataNondeterminism = true)
    @Order(3)
    public void testAPIService() throws InterruptedException {
        ManagedChannel apiChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
                .usePlaintext()
                .build();

        boolean expected = false;

        try {
            String name = "API";

            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName(name).build();
            Hello.HelloReply reply = blockingStub.hello(request);
            assertEquals("Hello, Hello, " + name + " World!!", reply.getMessage());
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            boolean wasFaultInjected = wasFaultInjected();

            if (wasFaultInjected) {
                testExceptionsThrownForAPIService.add(t.getMessage());

                // World failures.

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                }

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                }

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: INTERNAL")) {
                    expected = true;
                }

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
                    expected = true;
                }

                // Hello failures.

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                }

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                }

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: INTERNAL")) {
                    expected = true;
                }

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
                    expected = true;
                }

                // Otherwise.

                if (! expected) {
                    throw t;
                }
            } else {
                throw t;
            }
        }

        apiChannel.shutdownNow();
        apiChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    // Test 4
    // - Verify Filibuster generated the right number of tests for API Service.
    @Test
    @Order(4)
    public void testNumAssertionsForAPIService() {
        assertEqualsUnlessFilibusterDisabledByEnvironment(8, testExceptionsThrownForAPIService.size());
    }

    // Test 5, test the API service using a mock.
    // - It should test fewer failures then when not using the mock.
    @FilibusterTest(dataNondeterminism = true)
    @Order(5)
    public void testAPIServiceWithMock() throws InterruptedException {
        ManagedChannel apiChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
                .usePlaintext()
                .build();

        boolean expected = false;

        String name = "API";

        Hello.HelloExtendedReply response1 = Hello.HelloExtendedReply.newBuilder()
                .setName(name)
                .setFirstMessage("Hello, " + name + " World!!")
                .setSecondMessage(String.valueOf(Math.random()))
                .setCreatedAt("2023-01-01 01:02:03")
                .build();

        stubFor(unaryMethod(HelloServiceGrpc.getCompositionalHelloMethod()).willReturn(response1));

        try {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName(name).build();
            Hello.HelloReply reply = blockingStub.helloWithMock(request);
            assertEquals("Hello, Hello, " + name + " World!!", reply.getMessage());
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            boolean wasFaultInjected = wasFaultInjected();

            if (wasFaultInjected) {
                testExceptionsThrownForAPIServiceWithMock.add(t.getMessage());

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                }

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                }

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: INTERNAL")) {
                    expected = true;
                }

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
                    expected = true;
                }

                if (! expected) {
                    throw t;
                }
            } else {
                throw t;
            }
        }

        apiChannel.shutdownNow();
        apiChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    // Test 6
    // - Verify Filibuster generated the right number of tests for API Service using a mock only.
    @Test
    @Order(6)
    public void testNumAssertionsForAPIServiceWithMock() {
        assertEqualsUnlessFilibusterDisabledByEnvironment(4, testExceptionsThrownForAPIServiceWithMock.size());
    }

    // Test 7, test the API service using a mock and profile.
    // - It should test the correct number of failures (original, full graph, more than mock alone.)
    // TODO

    // Test 8
    // - Verify Filibuster generated the right number of tests for API Service using a mock and profile.
    // TODO

}