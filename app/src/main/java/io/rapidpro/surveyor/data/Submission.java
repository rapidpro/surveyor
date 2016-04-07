package io.rapidpro.surveyor.data;

import android.net.Uri;
import android.os.Environment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.FileUtils;
import org.threeten.bp.Instant;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // the username submitting this run
    private String m_username;

    // the app version when this submission started
    private String m_appVersion;

    private static FilenameFilter SUBMISSION_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return !filename.endsWith(FLOW_FILE) && !filename.equals(MEDIA_DIR);
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
        //File runsDir = new File(Surveyor.get().getFilesDir(), SUBMISSIONS_DIR);
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
        return Flow.fromJson(flow);
    }

    /**
     * Loads a submission from a file. Assumes the flow file is present in
     * the parent directory.
     */
    public static Submission load(String username, File file) {
        try {

            Flow.DeserializationContext context = new Flow.DeserializationContext(getFlow(file));
            String json = FileUtils.readFileToString(file);

            Surveyor.LOG.d(" << " + json);
            JsonElement obj = JsonUtils.getGson().fromJson(json, JsonElement.class);
            Submission submission = JsonUtils.fromJson(obj, context, Submission.class);

            if (submission.m_username == null) {
                submission.m_username = username;
            }

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
    public Submission(String username, DBFlow flow) {

        m_username = username;
        m_flow = flow.getUuid();
        m_contact = new Contact();
        m_revision = flow.getRevision();
        m_appVersion = BuildConfig.VERSION_NAME;

        // get a unique filename for our submission
        File flowDir = getFlowDir(flow.getOrg().getId(), flow.getUuid());
        m_file = getUniqueFile(flowDir, flow.getRevision() + "_" + UUID.randomUUID().toString(), "json");

        // write our flow definition if it isn't there yet
        File flowFile = new File(flowDir, flow.getRevision() + "_" + FLOW_FILE);
        if (!flowFile.exists()) {
            try {
                FileUtils.writeStringToFile(flowFile, flow.getDefinition());
            } catch (Exception e) {
                Surveyor.LOG.e("Failed to write flow to disk", e);
                // TODO: this should probably fail hard
            }
        }
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
            int flowRun = rapid.getFlowRun(this);
            for (RuleSet.Result result : results) {
                String media = result.getMedia();
                if (media != null) {
                    int split = media.indexOf(":");
                    String type = media.substring(0, split);
                    String fileUrl = media.substring(split + 1, media.length());
                    String newUrl = rapid.uploadMedia(new File(Uri.parse(fileUrl).getPath()), m_flow, flowRun);
                    result.setMedia(type + ":" + newUrl);
                }
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

    public void submit() throws IOException {
        final TembaService rapid = Surveyor.get().getRapidProService();
        final Submission submission = this;

        // submit any created fields
        rapid.addCreatedFields(m_fields);

        // first we need to create our contact
        rapid.addContact(m_contact);

        // then post the results
        submission.resolveMedia();
        rapid.addResults(submission);
    }

    public void delete() {
        if (m_file != null) {

            // delete ourselves
            FileUtils.deleteQuietly(m_file);

            // and our associated media files
            deleteMediaFiles();
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

    public File getMediaDir() {
        File submissionDir = m_file.getParentFile();
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
        String filename = m_file.getName();
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

}

