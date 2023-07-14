package cloud.filibuster.junit.statem.keys;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cloud.filibuster.junit.statem.keys.FaultKeyType.METHOD;
import static cloud.filibuster.junit.statem.keys.FaultKeyType.METHOD_AND_REQUEST;
import static cloud.filibuster.junit.statem.keys.FaultKeyType.METHOD_AND_CODE;
import static cloud.filibuster.junit.statem.keys.FaultKeyType.METHOD_REQUEST_AND_CODE;

public class SingleFaultKey<ReqT, ResT> implements FaultKey {
    private FaultKeyType faultKeyType = METHOD;

    private String method = "";

    private String request = "";

    private String code = "";

    public SingleFaultKey(Builder builder) {
        this.method = builder.method;
        this.request = builder.request;
        this.code = builder.code;
        this.faultKeyType = builder.faultKeyType;;
    }

    public SingleFaultKey(MethodDescriptor<ReqT, ResT> methodDescriptor) {
        this.method = methodDescriptor.getFullMethodName();
    }

    public SingleFaultKey(MethodDescriptor<ReqT, ResT> methodDescriptor, Status.Code code) {
        this.faultKeyType = METHOD_AND_CODE;
        this.method = methodDescriptor.getFullMethodName();
        this.code = code.toString();
    }

    public SingleFaultKey(MethodDescriptor<ReqT, ResT> methodDescriptor, ReqT request) {
        this.faultKeyType = METHOD_AND_REQUEST;
        this.method = methodDescriptor.getFullMethodName();
        this.request = request.toString();
    }

    public SingleFaultKey(MethodDescriptor<ReqT, ResT> methodDescriptor, Status.Code code, ReqT request) {
        this.faultKeyType = METHOD_REQUEST_AND_CODE;
        this.method = methodDescriptor.getFullMethodName();
        this.request = request.toString();
        this.code = code.toString();
    }

    public FaultKeyType getFaultKeyType() {
        return this.faultKeyType;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SingleFaultKey)) {
            return false;
        }

        return Objects.equals(this.toString(), other.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.toString());
    }

    @Override
    public String toString() {
        List<String> toStringArray = new ArrayList<>();
        toStringArray.add(this.faultKeyType.toString());

        switch (this.faultKeyType) {
            case METHOD:
                toStringArray.add(this.method);
                toStringArray.add("");
                toStringArray.add("");
                break;
            case METHOD_AND_CODE:
                toStringArray.add(this.method);
                toStringArray.add(this.code);
                toStringArray.add("");
                break;
            case METHOD_AND_REQUEST:
                toStringArray.add(this.method);
                toStringArray.add("");
                toStringArray.add(this.request);
                break;
            case METHOD_REQUEST_AND_CODE:
                toStringArray.add(this.method);
                toStringArray.add(this.code);
                toStringArray.add(this.request);
                break;
        }

        return String.join("", toStringArray);
    }

    public static List<SingleFaultKey> generateFaultKeysInDecreasingGranularity(JSONObject rpcWhereFaultsInjected) {
        List<SingleFaultKey> listOfFaultKeys = new ArrayList<>();

        String method = "";
        String request = "";
        String code = "";

        method = rpcWhereFaultsInjected.getString("method");

        if (rpcWhereFaultsInjected.has("args")) {
            JSONObject argsJsonObject = rpcWhereFaultsInjected.getJSONObject("args");

            if (argsJsonObject.has("toString")) {
                request = argsJsonObject.getString("toString");
            }
        }

        if (rpcWhereFaultsInjected.has("forced_exception")) {
            JSONObject forcedExceptionObject = rpcWhereFaultsInjected.getJSONObject("forced_exception");
            if (forcedExceptionObject.has("metadata")) {
                JSONObject metadataObject = forcedExceptionObject.getJSONObject("metadata");
                if (metadataObject.has("code")) {
                    code = metadataObject.getString("code");
                }
            }
        }

        listOfFaultKeys.add(
                new SingleFaultKey.Builder().setType(METHOD_REQUEST_AND_CODE).setMethod(method).setRequest(request).setCode(code).build()
        );
        listOfFaultKeys.add(
                new SingleFaultKey.Builder().setType(METHOD_AND_REQUEST).setMethod(method).setRequest(request).build()
        );
        listOfFaultKeys.add(
                new SingleFaultKey.Builder().setType(METHOD_AND_CODE).setMethod(method).setCode(code).build()
        );
        listOfFaultKeys.add(
                new SingleFaultKey.Builder().setType(METHOD).setMethod(method).build()
        );

        return listOfFaultKeys;
    }

    static class Builder {
        private FaultKeyType faultKeyType;
        private String method = "";
        private String code = "";
        private String request = "";

        @CanIgnoreReturnValue
        public Builder setType(FaultKeyType faultKeyType) {
            this.faultKeyType = faultKeyType;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder setRequest(String request) {
            this.request = request;
            return this;
        }

        public SingleFaultKey build() {
            return new SingleFaultKey(this);
        }
    }
}
