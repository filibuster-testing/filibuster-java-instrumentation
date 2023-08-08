package cloud.filibuster.functional.java.compositional;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.WorldServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.server.core.profiles.ServiceProfile;
import cloud.filibuster.junit.server.core.profiles.ServiceProfileBehavior;
import cloud.filibuster.junit.server.core.reports.ServerInvocationAndResponseReport;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

import static cloud.filibuster.instrumentation.helpers.Property.setCallsiteLineNumberProperty;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor.clearVectorClockForRequestId;
import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startExternalServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopExternalServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopWorldServerAndWaitUntilUnavailable;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.protocols.GrpcAssertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.assertions.protocols.GrpcAssertions.wasFaultInjectedOnService;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//
// Application:
//
// - ApiService -> HelloService -> WorldService
//
// - Hello -> World
//    - wraps any error of World in DATA_LOSS, propagates upstream
// - APIServer -> Hello
//    - Hello returns DATA_LOSS error -> RESOURCE_EXHAUSTED
//    - communications failure of Hello -> FAILED_PRECONDITION wrapping error.
//
// Fault Configuration:
// - UNAVAILABLE
// - UNKNOWN
// - INTERNAL
// - UNIMPLEMENTED
// - DEADLINE_EXCEEDED
//
@SuppressWarnings("Java8ApiChecker")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTestWithServiceFaultProfile {

    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("hello-mock"))
            .build();

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();
        startWorldServerAndWaitUntilAvailable();
        startExternalServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopAllServices() throws InterruptedException {
        stopExternalServerAndWaitUntilUnavailable();
        stopWorldServerAndWaitUntilUnavailable();
        stopHelloServerAndWaitUntilUnavailable();
        stopAPIServerAndWaitUntilUnavailable();
    }

    @BeforeEach
    protected void resetMyHelloServiceState() {
        MyHelloService.shouldReturnRuntimeExceptionWithCause = false;
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithCause = false;
    }

    @BeforeEach
    protected void resetFilibusterState() {
        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
        FilibusterClientInstrumentor.clearVectorClockForRequestId();
    }

    @BeforeAll
    protected static void enablePrettyDistributedExecutionIndexes() {
        setCallsiteLineNumberProperty(false);
    }

    @AfterAll
    protected static void disablePrettyDistributedExecutionIndexes() {
        setCallsiteLineNumberProperty(true);
    }

    @BeforeEach
    protected void clearStateFromLastExecution() {
        clearDistributedExecutionIndexForRequestId();
        clearVectorClockForRequestId();
    }

    @BeforeAll
    public static void cleanState() {
        ServerInvocationAndResponseReport.clear();
    }

    // *****************************************************************************************************************
    // Phase 1: Mimic old Filibuster by running all the services together.
    //          Explicitly testing for 4 failures, but system produced 2 new failures (FAILED_PRECONDITION, DATA_LOSS.)
    // *****************************************************************************************************************

    private static final List<String> testExceptionsThrownForAPIService = new ArrayList<>();

    @TestWithFilibuster(dataNondeterminism = true)
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

                if (t.getMessage().equals("RESOURCE_EXHAUSTED: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNKNOWN")) {
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

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: UNKNOWN")) {
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
        assertEquals(10, testExceptionsThrownForAPIService.size());
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
        assertEquals(6, serviceProfile.getServiceRequestAndResponsesForMethod("cloud.filibuster.examples.HelloService/CompositionalHello").size());
        assertEquals(11, serviceProfile.getServiceRequestAndResponsesForMethod("cloud.filibuster.examples.APIService/Hello").size());
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
    @TestWithFilibuster(dataNondeterminism = true)
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

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNKNOWN")) {
                    expected = true;
                }

                boolean wasFaultInjectedOnWorldService = wasFaultInjectedOnService("WorldService");
                assertTrue(wasFaultInjectedOnWorldService);

                boolean wasFaultInjectedOnWorldMethod = wasFaultInjectedOnMethod(WorldServiceGrpc.getWorldMethod());
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
        assertEquals(5, testExceptionsThrownForHelloService.size());
    }

    // Test 6
    // - Verify we can load the service profile and make a copy of it.
    @Test
    @Order(6)
    public void testHelloServiceProfile() throws IOException {
        ServiceProfile serviceProfile = ServiceProfile.readServiceProfile("/tmp/filibuster/fsp/latest.fsp");
        assertEquals(true, serviceProfile.sawMethod("cloud.filibuster.examples.HelloService/CompositionalHello"));
        assertEquals(1, serviceProfile.seenMethods().size());
        assertEquals(6, serviceProfile.getServiceRequestAndResponsesForMethod("cloud.filibuster.examples.HelloService/CompositionalHello").size());
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
    @TestWithFilibuster(dataNondeterminism = true)
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

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: UNKNOWN")) {
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
        assertEquals(5, testExceptionsThrownForAPIServiceWithMock.size());
    }

    // Test 9
    // - Verify we can load the service profile and make a copy of it.
    @Test
    @Order(9)
    public void testAPIServiceProfile() throws IOException {
        ServiceProfile serviceProfile = ServiceProfile.readServiceProfile("/tmp/filibuster/fsp/latest.fsp");
        assertEquals(true, serviceProfile.sawMethod("cloud.filibuster.examples.APIService/HelloWithMock"));
        assertEquals(1, serviceProfile.seenMethods().size());
        assertEquals(6, serviceProfile.getServiceRequestAndResponsesForMethod("cloud.filibuster.examples.APIService/HelloWithMock").size());
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
    @TestWithFilibuster(dataNondeterminism = true, serviceProfilesPath = "/tmp/filibuster/fsp/", serviceProfileBehavior = ServiceProfileBehavior.FAULT)
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

                if (t.getMessage().equals("RESOURCE_EXHAUSTED: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNKNOWN")) {
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

                if (t.getMessage().equals("FAILED_PRECONDITION: io.grpc.StatusRuntimeException: UNKNOWN")) {
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
        assertEquals(10, testExceptionsThrownForAPIServiceWithMockAndServiceProfile.size());
    }

    // Voila!
}