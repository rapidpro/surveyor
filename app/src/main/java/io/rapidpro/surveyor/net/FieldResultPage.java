package io.rapidpro.surveyor.net;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

import io.rapidpro.flows.runner.Field;

public class FieldResultPage {
    public int page;
    public String next;
    public String previous;
    public List<JsonElement> results;

    public List<Field> getRunnerFields() {
        List<Field> runnerFields = new ArrayList<>();
        for (JsonElement ele : results) {
            runnerFields.add(Field.fromJson(ele));
        }
        return runnerFields;
    }
}
