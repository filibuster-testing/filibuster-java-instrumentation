package cloud.filibuster.instrumentation.helpers;

import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.instrumentation.exceptions.MissingServiceSpecificationException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Networking {
    private static final Logger logger = Logger.getLogger(Networking.class.getName());

    private Networking() {

    }

    private static String readFilibusterNetworkingFile() throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get("../../networking.json"));
        return new String(encoded, StandardCharsets.US_ASCII);
    }

    public static int getPort(String serviceName) {
        try {
            String networkingFileContents = readFilibusterNetworkingFile();
            JSONObject networkingJsonObject = new JSONObject(networkingFileContents);
            JSONObject jsonObject = networkingJsonObject.getJSONObject(serviceName);
            return jsonObject.getInt("port");
        } catch (IOException e) {
            // Should use dependency injection somehow eventually.
            switch (serviceName) {
                case "hello":
                    return 5002;
                case "world":
                    return 5003;
                case "external":
                    return 5004;
                case "api_server":
                    return 5006;
                case "hello-mock":
                    return 6002;
                case "A":
                    return 7007;
                case "B":
                    return 7009;
                case "C":
                    return 7001;
                case "D":
                    return 8002;
                case "mock":
                    return 6007;
                default:
                    RuntimeException re = new MissingServiceSpecificationException();
                    re.initCause(e);
                    throw re;
            }
        }
    }

    public static String getHost(String serviceName) {
        try {
            String networkingFileContents = readFilibusterNetworkingFile();
            JSONObject networkingJsonObject = new JSONObject(networkingFileContents);
            JSONObject jsonObject = networkingJsonObject.getJSONObject(serviceName);
            return jsonObject.getString("default-host");
        } catch (IOException e) {
            // Should use dependency injection somehow eventually.
            return "0.0.0.0";
        }
    }

    public static String getFilibusterHost() {
        return Property.getServerHostProperty();
    }

    public static int getFilibusterPort() {
        return Property.getServerPortProperty();
    }

    public static Map.Entry<String, String> extractHostnameAndPortFromURI(String uri) {
        logger.log(Level.INFO, "uri: " + uri);

        String hostname;
        String port;

        String[] uriArray1 = uri.split("/");
        if (uriArray1[2].contains(":")) {
            // Port number present.

            // Get hostname.
            String[] uriArray = uri.split(":", 3);
            logger.log(Level.INFO, "uriArray[1]: " + uriArray[1]);
            hostname = uriArray[1].replace("//", "");

            // Get port.
            String[] portArray = uriArray[2].split("/", 2);
            port = portArray[0];
        } else {
            if (uriArray1[0].contains("https:")) {
                hostname = uriArray1[2];
                port = "443";
            } else {
                hostname = uriArray1[2];
                port = "80";
            }
        }

        return Pair.of(hostname, port);
    }

    public static String attemptHostnameResolution(String hostname, String defaultValue) {
        try {
            // Resolve host.
            InetAddress address = InetAddress.getByName(hostname);
            String resolvedHostname = address.getHostAddress();
            logger.log(Level.INFO, "resolved: " + resolvedHostname);
            return resolvedHostname;
        } catch (UnknownHostException e) {
            return defaultValue;
        }
    }
}
