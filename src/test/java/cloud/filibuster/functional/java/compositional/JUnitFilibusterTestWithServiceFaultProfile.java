package cloud.filibuster.functional.java.compositional;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.JUnitBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.server.core.profiles.ServiceProfile;
import cloud.filibuster.junit.server.core.profiles.ServiceProfileBehavior;
import cloud.filibuster.junit.server.core.reports.ServerInvocationAndResponseReport;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
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

@SuppressWarnings("Java8ApiChecker")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTestWithServiceFaultProfile extends JUnitBaseTest {

    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("hello-mock"))
            .build();

    @BeforeAll
    public static void startAPIServer() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopAPIServer() throws InterruptedException {
        stopAPIServerAndWaitUntilUnavailable();
    }

    // *****************************************************************************************************************
    // Phase 1: Mimic old Filibuster by running all the services together.
    //          Explicitly testing for 4 failures, but system produced 2 new failures (FAILED_PRECONDITION, DATA_LOSS.)
    // *****************************************************************************************************************

    private static final List<String> testExceptionsThrownForAPIService = new ArrayList<>();

    @FilibusterTest(dataNondeterminism = true)
    @Order(1)
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

                if (t.getMessage().equals("RESOURCE_EXHAUSTED: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                }

                if (t.getMessage().equals("RESOURCE_EXHAUSTED: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                }

                if (t.getMessage().equals("RESOURCE_EXHAUSTED: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: INTERNAL")) {
                    expected = true;
                }

                if (t.getMessage().equals("RESOURCE_EXHAUSTED: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
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

    // Test 2
    // - Verify Filibuster generated the right number of tests for API Service.
    @Test
    @Order(2)
    public void testNumAssertionsForAPIService() {
        assertEqualsUnlessFilibusterDisabledByEnvironment(8, testExceptionsThrownForAPIService.size());
    }

    // Test 3
    // - Verify we can load the service profile and make a copy of it.
    @Test
    @Order(3)
    public void testEndToEndServiceProfile() throws IOException {
        ServiceProfile serviceProfile = ServiceProfile.readServiceProfile("/tmp/filibuster/fsp/latest.fsp");
        assertEquals(true, serviceProfile.sawMethod("cloud.filibuster.examples.HelloService/CompositionalHello"));
        assertEquals(true, serviceProfile.sawMethod("cloud.filibuster.examples.APIService/Hello"));
        assertEquals(2, serviceProfile.seenMethods().size());
        assertEquals(5, serviceProfile.getServiceRequestAndResponsesForMethod("cloud.filibuster.examples.HelloService/CompositionalHello").size());
        assertEquals(9, serviceProfile.getServiceRequestAndResponsesForMethod("cloud.filibuster.examples.APIService/Hello").size());
        Files.delete(Path.of("/tmp/filibuster/fsp/latest.fsp"));
        ServerInvocationAndResponseReport.clear();
    }

    // *****************************************************************************************************************
    // Phase 2: Test Hello -> World using both.
    //          Done for expediency, since normally we would test World, but then test Hello.
    //          Expedient, since World doesn't produce any interesting behavior and just returns the exact errors.
    // *****************************************************************************************************************

    private static final List<String> testExceptionsThrownForHelloService = new ArrayList<>();

    // Test 4
    // - Call Hello (which calls World) with a random set of arguments.
    // Produces:
    // - Server failure profile for Hello with the possible failure responses.
    // - Server profile that contains various requests and responses for none of the inputs we will provide.
    @FilibusterTest(dataNondeterminism = true)
    @Order(4)
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

    // Test 5
    // - Verify Filibuster generated the right number of tests for Hello Service.
    @Test
    @Order(5)
    public void testNumAssertionsForHelloService() {
        assertEqualsUnlessFilibusterDisabledByEnvironment(4, testExceptionsThrownForHelloService.size());
    }

    // Test 6
    // - Verify we can load the service profile and make a copy of it.
    @Test
    @Order(6)
    public void testHelloServiceProfile() throws IOException {
        ServiceProfile serviceProfile = ServiceProfile.readServiceProfile("/tmp/filibuster/fsp/latest.fsp");
        assertEquals(true, serviceProfile.sawMethod("cloud.filibuster.examples.HelloService/CompositionalHello"));
        assertEquals(1, serviceProfile.seenMethods().size());
        assertEquals(5, serviceProfile.getServiceRequestAndResponsesForMethod("cloud.filibuster.examples.HelloService/CompositionalHello").size());
        Files.move(Path.of("/tmp/filibuster/fsp/latest.fsp"), Path.of("/tmp/filibuster/fsp/hello.fsp"), StandardCopyOption.REPLACE_EXISTING);
        ServerInvocationAndResponseReport.clear();
    }

    // *****************************************************************************************************************
    // Phase 3: Test API server with a mock of Hello.
    //          This is a negative result because the only code returned by Hello, DATA_LOSS, isn't tested.
    // *****************************************************************************************************************

    private static final List<String> testExceptionsThrownForAPIServiceWithMock = new ArrayList<>();

    // Test 7, test the API service using a mock.
    // - It should test fewer failures then when not using the mock.
    @FilibusterTest(dataNondeterminism = true)
    @Order(7)
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

    // Test 8
    // - Verify Filibuster generated the right number of tests for API Service using a mock only.
    @Test
    @Order(8)
    public void testNumAssertionsForAPIServiceWithMock() {
        assertEqualsUnlessFilibusterDisabledByEnvironment(4, testExceptionsThrownForAPIServiceWithMock.size());
    }

    // Test 9
    // - Verify we can load the service profile and make a copy of it.
    @Test
    @Order(9)
    public void testAPIServiceProfile() throws IOException {
        ServiceProfile serviceProfile = ServiceProfile.readServiceProfile("/tmp/filibuster/fsp/latest.fsp");
        assertEquals(true, serviceProfile.sawMethod("cloud.filibuster.examples.APIService/HelloWithMock"));
        assertEquals(1, serviceProfile.seenMethods().size());
        assertEquals(5, serviceProfile.getServiceRequestAndResponsesForMethod("cloud.filibuster.examples.APIService/HelloWithMock").size());
        Files.delete(Path.of("/tmp/filibuster/fsp/latest.fsp"));
        ServerInvocationAndResponseReport.clear();
    }

    // *****************************************************************************************************************
    // Phase 4: Test API server with a mock of Hello and using the Hello service profile.
    //          This should execute the original number of tests.
    // *****************************************************************************************************************

    private static final List<String> testExceptionsThrownForAPIServiceWithMockAndServiceProfile = new ArrayList<>();

    // Test 10, test the API service using a mock.
    // - It should test the correct number of failures using the mock and service profile together.
    @FilibusterTest(dataNondeterminism = true, serviceProfilesPath = "/tmp/filibuster/fsp/", serviceProfileBehavior = ServiceProfileBehavior.FAULT)
    @Order(10)
    public void testAPIServiceWithMockAndServiceProfile() throws InterruptedException {
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
                testExceptionsThrownForAPIServiceWithMockAndServiceProfile.add(t.getMessage());

                // World failures.

                if (t.getMessage().equals("RESOURCE_EXHAUSTED: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                }

                if (t.getMessage().equals("RESOURCE_EXHAUSTED: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                }

                if (t.getMessage().equals("RESOURCE_EXHAUSTED: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: INTERNAL")) {
                    expected = true;
                }

                if (t.getMessage().equals("RESOURCE_EXHAUSTED: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
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

    // Test 11
    // - Verify Filibuster generated the right number of tests for API Service using a mock only.
    @Test
    @Order(11)
    public void testNumAssertionsForAPIServiceWithMockAndServiceProfile() {
        assertEqualsUnlessFilibusterDisabledByEnvironment(8, testExceptionsThrownForAPIServiceWithMockAndServiceProfile.size());
    }

    // Voila!
}