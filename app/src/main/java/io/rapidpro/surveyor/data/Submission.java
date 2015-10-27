package io.rapidpro.surveyor.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.apache.commons.io.FileUtils;
import org.threeten.bp.Instant;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.rapidpro.expressions.utils.ExpressionUtils;
import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.runner.ContactUrn;
import io.rapidpro.flows.runner.Field;
import io.rapidpro.flows.runner.Org;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Step;
import io.rapidpro.flows.runner.Value;
import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.flows.utils.Jsonizable;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.net.RapidProService;

/**
 * Represents a single flow run. Manages saving run progress and
 * submission to the server. Submissions are stored on the file
 * system to be tolerant of database changes in the future.
 */
public class Submission implements Jsonizable {

    private transient static final String SUBMISSIONS_DIR = "submissions";
    private transient static final String FLOW_FILE = "flow.json";

    // the file we will be persisted in
    private transient File m_file;

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

    private static FilenameFilter NOT_FLOW_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return !filename.endsWith(FLOW_FILE);
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
        File runsDir = new File(Surveyor.get().getFilesDir(), SUBMISSIONS_DIR);
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
        long start = System.currentTimeMillis();
        Surveyor.get().LOG.d("Looking up submissions for " + flow.getName());
        File[] submissions = getFlowDir(flow.getOrg().getId(), flow.getUuid()).listFiles(NOT_FLOW_FILE_FILTER);
        Surveyor.get().LOG.d("Done: " + (System.currentTimeMillis() - start) + "ms");
        return submissions;
    }

    /**
     * Get all submission files across all flows
     */
    public static File[] getPendingSubmissions(int orgId) {

        long start = System.currentTimeMillis();
        Surveyor.get().LOG.d("Looking up all submissions..");
        List<File> files = new ArrayList<>();
        for (File dir : getOrgDir(orgId).listFiles()) {
            if (dir.isDirectory()) {
                for (File submission : dir.listFiles(NOT_FLOW_FILE_FILTER)) {
                    files.add(submission);
                }
            }
        }

        File[] results = new File[files.size()];
        results = files.toArray(results);
        Surveyor.get().LOG.d("Done: " + (System.currentTimeMillis() - start) + "ms");
        return results;
    }

    /**
     * Read the flow definition from disk
     */
    private static Flow getFlow(File file) {
        String revision = file.getName().split("_")[0];
        File flowFile = new File(file.getParent(), revision + "_" + FLOW_FILE);
        Surveyor.LOG.d("Reading flow: " + flowFile.getName());
        String flow = null;
        try {
            flow = FileUtils.readFileToString(flowFile);
        } catch (IOException e) {
            Surveyor.LOG.e("Error loading flow", e);
        }
        Surveyor.LOG.d("From file: " + flow);
        return Flow.fromJson(flow);
    }

    /**
     * Loads a submission from a file. Assumes the flow file is present in
     * the parent directory.
     */
    public static Submission load(File file) {
        try {

            Flow.DeserializationContext context = new Flow.DeserializationContext(getFlow(file));
            String json = FileUtils.readFileToString(file);

            Surveyor.LOG.d(" << " + json);
            JsonElement obj = JsonUtils.getGson().fromJson(json, JsonElement.class);
            Submission submission = JsonUtils.fromJson(obj, context, Submission.class);
            submission.setFile(file);
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

        if (obj.has("revision")) {
            submission.m_revision = obj.get("revision").getAsInt();
        } else if (obj.has("version")) {
            submission.m_revision = obj.get("version").getAsInt();
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
                "completed", m_completed
        );
    }


    /**
     * Create a new submission for a flow
     */
    public Submission(DBFlow flow) {

        m_flow = flow.getUuid();
        m_contact = new Contact();
        m_revision = flow.getRevision();

        String uuid = UUID.randomUUID().toString();

        // get a unique filename
        File flowDir = getFlowDir(flow.getOrg().getId(), flow.getUuid());
        File file =  new File(flowDir, flow.getRevision() + "_" + uuid + "_1.json");
        int count = 2;
        while (file.exists()) {
            file =  new File(flowDir, flow.getRevision() + "_" + uuid + "_" + count + ".json");
            count++;
        }

        File flowFile = new File(flowDir, flow.getRevision() + "_" + FLOW_FILE);
        if (!flowFile.exists()) {
            try {
                FileUtils.writeStringToFile(flowFile, flow.getDefinition());
            } catch (Exception e) {
                Surveyor.LOG.e("Failed to write flow to disk", e);
                // TODO: this should probably fail hard
            }
        }

        m_file = file;
    }

    public String getFilename() {
        return m_file.getAbsolutePath();
    }

    public void addSteps(RunState runState) {

        m_contact = runState.getContact();

        List<Step> completed = runState.getCompletedSteps();
        if (m_steps == null) {
            m_steps = new ArrayList<>();
        }
        m_steps.addAll(completed);

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

    public void save() {
        try {
            String output = toJson().toString();
            Surveyor.LOG.d(" >> " + output);
            FileUtils.write(m_file, output);
        } catch (IOException e) {
            Surveyor.LOG.e("Failure writing submission", e);
        }
    }

    public void submit() {
        final RapidProService rapid = Surveyor.get().getRapidProService();
        final Submission submission = this;

        // submit any created fields
        rapid.addCreatedFields(m_fields);

        // first we need to create our contact
        rapid.addContact(m_contact);

        // then post the results
        rapid.addResults(submission);
    }

    public void delete() {
        if (m_file != null) {
            FileUtils.deleteQuietly(m_file);
        }
    }

    public void setFile(File file) {
        m_file = file;
    }

    public boolean isCompleted() {
        return m_completed;
    }

    public static void deleteFlowSubmissions(int orgId, String uuid) {
        try {
            FileUtils.deleteDirectory(getFlowDir(orgId, uuid));
        } catch (IOException e) {}
    }
}

