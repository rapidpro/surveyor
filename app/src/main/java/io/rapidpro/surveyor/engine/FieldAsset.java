package io.rapidpro.surveyor.engine;

import io.rapidpro.surveyor.net.responses.Field;

public class FieldAsset {
    private String key;
    private String name;
    private String type;

    public FieldAsset(String key, String name, String type) {
        this.key = key;
        this.name = name;
        this.type = type;
    }

    public static FieldAsset fromTemba(Field field) {
        String type = field.getValueType().equals("numeric") ? "number" : field.getValueType();
        return new FieldAsset(field.getKey(), field.getLabel(), type);
    }
}
