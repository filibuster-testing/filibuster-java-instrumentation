package cloud.filibuster.instrumentation.instrumentors;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import io.grpc.Status;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import static java.util.Objects.requireNonNull;

public class FilibusterShared {
    @SuppressWarnings("Varifier")
    public static Status generateExceptionFromForcedException(FilibusterClientInstrumentor filibusterClientInstrumentor) {
        JSONObject forcedException = filibusterClientInstrumentor.getForcedException();
        requireNonNull(forcedException);

        // Get description of the fault.
        String exceptionNameString = forcedException.getString("name");
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
        String codeStr = forcedExceptionMetadata.getString("code");

        String descriptionStr = null;
        if (forcedExceptionMetadata.has("description")) {
            descriptionStr = forcedExceptionMetadata.getString("description");
        }

        String causeString = null;
        if (forcedExceptionMetadata.has("cause")) {
            causeString = forcedExceptionMetadata.getString("cause");
        }

        String causeMessageString = null;
        if (forcedExceptionMetadata.has("cause_message")) {
            causeMessageString = forcedExceptionMetadata.getString("cause_message");
        }

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

        // Notify Filibuster of failure.
        HashMap<String, String> additionalMetadata = new HashMap<>();
        additionalMetadata.put("code", codeStr);
        additionalMetadata.put("description", descriptionStr);
        filibusterClientInstrumentor.afterInvocationWithException(exceptionNameString, causeString, additionalMetadata);

        return status;
    }
}
