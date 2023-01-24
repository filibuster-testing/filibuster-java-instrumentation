package cloud.filibuster.junit.server.local_grpc;

import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.server.core.FilibusterCore;
import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.ConsumesJson;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.Post;
import com.linecorp.armeria.server.annotation.ProducesJson;
import com.linecorp.armeria.server.annotation.Put;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

@SuppressWarnings("Varifier")
public class FilibusterServer {
    public static Server serve(FilibusterCore filibusterCore) {
        ServerBuilder sb = Server.builder();
        sb.http(Networking.getFilibusterPort());

        // RPC hooks.

        sb.annotatedService(new Object() {
            @Put("/filibuster/create")
            @ProducesJson
            @ConsumesJson
            public HttpResponse create(AggregatedHttpRequest request) {
                JSONObject payload = new JSONObject(request.contentUtf8());
                JSONObject response = filibusterCore.beginInvocation(payload);
                return HttpResponse.of(response.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Post("/filibuster/update")
            @ProducesJson
            @ConsumesJson
            public HttpResponse update(AggregatedHttpRequest request) {
                JSONObject payload = new JSONObject(request.contentUtf8());
                JSONObject response = filibusterCore.endInvocation(payload);
                return HttpResponse.of(response.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/new-test-execution/{service_name}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse newTestExecution(@Param("service_name") String serviceName) {
                JSONObject response = new JSONObject();
                response.put("new-test-execution", filibusterCore.isNewTestExecution(serviceName));
                return HttpResponse.of(response.toString());
            }
        });

        // Configuration.

        sb.annotatedService(new Object() {
            @Post("/filibuster/analysis-file")
            @ProducesJson
            @ConsumesJson
            public HttpResponse analysisFile(AggregatedHttpRequest request) {
                JSONObject payload = new JSONObject(request.contentUtf8());
                filibusterCore.analysisFile(payload);
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        // JUnit hooks.

        sb.annotatedService(new Object() {
            @Get("/filibuster/has-next-iteration/{current_iteration}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse hasNextIteration(@Param("current_iteration") String currentIteration) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("has-next-iteration", filibusterCore.hasNextIteration(currentIteration));
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/has-next-iteration/{current_iteration}/{caller}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse hasNextIteration(@Param("current_iteration") String currentIteration, @Param("caller") String caller) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("has-next-iteration", filibusterCore.hasNextIteration(Integer.valueOf(currentIteration), caller));
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Post("/filibuster/complete-iteration/{current_iteration}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse completeIteration(@Param("current_iteration") String currentIteration) {
                filibusterCore.completeIteration(currentIteration);
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        sb.annotatedService(new Object() {
            @Post("/filibuster/complete-iteration/{current_iteration}/exception/{exception_occurred}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse completeIteration(@Param("current_iteration") String currentIteration, @Param("exception_occurred") int exceptionOccurred) {
                filibusterCore.completeIteration(currentIteration, exceptionOccurred);
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/teardowns-completed/{current_iteration}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse teardownsCompleted(@Param("current_iteration") String currentIteration) {
                filibusterCore.teardownsCompleted(currentIteration);
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/terminate")
            @ProducesJson
            @ConsumesJson
            public HttpResponse terminate() {
                filibusterCore.terminateFilibuster();
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        // Fault injection helpers.

        sb.annotatedService(new Object() {
            @Get("/filibuster/fault-injected")
            @ProducesJson
            @ConsumesJson
            public HttpResponse faultInjected(AggregatedHttpRequest request) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", filibusterCore.wasFaultInjected());
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/fault-injected/service/{service_name}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse faultInjectedOnService(@Param("service_name") String serviceName) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", filibusterCore.wasFaultInjectedOnService(serviceName));
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.annotatedService(new Object() {
            @Get("/filibuster/fault-injected/method/{service_name}/{method_name}")
            @ProducesJson
            @ConsumesJson
            public HttpResponse faultInjectedOnMethod(@Param("service_name") String serviceName, @Param("method_name") String methodName) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("result", filibusterCore.wasFaultInjectedOnMethod(serviceName, methodName));
                return HttpResponse.of(jsonObject.toString());
            }
        });

        // Health check.

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
}
