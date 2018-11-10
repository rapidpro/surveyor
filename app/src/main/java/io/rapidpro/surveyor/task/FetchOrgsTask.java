package io.rapidpro.surveyor.task;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.data.Org;

/**
 * Task to fetch orgs from RapidPro, create their directories, save their details, and return their UUIDs
 */
public class FetchOrgsTask extends AsyncTask<String, Void, Set<String>> {

    private FetchOrgsListener listener;
    private boolean failed;

    public FetchOrgsTask(FetchOrgsListener listener) {
        this.listener = listener;
    }

    @Override
    protected Set<String> doInBackground(String... tokens) {
        Set<String> orgUUIDs = new HashSet<>();
        for (String token : tokens) {
            try {
                Org org = Org.fetch(token);
                orgUUIDs.add(org.getUUID());

                SurveyorApplication.LOG.d("Fetched org with UUID " + org.getUUID());
            } catch (IOException e) {
                e.printStackTrace();
                this.failed = true;
                break;
            }
        }
        return orgUUIDs;
    }

    @Override
    protected void onPostExecute(Set<String> orgUUIDs) {
        super.onPostExecute(orgUUIDs);

        if (this.failed) {
            this.listener.onFailure();
        } else {
            this.listener.onComplete(orgUUIDs);
        }
    }

    public interface FetchOrgsListener {
        void onComplete(Set<String> orgUUIDs);
        void onFailure();
    }
}
