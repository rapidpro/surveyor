package io.rapidpro.surveyor.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.threeten.bp.Instant;

import java.io.IOException;

import io.rapidpro.expressions.utils.ExpressionUtils;

/**
 * Adapter for Instant instances to serialize as ISO8601 in UTC, with millisecond precision,
 * e.g. "2014-10-03T01:41:12.790Z"
 */
public class InstantJSONAdapter extends TypeAdapter<Instant> {
    @Override
    public void write(JsonWriter out, Instant instant) throws IOException {
        if (instant != null) {
            out.value(ExpressionUtils.formatJsonDate(instant));
        } else {
            out.nullValue();
        }
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        return ExpressionUtils.parseJsonDate(in.nextString());
    }
}