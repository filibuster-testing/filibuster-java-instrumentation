package cloud.filibuster.instrumentation;

import cloud.filibuster.instrumentation.helpers.Networking;
import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;

import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.Post;
import com.linecorp.armeria.server.annotation.Put;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.ProducesJson;
import com.linecorp.armeria.server.annotation.ConsumesJson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fake server for Filibuster that allows programming against the server API without starting
 * the Python Filibuster server.
 */
public class FilibusterServer {
    private static final Logger logger = Logger.getLogger(FilibusterServer.class.getName());

    public static ArrayList<JSONObject> payloadsReceived = new ArrayList<>();

    public static boolean noNewTestExecution = false;

    public static boolean shouldInjectExceptionFault = false;

    public static boolean shouldInjectStatusCodeFault = false;

    public static boolean emptyExceptionString = false;

    public static boolean emptyExceptionCauseString = false;

    public static boolean shouldReturnNotFounds = false;

    public static boolean shouldInjectGrpcMetadataFault = false;

    public static boolean skipSleepKey = false;

    public static boolean shouldNotAbort = false;

    public static boolean grpcExceptionType = false;

    public static boolean oneNewTestExecution = true;

    public static HashMap<String, String> additionalExceptionMetadata = new HashMap<>();

    /**
     * Reset the list of received payloads at the fake server.
     */
    public static void resetPayloadsReceived() {
        payloadsReceived = new ArrayList<>();
    }

    /**
     * Reset any fault configurations at the fake server.
     */
    public static void resetAdditionalExceptionMetadata() {
        additionalExceptionMetadata = new HashMap<>();
    }

    private static String getExceptionString() {
        if (grpcExceptionType) {
            return "io.grpc.StatusRuntimeException";
        } else {
            // same for both WebClient and AsyncHttpClient since they both use Netty; nice!
            return "com.linecorp.armeria.client.UnprocessedRequestException";
        }
    }

    private static String getExceptionCauseString() {
        if (grpcExceptionType) {
            return "";
        } else {
            // same for both WebClient and AsyncHttpClient since they both use Netty; nice!
            return "io.netty.channel.ConnectTimeoutException";
        }
    }

    private static JSONObject getExceptionMetadata() {
        JSONObject metadata = new JSONObject();

        metadata.put("abort", !shouldNotAbort);

        if (!skipSleepKey) {
            metadata.put("sleep", 0);
        }

        if (!emptyExceptionCauseString) {
            metadata.put("cause", getExceptionCauseString());
        } else {
            metadata.put("cause", "");
        }

        for (Map.Entry<String,String> entry : additionalExceptionMetadata.entrySet()) {
            metadata.put(entry.getKey(), entry.getValue());
        }

        return metadata;
    }


    /**
     * Initialize the fake Filibuster server.
     *
     * @return instance of the server.
     * @throws IOException if the server could not be started.
     */
    public static Server serve() throws IOException {
        ServerBuilder sb = Server.builder();
        sb.http(Networking.getFilibusterPort());

        sb.annotatedService(new Object() {
            @Put("/filibuster/create")
            @ProducesJson
            @ConsumesJson
            public HttpResponse create(AggregatedHttpRequest request) {
                if (shouldReturnNotFounds) {
                    return HttpResponse.of(HttpStatus.NOT_FOUND);
                } else {
                    JSONObject payload = new JSONObject(request.contentUtf8());
                    payloadsReceived.add(payload);

                    JSONObject responseBody = new JSONObject();
                    responseBody.put("generated_id", 0);

                    if (shouldInjectExceptionFault) {
                        JSONObject exception = new JSONObject();

                        if (!emptyExceptionString) {
                            exception.put("name", getExceptionString());
                        } else {
                            exception.put("name", "");
                        }

                        exception.put("metadata", getExceptionMetadata());

                        responseBody.put("forced_exception", exception);
                    }

                    if (shouldInjectStatusCodeFault) {
                        JSONObject returnValue = new JSONObject();
                        returnValue.put("status_code", "404");

                        JSONObject metadata = new JSONObject();
                        metadata.put("return_value", returnValue);

                        responseBody.put("failure_metadata", metadata);
                    }

                    if (shouldInjectGrpcMetadataFault) {
                        JSONObject exceptionMetadata = new JSONObject();
                        exceptionMetadata.put("code", "NOT_FOUND");

                        JSONObject exception = new JSONObject();
                        exception.put("metadata", exceptionMetadata);

                        JSONObject metadata = new JSONObject();
                        metadata.put("exception", exception);

                        responseBody.put("failure_metadata", metadata);
                    }

                    logger.log(Level.INFO, "responseBody: " + responseBody);

                    logger.log(Level.INFO, "FILIBUSTER SERVER: returning create response!");

                    return HttpResponse.of(responseBody.toString());
                }
            }
        });

        sb.annotatedService(new Object() {
            @Post("/filibuster/update")
            @ProducesJson
            @ConsumesJson
            public HttpResponse update(AggregatedHttpRequest request) {
                if (shouldReturnNotFounds) {
                    return HttpResponse.of(HttpStatus.NOT_FOUND);
                } else {
                    JSONObject payload = new JSONObject(request.contentUtf8());
                    payloadsReceived.add(payload);

                    logger.log(Level.INFO, "FILIBUSTER SERVER: returning update response!");
                    if (shouldReturnNotFounds) {
                        return HttpResponse.of(HttpStatus.NOT_FOUND);
                    } else {
                        // If we receive an update where *our* execution index is someone else's preliminary, update
                        // *our* execution index and return the new execution index back in the payload.
                        //
                        String distributedExecutionIndex = payload.getString("execution_index");
                        String existingDistributedExecutionIndex = null;

                        for (JSONObject existingPayload : payloadsReceived) {
                            if (existingPayload.has("preliminary_execution_index")) {
                                if (existingPayload.getString("preliminary_execution_index").equals(distributedExecutionIndex)) {
                                    existingDistributedExecutionIndex = existingPayload.getString("execution_index");
                                    payload.put("execution_index", existingDistributedExecutionIndex);
                                }
                            }
                        }

                        JSONObject returnObject = new JSONObject();

                        if (existingDistributedExecutionIndex != null) {
                            returnObject.put("execution_index", existingDistributedExecutionIndex);
                        }

                        return HttpResponse.of(returnObject.toString());
                    }
                }
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/new-test-execution/{service_name}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse newTestExecution(AggregatedHttpRequest request) {
                JSONObject jsonObject = new JSONObject();

                if (oneNewTestExecution) {
                    jsonObject.put("new-test-execution", true);
                    oneNewTestExecution = false;
                    noNewTestExecution = true;
                } else {
                    jsonObject.put("new-test-execution", !noNewTestExecution);
                }
                logger.log(Level.INFO, "jsonObject: " + jsonObject);

                if (shouldReturnNotFounds) {
                    return HttpResponse.of(HttpStatus.NOT_FOUND);
                } else {
                    return HttpResponse.of(jsonObject.toString());
                }
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/has-next-iteration/{current_iteration}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse hasNextIteration(@Param("current_iteration") String currentIteration) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("has-next-iteration", Integer.parseInt(currentIteration) <= 3);
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Post("/filibuster/analysis-file")
            @ProducesJson
            @ConsumesJson
            public HttpResponse analysisFile() {
                JSONObject jsonObject = new JSONObject();
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/has-next-iteration/{current_iteration}/{caller}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse hasNextIteration(@Param("current_iteration") String currentIteration, @Param("caller") String caller) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("has-next-iteration", Integer.parseInt(currentIteration) <= 3);
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/teardowns-completed/{current_iteration}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse teardownsCompleted() {
                JSONObject jsonObject = new JSONObject();
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/terminate")
            @ProducesJson
            @ConsumesJson
            public HttpResponse terminate() {
                JSONObject jsonObject = new JSONObject();
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Post("/filibuster/complete-iteration/{current_iteration}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse completeIteration(@Param("current_iteration") String currentIteration) {
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        sb.annotatedService(new Object() {
            @Post("/filibuster/complete-iteration/{current_iteration}/exception/{exception_occurred}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse completeIteration(@Param("current_iteration") String currentIteration, @Param("exception_occurred") int exceptionOccurred) {
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/fault-injected")
            @ProducesJson
            @ConsumesJson
            public HttpResponse faultInjected(AggregatedHttpRequest request) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", wasFaultInjected());
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/fault-injected/service/{service_name}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse faultInjectedOnService(@Param("service_name") String name) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", wasFaultInjected());
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/fault-injected/method/{service_name}/{method_name}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse faultInjectedOnMethod(@Param("service_name") String serviceName, @Param("method_name") String methodName) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", wasFaultInjected());
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.service("/health-check", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "OK");
                return HttpResponse.of(jsonObject.toString());
            }
        });

        return sb.build();
    }

    private static boolean wasFaultInjected() {
        if (shouldInjectExceptionFault) {
            return true;
        }

        if (shouldInjectStatusCodeFault) {
            return true;
        }

        return shouldInjectGrpcMetadataFault;
    }

    /**
     * Initialize the fake Filibuster server.
     *
     * @param args not used.
     * @throws IOException if server could not be started.
     */
    @SuppressWarnings("VoidMissingNullable")
    public static void main(String[] args) throws IOException {
        Server server = serve();
        CompletableFuture<Void> future = server.start();
        future.join();
    }

}
