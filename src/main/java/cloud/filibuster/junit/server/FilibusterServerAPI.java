package cloud.filibuster.junit.server;

import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.helpers.Response;
import cloud.filibuster.junit.exceptions.FilibusterServerBadResponseException;
import cloud.filibuster.junit.server.core.FilibusterCore;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;

@SuppressWarnings("Varifier")
public class FilibusterServerAPI {
    public static boolean healthCheck(WebClient webClient) throws ExecutionException, InterruptedException {
        CompletableFuture<Boolean> healthCheck = CompletableFuture.supplyAsync(() -> {
            RequestHeaders getJson = RequestHeaders.of(
                    HttpMethod.GET,
                    "/health-check",
                    HttpHeaderNames.ACCEPT,
                    "application/json",
                    "X-Filibuster-Instrumentation",
                    "true");
            AggregatedHttpResponse response = webClient.execute(getJson).aggregate().join();
            ResponseHeaders headers = response.headers();
            String statusCode = headers.get(HttpHeaderNames.STATUS);

            if (statusCode == null) {
                FilibusterServerBadResponseException.logAndThrow("healthCheck, statusCode: null");
                return false;
            }

            if (!Objects.equals(statusCode, "200")) {
                FilibusterServerBadResponseException.logAndThrow("healthCheck, statusCode: " + statusCode);
                return false;
            }

            if (statusCode.equals("200")) {
                return true;
            }

            return false;
        }, FilibusterExecutor.getExecutorService());

        return healthCheck.get();
    }

    public static void analysisFile(WebClient webClient, JSONObject jsonAnalysisConfiguration) throws ExecutionException, InterruptedException {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            FilibusterCore.getCurrentInstance().analysisFile(jsonAnalysisConfiguration);
        } else {
            CompletableFuture<Void> analysisFileFuture = CompletableFuture.supplyAsync(() -> {
                RequestHeaders postJson = RequestHeaders.of(
                        HttpMethod.POST,
                        "/filibuster/analysis-file",
                        HttpHeaderNames.CONTENT_TYPE,
                        "application/json",
                        "X-Filibuster-Instrumentation",
                        "true");
                AggregatedHttpResponse response = webClient.execute(postJson, jsonAnalysisConfiguration.toString()).aggregate().join();
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode == null) {
                    FilibusterServerBadResponseException.logAndThrow("analysisFile, statusCode: null");
                }

                if (!Objects.equals(statusCode, "200")) {
                    FilibusterServerBadResponseException.logAndThrow("analysisFile, statusCode: " + statusCode);
                }

                return null;
            }, FilibusterExecutor.getExecutorService());

            analysisFileFuture.get();
        }
    }

    public static void terminate(WebClient webClient) throws ExecutionException, InterruptedException {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            FilibusterCore.getCurrentInstance().terminateFilibuster();
        } else {
            CompletableFuture<Void> terminateFuture = CompletableFuture.supplyAsync(() -> {
                RequestHeaders getJson = RequestHeaders.of(
                        HttpMethod.GET,
                        "/filibuster/terminate",
                        HttpHeaderNames.CONTENT_TYPE,
                        "application/json",
                        "X-Filibuster-Instrumentation",
                        "true");
                AggregatedHttpResponse response = webClient.execute(getJson).aggregate().join();
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode == null) {
                    FilibusterServerBadResponseException.logAndThrow("terminate, statusCode: null");
                }

                if (!Objects.equals(statusCode, "200")) {
                    FilibusterServerBadResponseException.logAndThrow("terminate, statusCode: " + statusCode);
                }

                return null;
            }, FilibusterExecutor.getExecutorService());

            terminateFuture.get();
        }
    }

    public static void teardownsCompleted(WebClient webClient, int currentIteration) throws ExecutionException, InterruptedException {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            FilibusterCore.getCurrentInstance().teardownsCompleted(currentIteration);
        } else {
            CompletableFuture<Void> teardownsCompletedFuture = CompletableFuture.supplyAsync(() -> {
                RequestHeaders getJson = RequestHeaders.of(
                        HttpMethod.GET,
                        "/filibuster/teardowns-completed/" + currentIteration,
                        HttpHeaderNames.CONTENT_TYPE,
                        "application/json",
                        "X-Filibuster-Instrumentation",
                        "true");
                AggregatedHttpResponse response = webClient.execute(getJson).aggregate().join();
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode == null) {
                    FilibusterServerBadResponseException.logAndThrow("teardownsCompleted, statusCode: null");
                }

                if (!Objects.equals(statusCode, "200")) {
                    FilibusterServerBadResponseException.logAndThrow("teardownsCompleted, statusCode: " + statusCode);
                }

                return null;
            }, FilibusterExecutor.getExecutorService());

            teardownsCompletedFuture.get();
        }
    }

    public static void recordIterationComplete(WebClient webClient, int currentIteration, boolean exceptionOccurred) throws ExecutionException, InterruptedException {
        int exceptionOccurredInt;

        if (exceptionOccurred) {
            exceptionOccurredInt = 1;
        } else {
            exceptionOccurredInt = 0;
        }

        if (getServerBackendCanInvokeDirectlyProperty()) {
            FilibusterCore.getCurrentInstance().completeIteration(currentIteration, exceptionOccurredInt);
        } else {
            CompletableFuture<Void> updateFuture = CompletableFuture.supplyAsync(() -> {
                RequestHeaders postJson = RequestHeaders.of(
                        HttpMethod.POST,
                        "/filibuster/complete-iteration/" + currentIteration + "/exception/" + exceptionOccurredInt,
                        HttpHeaderNames.CONTENT_TYPE,
                        "application/json",
                        "X-Filibuster-Instrumentation",
                        "true");
                AggregatedHttpResponse response = webClient.execute(postJson).aggregate().join();
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode == null) {
                    FilibusterServerBadResponseException.logAndThrow("recordIterationComplete, statusCode: null");
                }

                if (!Objects.equals(statusCode, "200")) {
                    FilibusterServerBadResponseException.logAndThrow("recordIterationComplete, statusCode: " + statusCode);
                }

                return null;
            }, FilibusterExecutor.getExecutorService());

            updateFuture.get();
        }
    }

    public static boolean hasNextIteration(WebClient webClient, int currentIteration, String caller) throws ExecutionException, InterruptedException {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            return FilibusterCore.getCurrentInstance().hasNextIteration(currentIteration, caller);
        } else {
            CompletableFuture<Boolean> hasNextIteration = CompletableFuture.supplyAsync(() -> {
                RequestHeaders getJson = RequestHeaders.of(
                        HttpMethod.GET,
                        "/filibuster/has-next-iteration/" + currentIteration + "/" + caller,
                        HttpHeaderNames.ACCEPT,
                        "application/json",
                        "X-Filibuster-Instrumentation",
                        "true");
                AggregatedHttpResponse response = webClient.execute(getJson).aggregate().join();
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode == null) {
                    FilibusterServerBadResponseException.logAndThrow("hasNextIteration, statusCode: null");
                }

                if (!Objects.equals(statusCode, "200")) {
                    FilibusterServerBadResponseException.logAndThrow("hasNextIteration, statusCode: " + statusCode);
                }

                JSONObject jsonObject = Response.aggregatedHttpResponseToJsonObject(response);
                return jsonObject.getBoolean("has-next-iteration");
            }, FilibusterExecutor.getExecutorService());

            return hasNextIteration.get();
        }
    }
}
