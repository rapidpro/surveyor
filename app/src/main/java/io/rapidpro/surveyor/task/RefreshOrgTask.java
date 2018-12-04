package io.rapidpro.surveyor.task;

import android.os.AsyncTask;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.data.Org;

/**
 * Task to completely refresh a single org - details and assets
 */
public class RefreshOrgTask extends AsyncTask<Org, Integer, Void> {

    private Listener listener;
    private boolean failed;

    public RefreshOrgTask(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(Org... args) {
        Org org = args[0];

        try {
            org.refresh(true, new Org.RefreshProgress() {
                @Override
                public void reportProgress(int percent) {
                    publishProgress(percent);
                }
            });

        } catch (Exception e) {
            Logger.e("Unable to refresh org", e);
            this.failed = true;
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

        if (this.failed) {
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
