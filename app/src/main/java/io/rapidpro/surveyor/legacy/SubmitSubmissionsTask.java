package io.rapidpro.surveyor.legacy;

import android.os.AsyncTask;

import java.io.IOException;

import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.net.TembaException;

/**
 * Task for sending legacy submissions to the server
 */
public class SubmitSubmissionsTask extends AsyncTask<String, Integer, Void> {

    private Listener listener;
    private boolean failed = false;

    public SubmitSubmissionsTask(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(String... args) {
        String username = args[0];
        try {
            Legacy.submitAll(username, new Legacy.SubmitProgress() {
                @Override
                public void onProgress(int percent) {
                    publishProgress(percent);
                }
            });
        } catch (IOException |TembaException e) {
            SurveyorApplication.LOG.e("Error during submitting legacy submissions", e);
            failed = true;
        }

        return null;
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
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if (failed) {
            this.listener.onFailure();
        } else {
            this.listener.onComplete();
        }
    }

    public interface Listener {
        void onProgress(int percent);

        void onComplete();

        void onFailure();
    }
}
