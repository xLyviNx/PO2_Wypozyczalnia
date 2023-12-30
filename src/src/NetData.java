package src;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class NetData {
    OperationType operationType;
    Operation operation;
    ArrayList<String> Strings = new ArrayList<String>();
    ArrayList<Integer> Integers = new ArrayList<Integer>();
    ArrayList<Boolean> Booleans = new ArrayList<Boolean>();
    ArrayList<Byte[]> Images = new ArrayList<>();

    public NetData(Operation op, String str) {
        operation = op;
        Strings.add(str);
        System.out.println(toJSON());
    }

    public NetData(Operation op) {
        operation = op;
        System.out.println(toJSON());
    }

    public String toJSON() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Operation.class, new EnumSerializer())
                .registerTypeAdapter(OperationType.class, new EnumSerializer())
                .create();
        return gson.toJson(this);
    }

    public static NetData fromJSON(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Operation.class, new EnumDeserializer<>(Operation.class))
                .registerTypeAdapter(OperationType.class, new EnumDeserializer<>(OperationType.class))
                .create();
        return gson.fromJson(json, NetData.class);
    }

    public static enum Operation {
        Unspecified,
        Register,
        Login,
    }

    public static enum OperationType {
        Unspecified,
        Success,
        Error,
        MessageBox
    }

    private static class EnumSerializer implements JsonSerializer<Enum<?>> {
        @Override
        public JsonElement serialize(Enum<?> src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", src.name());
            return obj;
        }
    }

    private static class EnumDeserializer<T extends Enum<T>> implements JsonDeserializer<T> {
        private final Class<T> enumClass;

        public EnumDeserializer(Class<T> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String name = json.getAsJsonObject().get("name").getAsString();
            return Enum.valueOf(enumClass, name);
        }
    }
}
