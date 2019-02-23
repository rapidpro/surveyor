package io.rapidpro.surveyor.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Custom data type for fields which hold raw JSON
 */
public class RawJson {
    private String data;

    public RawJson(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return this.data;
    }

    /**
     * Type adapter to tell Gson how to use fields of this type
     */
    public static class Adapter extends TypeAdapter<RawJson> {
        @Override
        public void write(JsonWriter out, RawJson value) throws IOException {
            out.jsonValue(value.toString());
        }

        @Override
        public RawJson read(JsonReader in) throws IOException {
            // TODO ideally this wouldn't parse the entire tree into memory
            // see https://github.com/google/gson/issues/1368
            JsonElement parsed = new JsonParser().parse(in);

            return new RawJson(parsed.toString());
        }
    }
}
