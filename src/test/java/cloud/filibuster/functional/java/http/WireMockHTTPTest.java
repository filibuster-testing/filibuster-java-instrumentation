package cloud.filibuster.functional.java.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WireMockHTTPTest {
    private static WireMockServer wireMockServer;
    private final static Set<String> testErrorCodesReceived = new HashSet<>();

    private static int numberOfTestsExecuted;

    private final List<String> validErrorCodes = Arrays.asList("404", "503");

//    @BeforeAll
//    public static void setup() throws IOException, InterruptedException {
//        startHelloServerAndWaitUntilAvailable();
//
//        wireMockServer = new WireMockServer(options()
//                .bindAddress(Networking.getHost("world"))
//                .port(Networking.getPort("world")));
//        wireMockServer.start();
//        wireMockServer.stubFor(get("/")
//                .willReturn(ok()));
//    }
//
//    @AfterAll
//    public static void tearDown() {
//        wireMockServer.stop();
//    }
//
//    @DisplayName("Test world route with Filibuster and WireMock.")
//    @TestWithFilibuster
//    @Order(1)
//    public void testHelloAndWireMockWorldServiceWithFilibuster() {
//        numberOfTestsExecuted++;
//
//        try {
//            String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
//            WebClient webClient = TestHelper.getTestWebClient(baseURI);
//            RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/world", HttpHeaderNames.ACCEPT, "application/json");
//            AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
//            ResponseHeaders headers = response.headers();
//            String statusCode = headers.get(HttpHeaderNames.STATUS);
//
//            if (wasFaultInjected()) {
//                testErrorCodesReceived.add(statusCode);
//                assertTrue(validErrorCodes.contains(statusCode));
//            } else {
//                assertEquals("200", statusCode);
//            }
//        } catch (Throwable t) {
//            fail();
//        }
//    }
//
//    @DisplayName("Verify correct number of injected error codes.")
//    @Test
//    @Order(2)
//    public void testNumAssertions() {
//        assertEquals(validErrorCodes.size(), testErrorCodesReceived.size());
//    }
//
//    @DisplayName("Verify correct number of generated Filibuster tests.")
//    @Test
//    @Order(3)
//    public void testNumberOfTestsExecuted() {
//        assertEquals(5, numberOfTestsExecuted);
//    }

}