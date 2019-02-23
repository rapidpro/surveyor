package io.rapidpro.surveyor.legacy;

import android.net.Uri;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.threeten.bp.Instant;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.rapidpro.expressions.utils.ExpressionUtils;
import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.definition.RuleSet;
import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.runner.Field;
import io.rapidpro.flows.runner.Step;
import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.flows.utils.Jsonizable;
import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.net.TembaException;
import io.rapidpro.surveyor.net.TembaService;

/**
 * A legacy submission
 */
public class Submission implements Jsonizable {

    private transient static final String FLOW_FILE = "flow.json";
    private transient static final String MEDIA_DIR = "media";
    private transient static final String CURRENT_FILE = "current.json";

    private static final List<String> MSG_RESOLVED = Arrays.asList("text", "geo");
    public static FilenameFilter SUBMISSION_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return !filename.endsWith(FLOW_FILE)
                    && !filename.equals(MEDIA_DIR)
                    && !filename.endsWith(CURRENT_FILE);
        }
    };
    // fields created during this submission
    protected HashMap<String, Field> m_fields = new HashMap<>();
    protected List<Step> m_steps;
    // the files we will be persisted in
    private transient File m_currentFile;
    private transient File m_completedFile;
    // flow uuid
    private String m_flow;
    // our contact participating in the flow
    private Contact m_contact;
    // when the flow run started
    private Instant m_started;
    private int m_revision;
    // if the flow was completed
    private boolean m_completed;
    // the username submitting this run
    private String m_username;
    // the app version when this submission started
    private String m_appVersion;

    public Submission() {
    }

    private static JsonObject migrateFlowToVersion9(String flowUUID, JsonObject root) {

        if (root.has("flows")) {
            return root;
        }

        root.get("metadata").getAsJsonObject().addProperty("uuid", flowUUID);
        JsonArray flowArray = new JsonArray();
        flowArray.add(root);
        JsonObject newRoot = new JsonObject();
        newRoot.add("flows", flowArray);

        return newRoot;
    }

    /**
     * Read the flow definition from disk
     */
    private static Map<String, Flow> getFlows(File file) {
        String revision = file.getName().split("_")[0];
        File flowFile = new File(file.getParent(), revision + "_" + FLOW_FILE);
        Logger.d("Reading flow: " + flowFile.getName());
        String flowString = null;
        try {
            flowString = FileUtils.readFileToString(flowFile);
        } catch (IOException e) {
            Logger.e("Error loading flow", e);
        }

        String flowUUID = file.getParentFile().getName();
        JsonParser parser = new JsonParser();

        JsonObject root = parser.parse(flowString).getAsJsonObject();
        root = migrateFlowToVersion9(flowUUID, root);

        Map<String, Flow> flows = new HashMap<>();

        for (JsonElement flowElement : root.get("flows").getAsJsonArray()) {
            Flow flow = Flow.fromJson(flowElement.toString());
            flows.put(flow.getUuid(), flow);
        }

        return flows;
    }

    /**
     * Adds UUIDs where necessary to migrate a flow definition forward
     */
    private static JsonObject migrateToVersion9(JsonObject root) {
        JsonArray steps = root.get("steps").getAsJsonArray();
        String flowUUID = root.get("flow").getAsString();

        for (JsonElement ele : steps) {
            JsonObject step = ele.getAsJsonObject();
            if (!step.has("flow_uuid")) {
                step.addProperty("flow_uuid", flowUUID);
            }

            JsonElement stepEle = step.get("rule");
            if (!stepEle.isJsonNull()) {
                JsonObject rule = stepEle.getAsJsonObject();
                if (!rule.has("flow_uuid")) {
                    rule.addProperty("flow_uuid", flowUUID);
                }
            }
        }
        return root;
    }

    public static JsonObject migrateSubmission(JsonObject root) {
        return migrateToVersion9(root);
    }

    /**
     * Loads a submission from a file. Assumes the flow file is present in
     * the parent directory.
     */
    public static Submission load(String username, File file) throws IOException {
        String json = FileUtils.readFileToString(file);
        JsonParser parser = new JsonParser();
        JsonObject root = migrateSubmission(parser.parse(json).getAsJsonObject());
        Flow.DeserializationContext context = new Flow.DeserializationContext(getFlows(file));

        Submission submission = fromJson(root, context);

        if (submission.m_username == null) {
            submission.m_username = username;
        }

        submission.m_currentFile = file;
        submission.m_completedFile = file;
        return submission;
    }

    public static Submission fromJson(JsonElement ele, Flow.DeserializationContext context) {
        JsonObject obj = (JsonObject) ele;
        Submission submission = new Submission();
        submission.m_steps = JsonUtils.fromJsonArray(obj.get("steps").getAsJsonArray(), context, Step.class);
        submission.m_contact = JsonUtils.fromJson(obj.get("contact"), null, Contact.class);
        submission.m_started = ExpressionUtils.parseJsonDate(obj.get("started").getAsString());

        if (obj.has("submitted_by")) {
            submission.m_username = obj.get("submitted_by").getAsString();
        }

        if (obj.has("revision")) {
            submission.m_revision = obj.get("revision").getAsInt();
        } else if (obj.has("version")) {
            submission.m_revision = obj.get("version").getAsInt();
        }

        if (obj.has("app_version")) {
            JsonElement version = obj.get("app_version");
            if (!version.isJsonNull()) {
                submission.m_appVersion = version.getAsString();
            }
        }

        submission.m_completed = obj.get("completed").getAsBoolean();
        submission.m_flow = obj.get("flow").getAsString();
        return submission;
    }

    /**
     * Serializes this run state to JSON
     *
     * @return the JSON
     */
    @Override
    public JsonElement toJson() {
        return JsonUtils.object(
                "fields", JsonUtils.toJsonArray(m_fields.values()),
                "steps", JsonUtils.toJsonArray(m_steps),
                "flow", m_flow,
                "contact", m_contact.toJson(),
                "started", ExpressionUtils.formatJsonDate(m_started),
                "revision", m_revision,
                "completed", m_completed,
                "app_version", m_appVersion,
                "submitted_by", m_username
        );
    }

    public List<JsonObject> getResultsJson() {
        Map<Flow, List<Step>> resultsMap = getResultsMap();
        List<JsonObject> resultsJson = new ArrayList<>();
        for (Map.Entry entry : resultsMap.entrySet()) {

            List<Step> steps = (List<Step>) entry.getValue();
            Flow flow = (Flow) entry.getKey();
            Instant started = steps.get(0).getArrivedOn();

            if (steps.size() > 0) {
                resultsJson.add(JsonUtils.object(
                        "fields", JsonUtils.toJsonArray(m_fields.values()),
                        "steps", JsonUtils.toJsonArray(steps),
                        "flow", flow.getUuid(),
                        "contact", m_contact.getUuid(),
                        "started", ExpressionUtils.formatJsonDate(started),
                        "revision", flow.getMetadata().get("revision").getAsInt(),
                        "completed", m_completed,
                        "app_version", m_appVersion,
                        "submitted_by", m_username
                ));
            }
        }

        return resultsJson;
    }

    private Map<Flow, List<Step>> getResultsMap() {
        // build up a map for each flow to it's steps
        Map<Flow, List<Step>> resultMap = new HashMap<>();
        for (Step step : m_steps) {
            List<Step> steps = resultMap.get(step.getFlow());
            if (steps == null) {
                steps = new ArrayList<>();
            }
            steps.add(step);
            resultMap.put(step.getFlow(), steps);
        }

        return resultMap;
    }

    private File getUniqueFile(File dir, String name, String ext) {
        File file = new File(dir, name + "." + ext);
        int count = 2;
        while (file.exists()) {
            file = new File(dir, name + "_" + count + "." + ext);
            count++;
        }
        return file;
    }

    public File getCompletedFile() {
        return m_completedFile;
    }

    /**
     * Get all the results which contain an pointer to a local file
     */
    private List<RuleSet.Result> getLocalFileResults() {
        List<RuleSet.Result> localResults = new ArrayList<RuleSet.Result>();
        // resolve the media for all of our steps
        for (Step step : m_steps) {
            RuleSet.Result result = step.getRuleResult();
            if (result != null) {
                String media = result.getMedia();
                if (media != null) {
                    int split = media.indexOf(":");
                    String type = media.substring(0, split);
                    String fileUrl = media.substring(split + 1, media.length());
                    // don't attempt resolved types
                    if (!MSG_RESOLVED.contains(type) && fileUrl.startsWith("content:")) {
                        localResults.add(result);
                    }
                }
            }
        }
        return localResults;
    }

    /**
     * Submits all local media and updates with remote urls
     */
    private void resolveMedia(String token) throws TembaException {

        final TembaService rapid = SurveyorApplication.get().getTembaService();

        List<RuleSet.Result> results = getLocalFileResults();

        // if we have local files to upload, determine our flow run
        if (results.size() > 0) {
            for (RuleSet.Result result : results) {
                String media = result.getMedia();
                if (media != null) {
                    Logger.d("Resolving media result: " + media);
                    int split = media.indexOf(":");

                    String type = media.substring(0, split);
                    String fileUrl = media.substring(split + 1, media.length());
                    String extension = FilenameUtils.getExtension(fileUrl);

                    Uri mediaUri = Uri.parse(fileUrl);

                    String newUrl = rapid.uploadMedia(token, mediaUri);
                    result.setMedia(type + ":" + newUrl);
                }
            }
        }
    }

    public void submit(String token) throws TembaException {
        final TembaService rapid = SurveyorApplication.get().getTembaService();
        final Submission submission = this;

        // submit any created fields
        rapid.legacyAddCreatedFields(token, m_fields);

        // first we need to create our contact
        Logger.d(m_contact.toJson().toString());
        rapid.legacyAddContact(token, m_contact);

        // then post the results
        submission.resolveMedia(token);
        rapid.legacyAddResults(token, submission);

    }

    public void delete() {
        if (m_currentFile != null) {

            // delete ourselves
            FileUtils.deleteQuietly(m_currentFile);

            // and our associated media files
            deleteMediaFiles();
        }
    }

    public File getMediaDir() {
        File submissionDir = m_currentFile.getParentFile();
        File mediaDir = new File(submissionDir, "media");
        mediaDir.mkdirs();
        return mediaDir;

    }

    /**
     * Get the prefix associated with this submission
     *
     * @return
     */
    public String getPrefix() {
        // our prefix should be the filename up to the '.json' extension
        String filename = m_completedFile.getName();
        return filename.substring(0, filename.length() - 5);
    }

    private void deleteMediaFiles() {
        final String prefix = getPrefix();
        File[] mediaFiles = getMediaDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(prefix);
            }
        });

        for (File mediaFile : mediaFiles) {
            FileUtils.deleteQuietly(mediaFile);
        }
    }
}