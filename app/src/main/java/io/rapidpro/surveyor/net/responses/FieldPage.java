package io.rapidpro.surveyor.net.responses;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.rapidpro.flows.runner.Field;

public class FieldPage extends PaginatedResults<JsonObject> {

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
        List<Field> objs = new ArrayList<>();
        for (JsonObject obj : getResults()) {
            try {
                objs.add(unmarshalResult(obj));
            } catch (Throwable t) {
            }
        }
        return objs;
    }

    public Field unmarshalResult(JsonObject json) {
        return new Field(
                json.get("key").getAsString(),
                json.get("label").getAsString(),
                m_typeMapping.get(json.get("value_type").getAsString())
        );
    }
}
