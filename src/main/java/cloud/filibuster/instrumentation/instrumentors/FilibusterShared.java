package cloud.filibuster.instrumentation.instrumentors;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import io.grpc.Status;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import static java.util.Objects.requireNonNull;

public class FilibusterShared {
    public static String getForcedExceptionValue(JSONObject forcedException, String keyName, @Nullable String defaultValue) {
        requireNonNull(forcedException);

        if (forcedException.has(keyName)) {
            return forcedException.getString(keyName);
        } else {
            return defaultValue;
        }
    }

    public static String getForcedExceptionMetadataValue(JSONObject forcedException, String keyName, @Nullable String defaultValue) {
        requireNonNull(forcedException);

        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");

        if (forcedExceptionMetadata.has(keyName)) {
            return forcedExceptionMetadata.getString(keyName);
        } else {
            return defaultValue;
        }
    }

    public static Status generateExceptionFromForcedException(FilibusterClientInstrumentor filibusterClientInstrumentor) {
        JSONObject forcedException = filibusterClientInstrumentor.getForcedException();
        requireNonNull(forcedException);

        // Get description of the fault.
        String exceptionNameString = getForcedExceptionValue(forcedException, "name", "");
        String codeStr = getForcedExceptionMetadataValue(forcedException, "code", null);
        String descriptionStr = getForcedExceptionMetadataValue(forcedException, "description", null);
        String causeString = getForcedExceptionMetadataValue(forcedException, "cause", null);
        String causeMessageString = getForcedExceptionMetadataValue(forcedException, "cause_message", null);

        Status status = generateExceptionFromForcedException(
                exceptionNameString,
                codeStr,
                descriptionStr,
                causeString,
                causeMessageString);

        // Notify Filibuster of failure.
        HashMap<String, String> additionalMetadata = new HashMap<>();
        additionalMetadata.put("code", codeStr);
        additionalMetadata.put("description", descriptionStr);
        filibusterClientInstrumentor.afterInvocationWithException(exceptionNameString, causeString, additionalMetadata, status);

        // Return status.
        return status;
    }

    public static Status generateExceptionFromForcedException(
            String exceptionNameString,
            @Nullable String codeStr,
            @Nullable String descriptionStr,
            @Nullable String causeString,
            @Nullable String causeMessageString
    ) {
        // Status object to return to the user.
        Status status;

        if (causeString != null && !causeString.isEmpty()) {
            // Cause always takes priority in gRPC because it implies a UNKNOWN response.
            try {
                String throwableMessage = "Filibuster generated exception.";

                if (causeMessageString != null && !causeMessageString.isEmpty()) {
                    throwableMessage = causeMessageString;
                }

                Throwable throwable = Class.forName(causeString).asSubclass(Throwable.class).getConstructor(new Class[]{String.class}).newInstance(throwableMessage);
                status = Status.fromThrowable(throwable);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new FilibusterFaultInjectionException("Unable to generate custom exception from string '" + causeString + "':" + e, e);
            }
        } else if (!codeStr.isEmpty()) {
            // Code is checked secondary and ignored if cause is present.
            Status.Code code = Status.Code.valueOf(codeStr);

            if (descriptionStr != null) {
                status = Status.fromCode(code).withDescription(descriptionStr);
            } else {
                status = Status.fromCode(code);
            }
        } else {
            // Otherwise, we do not know what to inject.
            throw new FilibusterFaultInjectionException("No code or cause provided for injection of io.grpc.StatusRuntimeException.");
        }

        return status;
    }
}
