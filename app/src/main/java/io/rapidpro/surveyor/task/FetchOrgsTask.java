package io.rapidpro.surveyor.task;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.OrgService;
import io.rapidpro.surveyor.legacy.Legacy;
import io.rapidpro.surveyor.net.responses.Token;

/**
 * Task to fetch orgs from RapidPro, create their directories, save their details, and return their UUIDs
 */
public class FetchOrgsTask extends AsyncTask<Token, Void, Set<String>> {

    private Listener listener;
    private boolean failed;

    public FetchOrgsTask(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected Set<String> doInBackground(Token... tokens) {
        OrgService svc = SurveyorApplication.get().getOrgService();

        Set<Org> orgs = new HashSet<>();
        Set<String> orgUUIDs = new HashSet<>();

        for (Token token : tokens) {
            try {
                Org org = svc.getOrFetch(token.getOrg().getUuid(), token.getOrg().getName(), token.getToken());

                orgs.add(org);
                orgUUIDs.add(org.getUuid());

                Logger.d("Fetched org with UUID " + org.getUuid());
            } catch (Exception e) {
                Logger.e("Unable to fetch org", e);
                this.failed = true;
                break;
            }
        }

        // finally clean up any legacy orgs leftover from an older installation
        if (Legacy.isCleanupNeeded()) {
            try {
                Legacy.cleanupOrgs(orgs);
            } catch (IOException e) {
                Logger.e("Unable to cleanup legacy orgs", e);
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

    public interface Listener {
        void onComplete(Set<String> orgUUIDs);

        void onFailure();
    }
}
