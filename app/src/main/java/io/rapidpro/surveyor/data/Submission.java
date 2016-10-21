package io.rapidpro.surveyor.data;

import android.net.Uri;
import android.os.Environment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FileUtils;
import org.threeten.bp.Instant;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.rapidpro.expressions.utils.ExpressionUtils;
import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.definition.RuleSet;
import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.runner.Field;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Step;
import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.flows.utils.Jsonizable;
import io.rapidpro.surveyor.BuildConfig;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.activity.FlowRunActivity;
import io.rapidpro.surveyor.net.TembaService;

/**
 * Represents a single flow run. Manages saving run progress and
 * submission to the server. Submissions are stored on the file
 * system to be tolerant of database changes in the future.
 */
public class Submission implements Jsonizable {

    private transient static final String SUBMISSIONS_DIR = "submissions";
    private transient static final String FLOW_FILE = "flow.json";
    private transient static final String MEDIA_DIR = "media";
    private transient static final String CURRENT_FILE = "current.json";

    // the files we will be persisted in
    private transient File m_currentFile;
    private transient File m_completedFile;

    // fields created during this submission
    protected HashMap<String,Field> m_fields = new HashMap<>();

    protected List<Step> m_steps;

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

    private static FilenameFilter SUBMISSION_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return !filename.endsWith(FLOW_FILE)
                    && !filename.equals(MEDIA_DIR)
                    && !filename.endsWith(CURRENT_FILE);
        }
    };

    public Submission() {}

    /**
     * Clear out all submissions for all flows
     */
    public static void clear() {
        FileUtils.deleteQuietly(getSubmissionsDir());
    }

    /**
     * The root submission directory
     */
    private static File getSubmissionsDir() {
        File runsDir = new File(Environment.getExternalStorageDirectory(), "Surveyor");
        runsDir = new File(runsDir, SUBMISSIONS_DIR);
        runsDir.mkdirs();
        return runsDir;
    }

    /**
     * The submission directory for the given flow
     */
    private static File getFlowDir(int orgId, String flowUuid) {
        File flowDir = new File(getOrgDir(orgId), flowUuid);
        flowDir.mkdirs();
        return flowDir;
    }

    private static File getOrgDir(int orgId) {
        File orgDir = new File(getSubmissionsDir(), orgId + "");
        orgDir.mkdirs();
        return orgDir;
    }

    /**
     * Get the number of pending submissions for this flow
     */
    public static int getPendingSubmissionCount(DBFlow flow) {
        return Submission.getPendingSubmissions(flow).length;
    }

    /**
     * Get all the submission files for the given flow
     */
    public static File[] getPendingSubmissions(DBFlow flow) {
        return getFlowDir(flow.getOrg().getId(), flow.getUuid()).listFiles(SUBMISSION_FILTER);
    }

    /**
     * Get all submission files across all flows
     */
    public static File[] getPendingSubmissions(int orgId) {

        long start = System.currentTimeMillis();
        List<File> files = new ArrayList<>();

        for (File dir : getOrgDir(orgId).listFiles()) {
            if (dir.isDirectory()) {
                for (File submission : dir.listFiles(SUBMISSION_FILTER)) {
                    files.add(submission);
                }
            }
        }

        File[] results = new File[files.size()];
        results = files.toArray(results);
        Surveyor.get().LOG.d("Fetched all submissions. Took: " + (System.currentTimeMillis() - start) + "ms");
        return results;
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
    private static Map<String,Flow> getFlows(File file) {
        String revision = file.getName().split("_")[0];
        File flowFile = new File(file.getParent(), revision + "_" + FLOW_FILE);
        Surveyor.LOG.d("Reading flow: " + flowFile.getName());
        String flowString = null;
        try {
            flowString = FileUtils.readFileToString(flowFile);
        } catch (IOException e) {
            Surveyor.LOG.e("Error loading flow", e);
        }

        String flowUUID = file.getParentFile().getName();
        JsonParser parser = new JsonParser();

        JsonObject root = parser.parse(flowString).getAsJsonObject();
        root = migrateFlowToVersion9(flowUUID, root);

        Map<String,Flow> flows = new HashMap<>();

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
    public static Submission load(String username, File file) {
        try {

            String json = FileUtils.readFileToString(file);
            JsonParser parser = new JsonParser();
            JsonObject root = migrateSubmission(parser.parse(json).getAsJsonObject());
            Flow.DeserializationContext context = new Flow.DeserializationContext(getFlows(file));

            Surveyor.LOG.d(" << " + root.toString());
            Submission submission = JsonUtils.fromJson(root, context, Submission.class);

            if (submission.m_username == null) {
                submission.m_username = username;
            }

            submission.m_currentFile = file;
            submission.m_completedFile = file;
            return submission;
        } catch (IOException e) {
            // we'll return null
            Surveyor.LOG.e("Failure reading submission", e);
        }
        return null;
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


    private Map<Flow,List<Step>> getResultsMap() {
        // build up a map for each flow to it's steps
        Map<Flow,List<Step>> resultMap = new HashMap<>();
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
        File file =  new File(dir, name + "." + ext);
        int count = 2;
        while (file.exists()) {
            file =  new File(dir, name + "_" + count + "." + ext);
            count++;
        }
        return file;
    }

    /**
     * Create a new submission for a flow
     */
    public Submission(String username, DBFlow flow, String revision) {

        m_username = username;
        m_flow = flow.getUuid();
        m_contact = new Contact();
        m_revision = flow.getRevision();
        m_appVersion = BuildConfig.VERSION_NAME;

        // get a unique filename for our submission
        File flowDir = getFlowDir(flow.getOrg().getId(), flow.getUuid());
        m_completedFile = getUniqueFile(flowDir, revision + "_" + UUID.randomUUID().toString(), "json");

        // start with an empty submission file
        m_currentFile = new File(flowDir, CURRENT_FILE);
        if (m_currentFile.exists()) {
            m_currentFile.delete();
        }

        // write our flow definition if it isn't there yet
        File flowFile = new File(flowDir, revision + "_" + FLOW_FILE);
        if (!flowFile.exists()) {
            try {
                FileUtils.writeStringToFile(flowFile, flow.getDefinition());
            } catch (Exception e) {
                Surveyor.LOG.e("Failed to write flow to disk", e);
                // TODO: this should probably fail hard
            }
        }
    }

    public File getCompletedFile() {
        return m_completedFile;
    }

    public void addSteps(RunState runState) {

        m_contact = runState.getContact();

        if (m_steps == null) {
            m_steps = new ArrayList<>();
        }
        m_steps.addAll(runState.getCompletedSteps());

        // keep track of when we were started
        m_started = runState.getStarted();

        // mark us completed if necessary
        m_completed = runState.getState() == RunState.State.COMPLETED;

        // save off our current set of created fields
        for (Field field : runState.getCreatedFields()) {
            if (field.isNew()) {
                m_fields.put(field.getKey(), field);
            }
        }
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
                    if (!FlowRunActivity.MSG_RESOLVED.contains(type) && fileUrl.startsWith("file:")) {
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
    private void resolveMedia() {

        final TembaService rapid = Surveyor.get().getRapidProService();

        List<RuleSet.Result> results = getLocalFileResults();

        // if we have local files to upload, determine our flow run
        if (results.size() > 0) {
            for (RuleSet.Result result : results) {
                String media = result.getMedia();
                if (media != null) {
                    int split = media.indexOf(":");

                    String type = media.substring(0, split);
                    String fileUrl = media.substring(split + 1, media.length());

                    String extension = fileUrl.substring(fileUrl.lastIndexOf("."));
                    String newUrl = rapid.uploadMedia(new File(Uri.parse(fileUrl).getPath()), extension);
                    result.setMedia(type + ":" + newUrl);
                }
            }
        }
    }

    public void save() {
        save(m_currentFile);
    }

    protected void save(File file) {
        try {
            String output = toJson().toString();
            Surveyor.LOG.d(" >> " + output);
            FileUtils.write(file, output);
        } catch (IOException e) {
            Surveyor.LOG.e("Failure writing submission", e);
        }
    }

    public void submit() {
        final TembaService rapid = Surveyor.get().getRapidProService();
        final Submission submission = this;

        // submit any created fields
        rapid.addCreatedFields(m_fields);

        // first we need to create our contact
        Surveyor.LOG.d(m_contact.toJson().toString());
        rapid.addContact(m_contact);

        // then post the results
        submission.resolveMedia();
        rapid.addResults(submission);

    }

    public void delete() {
        if (m_currentFile != null) {

            // delete ourselves
            FileUtils.deleteQuietly(m_currentFile);

            // and our associated media files
            deleteMediaFiles();
        }
    }

    public boolean isCompleted() {
        return m_completed;
    }

    public static void deleteFlowSubmissions(int orgId, String uuid) {
        try {
            FileUtils.deleteDirectory(getFlowDir(orgId, uuid));
        } catch (IOException e) {}
    }

    public File getMediaDir() {
        File submissionDir = m_currentFile.getParentFile();
        File mediaDir = new File(submissionDir, "media");
        mediaDir.mkdirs();
        return mediaDir;

    }

    /**
     * Get the prefix associated with this submission
     * @return
     */
    public String getPrefix() {
        // our prefix should be the filename up to the '.json' extension
        String filename = m_completedFile.getName();
        return filename.substring(0, filename.length() - 5);
    }

    /**
     * Gets a unique file in the media directory keyed for this submission
     */
    public File createMediaFile(String extension) {
        return getUniqueFile(getMediaDir(), getPrefix(), extension);
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

    public void complete() {
        save(m_completedFile);
    }
}