package io.rapidpro.surveyor.net.responses;

import com.google.gson.annotations.SerializedName;

public class Field {
    private String key;
    private String label;

    public Field() {
    }

    public Field(String key, String label, String valueType) {
        this.key = key;
        this.label = label;
        this.valueType = valueType;
    }

    @SerializedName("value_type")
    private String valueType;

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getValueType() {
        return valueType;
    }
}
