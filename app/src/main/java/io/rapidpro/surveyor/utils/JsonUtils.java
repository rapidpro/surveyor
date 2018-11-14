package io.rapidpro.surveyor.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtils {
    private static Gson s_gson = new GsonBuilder().create();

    public static String marshal(Object src) {
        return s_gson.toJson(src);
    }

    public static <T> T unmarshal(String json, Class<T> clazz) {
        return s_gson.fromJson(json, clazz);
    }
}
