package cloud.filibuster.junit.server.core.profiles;

import cloud.filibuster.exceptions.filibuster.FilibusterServiceProfileReaderException;
import cloud.filibuster.exceptions.filibuster.FilibusterServiceProfileWriterException;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ServiceProfile {
    private final HashMap<String, List<ServiceRequestAndResponse>> profile = new HashMap<>();

    public boolean sawMethod(String method) {
        return profile.containsKey(method);
    }

    public List<String> seenMethods() {
        return new ArrayList<>(profile.keySet());
    }

    @Nullable
    public List<ServiceRequestAndResponse> getServiceRequestAndResponsesForMethod(String method) {
        if (!sawMethod(method)) {
            return null;
        }

        return profile.get(method);
    }

    public void addToProfile(String method, GeneratedMessageV3 request, Status status, @Nullable GeneratedMessageV3 response) {
        ServiceRequestAndResponse serviceRequestAndResponse = new ServiceRequestAndResponse(
                request,
                status,
                response
        );

        addToProfile(method, serviceRequestAndResponse);
    }

    private void addToProfile(String method, ServiceRequestAndResponse serviceRequestAndResponse) {
        if (!profile.containsKey(method)) {
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

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ServiceRequestAndResponse srr = ServiceRequestAndResponse.fromJSONObject(obj);
                serviceProfile.addToProfile(key, srr);
            }
        }

        return serviceProfile;
    }

    public void writeServiceProfile(UUID testUUID) {
        File rootDirectory = new File("/tmp/filibuster/", testUUID.toString());
        try {
            //noinspection ResultOfMethodCallIgnored
            rootDirectory.mkdirs();
        } catch (SecurityException e) {
            throw new FilibusterServiceProfileWriterException("Filibuster failed to write out the service profile: ", e);
        }

        File directory = new File(rootDirectory, "fsp/");

        try {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        } catch (SecurityException e) {
            throw new FilibusterServiceProfileWriterException("Filibuster failed to write out the service profile: ", e);
        }

        // Write out the actual JSON data.
        Path fspFile = Paths.get(directory + "/latest.fsp");
        try {
            Files.write(fspFile, toJSONObject().toString(4).getBytes(Charset.defaultCharset()));
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

    public static List<ServiceProfile> loadFromDirectory(Path directory) {
        List<ServiceProfile> serviceProfiles = new ArrayList<>();
        Iterator it = FileUtils.iterateFiles(directory.toFile(), null, /* recursive= */ false);

        while (it.hasNext()) {
            File nextFile = (File) it.next();
            Path nextFilePath = nextFile.toPath();

            if (Files.isRegularFile(nextFilePath)) {
                ServiceProfile serviceProfile = ServiceProfile.loadFromFile(nextFilePath);
                serviceProfiles.add(serviceProfile);
            }
        }

        return serviceProfiles;
    }

    public static ServiceProfile loadFromFile(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), Charset.defaultCharset());
            JSONObject jsonObject = new JSONObject(content);
            return ServiceProfile.fromJSONObject(jsonObject);
        } catch (IOException e) {
            throw new FilibusterServiceProfileReaderException("Cannot load service profile from file " + path + ": " + e, e);
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
