package io.rapidpro.surveyor.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class JsonUtils {
    private static Gson s_gson = new GsonBuilder()
            //.setPrettyPrinting()
            .registerTypeAdapter(RawJson.class, new RawJson.Adapter())
            .create();

    public static Gson getGson() {
        return s_gson;
    }

    public static String marshal(Object src) {
        return s_gson.toJson(src);
    }

    public static <T> T unmarshal(String json, Class<T> clazz) {
        return s_gson.fromJson(json, clazz);
    }

    public static <T> T unmarshal(String json, TypeToken type) {
        return s_gson.fromJson(json, type.getType());
    }
}
