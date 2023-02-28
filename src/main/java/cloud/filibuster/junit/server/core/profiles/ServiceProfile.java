package cloud.filibuster.junit.server.core.profiles;

import cloud.filibuster.exceptions.filibuster.FilibusterServiceProfileReaderException;
import cloud.filibuster.exceptions.filibuster.FilibusterServiceProfileWriterException;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ServiceProfile {
    private final HashMap<String, List<ServiceRequestAndResponse>> profile = new HashMap<>();

    public void addToProfile(String method, GeneratedMessageV3 request, Status status, @Nullable GeneratedMessageV3 response) {
        ServiceRequestAndResponse serviceRequestAndResponse = new ServiceRequestAndResponse(
                request,
                status,
                response
        );

        addToProfile(method, serviceRequestAndResponse);
    }

    private void addToProfile(String method, ServiceRequestAndResponse serviceRequestAndResponse) {
        if (! profile.containsKey(method)) {
            profile.put(method, new ArrayList<>());
        }

        profile.get(method).add(serviceRequestAndResponse);
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();

        for (Map.Entry<String, List<ServiceRequestAndResponse>> entry : profile.entrySet()) {
            List<JSONObject> jsonObjectList = new ArrayList<>();
            for (ServiceRequestAndResponse srr : entry.getValue()) {
                jsonObjectList.add(srr.toJSONObject());
            }

            jsonObject.put(entry.getKey(), jsonObjectList);
        }

        return jsonObject;
    }

    public static ServiceProfile fromJSONObject(JSONObject jsonObject) {
        ServiceProfile serviceProfile = new ServiceProfile();

        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONArray jsonArray = jsonObject.getJSONArray(key);

            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ServiceRequestAndResponse srr = ServiceRequestAndResponse.fromJSONObject(obj);
                serviceProfile.addToProfile(key, srr);
            }
        }

        return serviceProfile;
    }

    public void writeServiceProfile() {
        Path directory = Paths.get("/tmp/filibuster/fsp");

        try {
            Files.createDirectory(directory);
        } catch(FileAlreadyExistsException e) {
            // Nothing, directory already exists.
        } catch (IOException e) {
            throw new FilibusterServiceProfileWriterException("Filibuster failed to write out the service profile: ", e);
        }

        // Write out the actual JSON data.
        Path fspFile = Paths.get(directory + "/latest.fsp");
        try {
            Files.write(fspFile, toJSONObject().toString().getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterServiceProfileWriterException("Filibuster failed to write out the service profile: ", e);
        }
    }

    @SuppressWarnings("DefaultCharset")
    public static ServiceProfile readServiceProfile(String fileName) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            JSONObject contentObject = new JSONObject(content);
            ServiceProfile serviceProfile = fromJSONObject(contentObject);
            return serviceProfile;
        } catch (IOException e) {
            throw new FilibusterServiceProfileReaderException("Filibuster failed to read the service profile at " + fileName, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof ServiceProfile)) {
            return false;
        }

        ServiceProfile sp = (ServiceProfile) o;

        return Objects.equals(this.profile, sp.profile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.profile);
    }
}
