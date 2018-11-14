package io.rapidpro.surveyor.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.rapidpro.surveyor.utils.JsonUtils;
import io.rapidpro.surveyor.utils.RawJson;

public class FlowSummary {
    private String uuid;
    private String name;
    private int revision;
    private int questions;

    /**
     * Creates a new flow summary
     * @param uuid the flow UUID
     * @param name the flow name
     * @param revision the flow revision number
     * @param questions the number of questions
     */
    public FlowSummary(String uuid, String name, int revision, int questions) {
        this.uuid = uuid;
        this.name = name;
        this.revision = revision;
        this.questions = questions;
    }

    /**
     * Extracts a flow summary from the given flow definition
     * @param flow the flow definition
     * @return the flow summary
     */
    public static FlowSummary extract(RawJson flow) {

        // TODO use streaming for performance https://developer.android.com/reference/android/util/JsonReader
        JsonObject definition = JsonUtils.unmarshal(flow.toString(), JsonObject.class);

        String uuid = definition.get("uuid").getAsString();
        String name = definition.get("name").getAsString();
        int revision = definition.get("revision").getAsInt();
        int questions = 0;

        JsonArray nodes = definition.get("nodes").getAsJsonArray();
        for (JsonElement elem : nodes) {
            JsonObject node = elem.getAsJsonObject();
            if (node.get("wait") != null) {
                questions++;
            }
        }

        return new FlowSummary(uuid, name, revision, questions);
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getRevision() {
        return revision;
    }

    public int getQuestions() {
        return questions;
    }
}
