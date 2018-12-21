package io.rapidpro.surveyor.data;

import android.net.Uri;

import com.nyaruka.goflow.mobile.Event;
import com.nyaruka.goflow.mobile.Modifier;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.engine.EngineException;
import io.rapidpro.surveyor.engine.Session;
import io.rapidpro.surveyor.net.TembaException;
import io.rapidpro.surveyor.net.requests.SubmissionPayload;
import io.rapidpro.surveyor.utils.RawJson;
import io.rapidpro.surveyor.utils.SurveyUtils;

public class Submission {

    private static final String SESSION_FILE = "session.json";
    private static final String MODIFIERS_FILE = "modifiers.jsonl";
    private static final String EVENTS_FILE = "events.jsonl";
    private static final String COMPLETION_FILE = ".completed";
    private static final String MEDIA_DIR = "media";

    private Org org;
    private File directory;

    /**
     * Creates a new submission for the given org in the given directory
     *
     * @param org       the org
     * @param directory the directory
     */
    public Submission(Org org, File directory) {
        this.org = org;
        this.directory = directory;
    }

    /**
     * Gets the UUID of this org (i.e. the name of its directory)
     *
     * @return the UUID
     */
    public String getUuid() {
        return directory.getName();
    }

    /**
     * Gets the org this submission belongs to
     *
     * @return the org
     */
    public Org getOrg() {
        return org;
    }

    /**
     * Get's the directory this submission is stored in
     *
     * @return the directory
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Get's the directory this submission's media is stored in
     *
     * @return the directory
     */
    public File getMediaDirectory() throws IOException {
        return SurveyUtils.mkdir(directory, MEDIA_DIR);
    }

    /**
     * Gets whether this submission is complete
     *
     * @return true if complete
     */
    public boolean isCompleted() {
        return new File(directory, COMPLETION_FILE).exists();
    }

    /**
     * Saves the current session
     *
     * @param session the current session
     */
    public void saveSession(Session session) throws IOException, EngineException {
        FileUtils.writeStringToFile(new File(directory, SESSION_FILE), session.toJSON());
    }

    /**
     * Saves new modifiers to this submission
     *
     * @param modifiers the modifiers to save
     */
    public void saveNewModifiers(List<Modifier> modifiers) throws IOException {
        File file = new File(directory, MODIFIERS_FILE);

        BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

        for (Modifier mod : modifiers) {
            writer.write(mod.payload());
            writer.newLine();
        }

        writer.close();
    }

    /**
     * Saves new events to this submission
     *
     * @param events the events to save
     */
    public void saveNewEvents(List<Event> events) throws IOException {
        File file = new File(directory, EVENTS_FILE);

        BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

        for (Event event : events) {
            writer.write(event.payload());
            writer.newLine();
        }

        writer.close();
    }

    /**
     * Saves a new media file to this submission
     *
     * @param data      the media data
     * @param extension the file extension
     * @return the URI of the saved file
     */
    public Uri saveMedia(byte[] data, String extension) throws IOException {
        File file = new File(getMediaDirectory(), UUID.randomUUID().toString() + "." + extension);
        FileUtils.writeByteArrayToFile(file, data);
        return SurveyorApplication.get().getUriForFile(file);
    }

    /**
     * Saves a new media file to this submission
     *
     * @param src the file to copy
     * @return the URI of the saved file
     */
    public Uri saveMedia(File src) throws IOException {
        String extension = FilenameUtils.getExtension(src.getName());
        File file = new File(getMediaDirectory(), UUID.randomUUID().toString() + "." + extension);
        FileUtils.copyFile(src, file);
        return SurveyorApplication.get().getUriForFile(file);
    }

    /**
     * Marks this submission as completed
     */
    public void complete() throws IOException {
        FileUtils.writeStringToFile(new File(directory, COMPLETION_FILE), "");
    }

    /**
     * Deletes this submission from the file system
     */
    public void delete() {
        try {
            FileUtils.deleteDirectory(directory);
            directory = null;
        } catch (IOException e) {
            Logger.e("Unable to delete submission " + directory.getAbsolutePath(), e);
        }
    }

    public void submit() throws IOException, TembaException {
        Logger.d("Submitting submission " + getUuid() + "...");

        String session = FileUtils.readFileToString(new File(directory, SESSION_FILE));
        List<String> modifiers = FileUtils.readLines(new File(directory, MODIFIERS_FILE));
        List<String> events = FileUtils.readLines(new File(directory, EVENTS_FILE));

        // upload all media and get a new remote URL for each item
        Map<Uri, String> mediaUrls = uploadMedia();

        // convert the map to parallel arrays of strings for replacement
        String[] oldUris = new String[mediaUrls.size()];
        String[] newUrls = new String[mediaUrls.size()];
        int e = 0;
        for (Map.Entry<Uri, String> entry : mediaUrls.entrySet()) {
            oldUris[e] = entry.getKey().toString();
            newUrls[e] = entry.getValue();
            e++;
        }

        for (int i = 0; i < oldUris.length; i++) {
            Logger.d(oldUris[i] + " --> " + newUrls[i]);
        }

        RawJson sessionJson = new RawJson(StringUtils.replaceEach(session, oldUris, newUrls));
        List<RawJson> modifiersJson = new ArrayList<>(modifiers.size());
        for (String modifier : modifiers) {
            modifiersJson.add(new RawJson(modifier));
        }
        List<RawJson> eventsJson = new ArrayList<>(events.size());
        for (String event : events) {
            eventsJson.add(new RawJson(StringUtils.replaceEach(event, oldUris, newUrls)));
        }

        SubmissionPayload payload = new SubmissionPayload(sessionJson, modifiersJson, eventsJson);

        SurveyorApplication.get().getTembaService().submit(org.getToken(), payload);

        delete();
    }

    /**
     * Upload all media files for this submission and return a map of their new URLs
     *
     * @return the map of local URIs to remote URLs
     */
    private Map<Uri, String> uploadMedia() throws IOException, TembaException {
        if (!hasMedia()) {
            return Collections.emptyMap();
        }

        SurveyorApplication app = SurveyorApplication.get();
        Map<Uri, String> uploads = new HashMap<>();

        for (File mediaFile : getMediaDirectory().listFiles()) {
            Uri mediaUri = app.getUriForFile(mediaFile);
            String newUrl = app.getTembaService().uploadMedia(org.getToken(), mediaUri);

            uploads.put(mediaUri, newUrl);

            Logger.d("Uploaded media " + mediaUri + " to " + newUrl);
        }
        return uploads;
    }

    private boolean hasMedia() {
        return new File(directory, MEDIA_DIR).exists();
    }
}
