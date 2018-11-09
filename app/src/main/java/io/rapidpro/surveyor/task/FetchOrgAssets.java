package io.rapidpro.surveyor.task;

import android.os.AsyncTask;

import java.util.List;

import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Group;

/**
 * Task to fetch all relevant assets for a single org
 */
public class FetchOrgAssets extends AsyncTask<Org, Integer, Void> {

    private FetchOrgAssetsListener listener;
    private boolean failed;

    public FetchOrgAssets(FetchOrgAssetsListener listener) {
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(Org... args) {
        Org org = args[0];

        try {
            // start by re-fetching the org details
            Org.fetch(org.getToken());

            publishProgress(10);

            List<Field> fields = Surveyor.get().getRapidProService().getFields(org.getToken());

            publishProgress(50);

            List<Group> groups = Surveyor.get().getRapidProService().getGroups(org.getToken());

            publishProgress(100);

            // TODO

        } catch (Exception e) {
            e.printStackTrace();
            this.failed = true;
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        listener.onProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if (this.failed) {
            this.listener.onFailure();
        } else {
            this.listener.onComplete();
        }
    }

    public interface FetchOrgAssetsListener {
        void onProgress(int percent);
        void onComplete();
        void onFailure();
    }
}
