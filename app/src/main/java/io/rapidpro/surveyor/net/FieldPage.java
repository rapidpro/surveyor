package io.rapidpro.surveyor.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.rapidpro.flows.runner.Field;

public class FieldPage extends PaginatedResults<JsonElement> {

    private static  Map<String, Field.ValueType> m_typeMapping = new HashMap<>();
    static {
        m_typeMapping.put("text", Field.ValueType.TEXT);
        m_typeMapping.put("numeric", Field.ValueType.DECIMAL);
        m_typeMapping.put("datetime", Field.ValueType.DATETIME);
        m_typeMapping.put("state", Field.ValueType.STATE);
        m_typeMapping.put("district", Field.ValueType.DISTRICT);
        m_typeMapping.put("ward", Field.ValueType.WARD);
    }

    public List<Field> toRunnerFields() {
        List<Field> runnerFields = new ArrayList<>();
        for (JsonElement ele : getResults()) {
            try {
                JsonObject obj = ele.getAsJsonObject();
                Field field = new Field(
                        obj.get("key").getAsString(),
                        obj.get("label").getAsString(),
                        m_typeMapping.get(obj.get("value_type").getAsString())
                );
                runnerFields.add(field);
            } catch (Throwable t) {
                // if we fail to create the field, continue
            }
        }
        return runnerFields;
    }
}
