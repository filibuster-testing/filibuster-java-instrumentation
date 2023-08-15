package cloud.filibuster.instrumentation.helpers;

import cloud.filibuster.instrumentation.exceptions.CounterexampleFileDoesNotExistException;
import cloud.filibuster.instrumentation.exceptions.CounterexampleInvalidException;
import cloud.filibuster.instrumentation.exceptions.CounterexampleIsNullException;
import cloud.filibuster.instrumentation.exceptions.EnvironmentMissingCounterexampleException;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Counterexample {
    private Counterexample() {

    }

    public static boolean canLoadCounterexample() {
        String counterexampleFileName = Property.getInstrumentationCounterexampleFileProperty();

        if (counterexampleFileName != null && !counterexampleFileName.isEmpty()) {
            File f = new File(counterexampleFileName);
            return f.exists();
        }

        return false;
    }

    public static JSONObject loadCounterexampleAsJsonObjectFromEnvironment() {
        String counterexampleFileName = Property.getInstrumentationCounterexampleFileProperty();

        if (counterexampleFileName != null && !counterexampleFileName.isEmpty()) {
            return loadCounterexampleAsJsonObject(counterexampleFileName);
        } else {
            throw new EnvironmentMissingCounterexampleException();
        }
    }

    public static JSONObject loadCounterexampleAsJsonObject(String counterexampleFileName) {
        if (counterexampleFileName != null && !counterexampleFileName.isEmpty()) {
            File f = new File(counterexampleFileName);
            if (f.exists()) {
                String rawCounterexample = readLineByLineJava8(counterexampleFileName);
                return new JSONObject(rawCounterexample);
            } else {
                throw new CounterexampleFileDoesNotExistException();
            }
        } else {
            throw new CounterexampleIsNullException();
        }
    }

    public static JSONObject loadTestExecutionFromCounterexample(JSONObject counterexample) {
        if (counterexample != null) {
            return new JSONObject(counterexample.getString("TestExecution"));
        } else {
            throw new CounterexampleInvalidException();
        }
    }

    @Nullable
    public static JSONObject shouldFailRequestWith(String distributedExecutionIndex, JSONObject testExecution) {
        JSONArray failures = testExecution.getJSONArray("failures");

        for (int i = 0; i < failures.length(); i++) {
            JSONObject failure = failures.getJSONObject(i);
            String failureDistributedExecutionIndex = failure.getString("execution_index");
            if (distributedExecutionIndex.equals(failureDistributedExecutionIndex)) {
                return failure;
            }
        }

        return null;
    }

    public static JSONObject shouldFailRequestWithOrDefault(String distributedExecutionIndex, JSONObject testExecution) {
        JSONObject failure = shouldFailRequestWith(distributedExecutionIndex, testExecution);

        if (failure == null) {
            JSONObject response = new JSONObject();
            response.put("execution_index", distributedExecutionIndex);
            return response;
        }

        return failure;
    }

    @SuppressWarnings({"CatchAndPrintStackTrace", "SystemOut"})
    private static String readLineByLineJava8(String filePath)
    {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }
}
