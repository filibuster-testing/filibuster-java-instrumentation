package cloud.filibuster.instrumentation.libraries.armeria.http;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.client.SimpleDecoratingHttpClient;
import com.linecorp.armeria.common.FilteredHttpResponse;
import com.linecorp.armeria.common.HttpObject;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;

public class FilibusterNullDecoratingHttpClient extends SimpleDecoratingHttpClient {
    public FilibusterNullDecoratingHttpClient(HttpClient delegate) {
        super(delegate);
    }

    @Override
    public HttpResponse execute(ClientRequestContext ctx, HttpRequest req) throws Exception {
        HttpResponse response = unwrap().execute(ctx, req);

        return new FilteredHttpResponse(response) {
            @Override
            @CanIgnoreReturnValue
            protected HttpObject filter(HttpObject obj) {
                return obj;
            }
        };
    }
}

