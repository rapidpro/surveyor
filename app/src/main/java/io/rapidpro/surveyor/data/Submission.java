package io.rapidpro.surveyor.data;

import android.media.MediaMetadataRetriever;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.FileUtils;
import org.threeten.bp.Instant;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.runner.ContactUrn;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Step;
import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.net.RapidProService;
import retrofit.RetrofitError;

/**
 * Represents a single flow run. Manages saving run progress and
 * submission to the server. Submissions are stored on the file
 * system to be tolerant of database changes in the future.
 */
public class Submission {

    private transient static final String SUBMISSIONS_DIR = "submissions";
    private transient static final String FLOW_FILE = "flow.json";

    // the file we will be persisted in
    private transient File m_file;

    @SerializedName("steps")
    protected List<Step> m_steps;

    // flow uuid
    @SerializedName("flow")
    private String m_flow;

    // our contact participating in the flow
    @SerializedName("contact")
    private Contact m_contact;

    // when the flow run started
    @SerializedName("started")
    @JsonAdapter(JsonUtils.InstantAdapter.class)
    private Instant m_started;

    // if the flow was completed
    @SerializedName("completed")
    private boolean m_completed;

    private static FilenameFilter FLOW_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return !filename.equals(FLOW_FILE);
        }
    };

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
    private static File getFlowDir(DBFlow flow) {
        File flowDir = new File(getSubmissionsDir(), flow.getUuid());
        flowDir.mkdirs();
        return flowDir;
    }

    /**
     * Get the number of pending submissions for this flow
     */
    public static int getPendingSubmissionCount(DBFlow flow) {
        return getFlowDir(flow).list(FLOW_FILE_FILTER).length;
    }

    /**
     * Get all the submission files for the given flow
     */
    public static File[] getPendingSubmissions(DBFlow flow) {
        return getFlowDir(flow).listFiles(FLOW_FILE_FILTER);
    }

    /**
     * Get all submission files across all flows
     */
    public static File[] getPendingSubmissions() {
        List<File> files = new ArrayList<>();
        for (File dir : getSubmissionsDir().listFiles()) {
            if (dir.isDirectory()) {
                for (File submission : dir.listFiles(FLOW_FILE_FILTER)) {
                    files.add(submission);
                }
            }
        }

        File[] results = new File[files.size()];
        return files.toArray(results);
    }

    /**
     * Read the flow definition from disk
     */
    private static Flow getFlow(File file) {
        File flowFile = new File(file.getParent(), FLOW_FILE);
        Surveyor.LOG.d("Reading flow: " + flowFile.getName());
        String flow = null;
        try {
            flow = FileUtils.readFileToString(flowFile);
        } catch (IOException e) {
            Surveyor.LOG.e("Error loading flow", e);
        }
        return Flow.fromJson(flow);
    }

    /**
     * Loads a submission from a file. Assumes the flow file is present in
     * the parent directory.
     */
    public static Submission load(File file) {
        try {

            Flow.DeserializationContext context = new Flow.DeserializationContext(getFlow(file));
            JsonUtils.setDeserializationContext(context);
            String json = FileUtils.readFileToString(file);
            Submission submission = JsonUtils.getGson().fromJson(json, Submission.class);

            submission.setFile(file);
            return submission;
        } catch (IOException e) {
            // we'll return null
            Surveyor.LOG.e("Failure reading submission", e);
        } finally {
            JsonUtils.clearDeserializationContext();
        }
        return null;
    }

    /**
     * Create a new submission for a flow
     */
    public Submission(DBFlow flow) {

        m_flow = flow.getUuid();
        m_contact = new Contact();

        String uuid = UUID.randomUUID().toString();

        // get a unique filename
        File flowDir = getFlowDir(flow);
        File file =  new File(flowDir, uuid + "_1.json");
        int count = 2;
        while (file.exists()) {
            file =  new File(flowDir, uuid + "_" + count + ".json");
            count++;
        }

        File flowFile = new File(flowDir, FLOW_FILE);
        if (!flowFile.exists()) {
            try {
                FileUtils.writeStringToFile(flowFile, flow.getDefinition());
            } catch (IOException e) {
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

        List<Step> completed = runState.getCompletedSteps();
        if (m_steps == null) {
            m_steps = new ArrayList<>();
        }
        m_steps.addAll(completed);

        // keep track of when we were started
        m_started = runState.getStarted();

        // mark us completed if necessary
        m_completed = runState.getState() == RunState.State.COMPLETED;

    }

    public void save() {
        String json = JsonUtils.getGson().toJson(this);
        try {
            FileUtils.write(m_file, json);
        } catch (IOException e) {
            Surveyor.LOG.e("Failure writing submission", e);
        }
    }

    public void submit() {
        final RapidProService rapid = Surveyor.get().getRapidProService();
        final Submission submission = this;

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

    public static class ContactSerializer implements JsonSerializer<Contact> {
        @Override
        public JsonElement serialize(Contact src, Type typeOfSrc, JsonSerializationContext context) {

            // if we have a uuid, it's been provided from the server, so we can
            // represent ourselves with the uuid
            if (src.getUuid() != null) {
                return new JsonPrimitive(src.getUuid());
            }

            // if we don't know the uuid yet, create a json object
            // with all of our known properties
            else {
                JsonArray urns = new JsonArray();
                for (ContactUrn urn : src.getUrns()) {
                    urns.add(new JsonPrimitive(urn.toString()));
                }

                JsonObject obj = new JsonObject();
                obj.addProperty("name", src.getName());
                obj.addProperty("language", src.getLanguage());
                obj.add("urns", urns);
                return obj;
            }
        }
    }
}

