package cloud.filibuster.functional.shouldFails;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.server.core.FilibusterCore;
import cloud.filibuster.junit.server.core.reports.TestExecutionReport;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FilibusterFailureReportingAfterBlockTest extends JUnitAnnotationBaseTest {

    static class FakeClass {
        private FakeClass() {

        }

        static void FakeMethod() throws ArithmeticException {
            throw new ArithmeticException();
        }
    }

    public static class IgnoreAfterEachInterceptor implements InvocationInterceptor {
        public IgnoreAfterEachInterceptor() {

        }

        @Override
        public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
            invocation.skip();
        }
    }

    @TestWithFilibuster()
    @Order(1)
    @DisplayName("Should Fail : This test throws exceptions")
    public void testShouldFail() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            blockingStub.unimplemented(request);
        } catch (StatusRuntimeException e) {
            assertTrue(true);
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);

        assertEquals("main test block failure", "");
    }

    @AfterEach
    public void after1() {
        assertEquals("first after block failure", "");
    }

    @AfterEach
    public void after2() {
        assertNotEquals("second after block does not fail", "");
    }


    @AfterEach
    public void after3() {
        assertEquals("third after block fails", "");
    }

    @AfterEach
    public void after4() {
        FakeClass.FakeMethod();
    }

    @Order(2)
    @Test
    @DisplayName("Should Pass : Check Assert Message Failures and Counts")
    @ExtendWith(IgnoreAfterEachInterceptor.class)
    public void testMustPassMessageCountAndNature() {
        TestExecutionReport testExecutionReport = FilibusterCore.getMostRecentInitialTestExecutionReport();
        List<TestExecutionReport.FailureMetadata> a = testExecutionReport.getFailures();
        assertEquals(a.stream().filter(f -> f.getAssertionFailureMessage().contains("<main test block failure>")).count(),
                1);
        assertEquals(a.stream().filter(f -> f.getAssertionFailureMessage().contains("<first after block failure>")).count(),
                1);
        assertEquals(a.stream().filter(f -> f.getAssertionFailureMessage().contains("<third after block fails>")).count(),
                1);
        List<TestExecutionReport.FailureMetadata> arithmeticFailureList = a.stream().filter(f -> f.getAssertionFailureMessage().contains("ArithmeticException")).collect(Collectors.toList());
        assertEquals(arithmeticFailureList.size(),
                1);
        TestExecutionReport.FailureMetadata arithmeticFailure = arithmeticFailureList.get(0);
        String arithmeticStackTrace = arithmeticFailure.getAssertionFailureStackTrace();
        assertTrue(arithmeticStackTrace.contains("FilibusterFailureReportingAfterBlockTest$FakeClass.FakeMethod"));
        assertTrue(arithmeticStackTrace.contains("FilibusterFailureReportingAfterBlockTest.after4"));
        assertTrue(arithmeticStackTrace.indexOf("FilibusterFailureReportingAfterBlockTest$FakeClass.FakeMethod") <
                arithmeticStackTrace.indexOf("FilibusterFailureReportingAfterBlockTest.after4"));
        assertEquals(a.stream().filter(f -> "<second after block does not fail>".contains(f.getAssertionFailureMessage())).count(),
                0);
        assertEquals(a.size(), 4);
    }

}
