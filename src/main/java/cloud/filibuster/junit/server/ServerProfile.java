package cloud.filibuster.junit.server;

import cloud.filibuster.exceptions.filibuster.FilibusterServerProfileReaderException;
import cloud.filibuster.exceptions.filibuster.FilibusterServerProfileWriterException;
import com.google.gson.Gson;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class ServerProfile {
    private static final Gson gson = new Gson();

    private HashMap<String, ServerRequestAndResponse> profile = new HashMap<>();

    public class ServerRequestAndResponse {
        private GeneratedMessageV3 request;
        private Status status;
        @Nullable
        private GeneratedMessageV3 response;

        public ServerRequestAndResponse(
            GeneratedMessageV3 request,
            Status status,
            GeneratedMessageV3 response
        ) {
            this.request = request;
            this.status = status;
            this.response = response;
        }
    }

    public void addToProfile(String method, GeneratedMessageV3 request, Status status, @Nullable GeneratedMessageV3 response) {
        ServerRequestAndResponse serverRequestAndResponse = new ServerRequestAndResponse(
                request,
                status,
                response
        );

        profile.put(method, serverRequestAndResponse);
    }

    public JSONObject toJSONObject() {
        String gsonSerialized = gson.toJson(this);
        return new JSONObject(gsonSerialized);
    }

    public static ServerProfile fromJSONObject(JSONObject jsonObject) {
        String gsonPayloadString = jsonObject.toString();
        ServerProfile target = gson.fromJson(gsonPayloadString, ServerProfile.class);
        return target;
    }

    public void writeServerProfile() {
        Path directory = Paths.get("/tmp/filibuster/fsp");

        try {
            Files.createDirectory(directory);
        } catch(FileAlreadyExistsException e) {
            // Nothing, directory already exists.
        } catch (IOException e) {
            throw new FilibusterServerProfileWriterException("Filibuster failed to write out the server profile: ", e);
        }

        // Write out the actual JSON data.
        Path fspFile = Paths.get(directory + "/latest.fsp");
        try {
            Files.write(fspFile, toJSONObject().toString().getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterServerProfileWriterException("Filibuster failed to write out the server profile: ", e);
        }
    }

    public static ServerProfile readServerProfile(String fileName) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            JSONObject contentObject = new JSONObject(content);
            ServerProfile serverProfile = fromJSONObject(contentObject);
            return serverProfile;
        } catch (IOException e) {
            throw new FilibusterServerProfileReaderException("Filibuster failed to read the server profile at " + fileName, e);
        }
    }
}
