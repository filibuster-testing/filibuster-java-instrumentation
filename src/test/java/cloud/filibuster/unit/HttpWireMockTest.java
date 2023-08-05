package cloud.filibuster.unit;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class HttpWireMockTest {
    private WireMockServer wireMockServer;

    @BeforeEach
    public void setup() {
        wireMockServer = new WireMockServer(); //No-args constructor will start on port 8080, no HTTPS
        wireMockServer.start();
        WireMock.configureFor("localhost", 8080);
    }

    @Test
    public void testWireMock() throws Exception {
        // Set up a mock HTTP server to respond with a status of 200 and body of "Hello World" when a GET request is made
        WireMock.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Hello World")));

        // Use Apache HttpClient to make the GET request
        HttpResponse httpResponse = HttpClients.createDefault().execute(new HttpGet("http://localhost:8080/test"));

        // Convert the response to a String
        String responseBody = EntityUtils.toString(httpResponse.getEntity());

        // Assert that the mock server responded with "Hello World"
        assertEquals("Hello World", responseBody);
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

}