package algorithmjacob.jacobservices;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonUtil {
    private static final Logger logger = Logger.getLogger(JsonUtil.class.getName());

    public static void writeMetaData(MetaDataContainer data, String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(data);

        try (FileWriter file = new FileWriter(filePath)) {
            file.write(jsonString);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.toString());
        }
    }

    public static MetaDataContainer readMetaData(String filePath) {
        Gson gson = new Gson();
        Type type = new TypeToken<MetaDataContainer>() {}.getType();
        try (Reader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.toString());
        }
        return null;
    }

}

