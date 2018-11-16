package io.rapidpro.surveyor.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import io.rapidpro.surveyor.utils.JsonUtils;
import io.rapidpro.surveyor.utils.RawJson;

public class Flow {
    private String uuid;

    private String name;

    private int revision;

    @SerializedName("question_count")
    private int questionCount;

    /**
     * Creates a new flow
     *
     * @param uuid          the flow UUID
     * @param name          the flow name
     * @param revision      the flow revision number
     * @param questionCount the number of questions
     */
    public Flow(String uuid, String name, int revision, int questionCount) {
        this.uuid = uuid;
        this.name = name;
        this.revision = revision;
        this.questionCount = questionCount;
    }

    /**
     * Extracts a flow summary from the given flow definition
     *
     * @param flow the flow definition
     * @return the flow summary
     */
    public static Flow extract(RawJson flow) {

        // TODO use streaming for performance https://developer.android.com/reference/android/util/JsonReader
        JsonObject definition = JsonUtils.unmarshal(flow.toString(), JsonObject.class);

        String uuid = definition.get("uuid").getAsString();
        String name = definition.get("name").getAsString();
        int revision = definition.get("revision").getAsInt();
        int questionCount = 0;

        JsonArray nodes = definition.get("nodes").getAsJsonArray();
        for (JsonElement elem : nodes) {
            JsonObject node = elem.getAsJsonObject();
            if (node.get("wait") != null) {
                questionCount++;
            }
        }

        return new Flow(uuid, name, revision, questionCount);
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

    public int getQuestionCount() {
        return questionCount;
    }
}
