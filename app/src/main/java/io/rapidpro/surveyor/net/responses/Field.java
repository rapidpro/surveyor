package io.rapidpro.surveyor.net.responses;

import com.google.gson.annotations.SerializedName;

public class Field {
    private String key;
    private String label;

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
