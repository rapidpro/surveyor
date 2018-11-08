package io.rapidpro.surveyor.task;

import android.os.AsyncTask;

import java.io.IOException;

import io.rapidpro.surveyor.data.Org;

/**
 * Task to fetch orgs from RapidPro
 */
public class FetchOrgsTask extends AsyncTask<String, Void, Void> {

    private FetchOrgsListener listener;
    private boolean failed;

    public FetchOrgsTask(FetchOrgsListener listener) {
        this.listener = listener;
        this.failed = false;
    }

    @Override
    protected Void doInBackground(String... tokens) {
        for (String token : tokens) {
            try {
                Org.fetch(token);
            } catch (IOException e) {
                e.printStackTrace();
                this.failed = true;
            }
        }
        return null;
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

    public interface FetchOrgsListener {
        void onComplete();
        void onFailure();
    }
}
