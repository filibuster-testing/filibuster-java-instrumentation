package cloud.filibuster.functional.java.http;

import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HttpWireMockTest {
    static WireMockServer wireMockServer;
    private static final String path = "/test";
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    @BeforeAll
    public static void startHelloService() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
    }

    @DisplayName("Test stubbing HTTP requests using WireMock.")
    @TestWithFilibuster
    @Order(1)
    public void testWireMockHTTPStubbing() throws IOException {
        configureFor(Networking.getHost("mock"), Networking.getPort("mock"));
        stubFor(get(urlEqualTo(path)).willReturn(aResponse().withBody("Hello, world!")));

        String uri = "http://" + Networking.getHost("mock") + ":" + Networking.getPort("mock")
                + "/" + path;
        HttpGet request = new HttpGet(uri);
        HttpResponse httpResponse = httpClient.execute(request);
        String stringResponse = convertResponseToString(httpResponse);

        verify(getRequestedFor(urlEqualTo(path)));
        assertEquals("Hello, world!", stringResponse);
    }

    private static String convertResponseToString(HttpResponse response) throws IOException {
        InputStream responseStream = response.getEntity().getContent();
        Scanner scanner = new Scanner(responseStream, "UTF-8");
        String stringResponse = scanner.useDelimiter("\\Z").next();
        scanner.close();
        return stringResponse;
    }

}
