package io.rapidpro.surveyor.data;

import android.net.Uri;

import com.nyaruka.goflow.mobile.Event;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.engine.EngineException;
import io.rapidpro.surveyor.engine.Session;
import io.rapidpro.surveyor.utils.SurveyUtils;

public class Submission {

    private static final String SESSION_FILE = "session.json";
    private static final String EVENTS_FILE = "events.jsonl";
    private static final String MEDIA_DIR = "media";

    private transient File directory;

    public Submission(File directory) {
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
     * Saves the current session
     *
     * @param session the current session
     */
    public void saveSession(Session session) throws IOException, EngineException {
        File file = new File(directory, SESSION_FILE);
        FileUtils.writeStringToFile(file, session.toJSON());
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
            writer.write(event.getPayload());
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
     * Deletes this submission from the file system
     */
    public void delete() {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            SurveyorApplication.LOG.e("Unable to delete submission " + directory.getAbsolutePath(), e);
        }
    }

    public void submit() {
        // TODO upload media

        // TODO send events

        delete();
    }

    private File getMediaDirectory() throws IOException {
        return SurveyUtils.mkdir(directory, MEDIA_DIR);
    }
}
