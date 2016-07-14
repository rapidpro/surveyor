package io.rapidpro.surveyor.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.flows.utils.Jsonizable;

public class FlowDefinition implements Jsonizable {
    public String base_language;
    public JsonArray action_sets;
    public JsonArray rule_sets;
    public int version;
    public String flow_type;
    public String entry;

    public Metadata metadata;

    public static class Metadata {
        public int revision;
        public String name;
        public String contact_creation;
        public String uuid;
    }

    @Override
    public JsonElement toJson() {
        return JsonUtils.object(
                "base_language", base_language,
                "action_sets", action_sets,
                "rule_sets", rule_sets,
                "version", version,
                "flow_type", flow_type,
                "entry", entry,
                "metadata", JsonUtils.object(
                        "uuid", metadata.uuid,
                        "revision", metadata.revision,
                        "name", metadata.name,
                        "contact_creation", metadata.contact_creation)
        );
    }

    public String toString() {
        return toJson().toString();
    }
}
