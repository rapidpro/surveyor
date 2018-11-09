package io.rapidpro.surveyor.task;

import android.os.AsyncTask;

import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.Org;

/**
 * Task to fetch all relavent assets for a single org
 */
public class FetchOrgAssets extends AsyncTask<Org, Void, Void> {
    @Override
    protected Void doInBackground(Org... args) {
        Org org = args[0];
        //Surveyor.get().getRapidProService().get

        return null;
    }

    public interface FetchOrgAssetsListener {
        void onProgress(int percent);
        void onComplete();
        void onFailure();
    }
}
