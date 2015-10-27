package io.rapidpro.surveyor.net;

import com.google.gson.JsonArray;

public class FlowDefinition {
    public String base_language;
    public JsonArray action_sets;
    public JsonArray rule_sets;
    public String version;
    public String flow_type;
    public String entry;

    public Metadata metadata;

    public static class Metadata {
        public int revision;
        public String name;
    }
}
