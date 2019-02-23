package io.rapidpro.surveyor.task;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.legacy.Legacy;
import io.rapidpro.surveyor.net.TembaException;

/**
 * Task for sending submissions to the server
 */
public class SubmitSubmissionsTask extends AsyncTask<Submission, Integer, Integer> {

    private Listener listener;
    private int numFailed = 0;
    private String username;

    private String legacyToken;
    private List<File> legacySubmissions = new ArrayList<>();

    public void setUsername(String username) {
        this.username = username;
    }

    public void setLegacyToken(String legacyToken) {
        this.legacyToken = legacyToken;
    }

    public void includeLegacySubmissions(List<File> legacySubmissions) {
        this.legacySubmissions.addAll(legacySubmissions);
    }

    public SubmitSubmissionsTask(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(Submission... submissions) {
        int total = submissions.length + legacySubmissions.size();

        int s = 0;
        for (Submission submission : submissions) {
            try {
                submission.submit();
            } catch (IOException | TembaException e) {
                Logger.e("Unable to send submission", e);
                numFailed++;
            }

            s++;
            publishProgress(100 * s / total);
        }

        for (File legacy : legacySubmissions) {
            try {
                Legacy.submit(legacy, legacyToken, username);
            } catch (IOException | TembaException e) {
                Logger.e("Unable to send legacy submission", e);
                numFailed++;
            }

            s++;
            publishProgress(100 * s / total);
        }

        return total;
    }

    /**
     * @see AsyncTask#onProgressUpdate(Object[])
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        listener.onProgress(values[0]);
    }

    /**
     * @see AsyncTask#onPostExecute(Object)
     */
    @Override
    protected void onPostExecute(Integer total) {
        super.onPostExecute(total);

        if (numFailed > 0) {
            this.listener.onFailure(numFailed);
        } else {
            this.listener.onComplete(total);
        }
    }

    public interface Listener {
        void onProgress(int percent);

        void onComplete(int total);

        void onFailure(int numFailed);
    }
}
