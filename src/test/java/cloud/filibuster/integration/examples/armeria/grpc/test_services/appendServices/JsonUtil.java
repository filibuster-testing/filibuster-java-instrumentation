package cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices;

import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


import java.lang.reflect.Type;

import java.util.logging.Logger;

public class JsonUtil {
    private static final Logger logger = Logger.getLogger(JsonUtil.class.getName());

    public static void writeMetaData(MetaDataContainer data, String key) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(data);
        RedisClientService.getInstance().redisClient.connect().sync().set(key, jsonString);
    }

    public static MetaDataContainer readMetaData(String key) {
        Gson gson = new Gson();
        Type type = new TypeToken<MetaDataContainer>() {}.getType();
        return gson.fromJson((RedisClientService.getInstance().redisClient.connect().sync().get(key)), type);
    }

}

