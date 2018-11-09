package io.rapidpro.surveyor.net.responses;

import com.google.gson.annotations.SerializedName;

public class Field {
    private String key;
    private String name;

    public Field() {
    }

    public Field(String key, String name, String valueType) {
        this.key = key;
        this.name = name;
        this.valueType = valueType;
    }

    @SerializedName("value_type")
    private String valueType;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }
}
