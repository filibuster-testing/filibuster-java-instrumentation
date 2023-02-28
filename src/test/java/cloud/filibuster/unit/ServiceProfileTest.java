package cloud.filibuster.unit;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.junit.server.core.profiles.ServiceProfile;
import io.grpc.Status;
import io.grpc.Status.Code;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class ServiceProfileTest {
    @Test
    public void testServiceProfileSerialization() {
        ServiceProfile serviceProfile = new ServiceProfile();

        serviceProfile.addToProfile(
                "cloud.filibuster.examples.Hello/PartialHello",
                Hello.HelloRequest.newBuilder().setName("Alice").build(),
                Status.fromCode(Code.OK),
                Hello.HelloReply.newBuilder().setMessage("Hi, Alice!").build()
        );

        serviceProfile.addToProfile(
                "cloud.filibuster.examples.Hello/PartialHello",
                Hello.HelloRequest.newBuilder().setName("Bob").build(),
                Status.fromCode(Code.FAILED_PRECONDITION),
                null
        );

        serviceProfile.addToProfile(
                "cloud.filibuster.examples.Hello/PartialHello",
                Hello.HelloRequest.newBuilder().setName("Chris").build(),
                Status.fromCode(Code.OK),
                Hello.HelloReply.newBuilder().setMessage("Hi, Chris!").build()
        );

        JSONObject serviceProfileJSONObject = serviceProfile.toJSONObject();

        ServiceProfile newServiceProfile = ServiceProfile.fromJSONObject(serviceProfileJSONObject);
        JSONObject newServiceProfileJSONObject = newServiceProfile.toJSONObject();
        assertEquals(true, serviceProfileJSONObject.similar(newServiceProfileJSONObject));

        assertEquals(serviceProfile, newServiceProfile);
    }
}
