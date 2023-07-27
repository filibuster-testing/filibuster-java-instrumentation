package algorithmjacob.jacobservices;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

public class JsonUtil {

    public static void writeMetaData(MetaDataContainer data, String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(data);

        try (FileWriter file = new FileWriter(filePath)) {
            file.write(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static MetaDataContainer readMetaData(String filePath) {
        Gson gson = new Gson();
        Type type = new TypeToken<MetaDataContainer>() {}.getType();
        try (Reader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, type);;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}

