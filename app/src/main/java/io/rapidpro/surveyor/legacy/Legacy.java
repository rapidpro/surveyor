package io.rapidpro.surveyor.legacy;

import android.os.Environment;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.net.TembaException;
import io.rapidpro.surveyor.utils.SurveyUtils;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class Legacy {

    /**
     * Gets whether a cleanup is needed - i.e. there's an existing 11.x installation
     *
     * @return true if we have an existing 11.x installation
     */
    public static boolean isCleanupNeeded() {
        return new File(SurveyorApplication.get().getFilesDir(), "default.realm").exists();
    }

    public static File getStorageDirectory() {
        return new File(Environment.getExternalStorageDirectory(), "Surveyor");
    }

    public static File getSubmissionsDirectory() {
        return new File(getStorageDirectory(), "submissions");
    }

    /**
     * Loads orgs from a legacy Realm database and matches them with new orgs
     */
    public static void cleanupOrgs(Set<Org> orgs) throws IOException {
        RealmConfiguration config = new RealmConfiguration.Builder(SurveyorApplication.get()).build();
        Realm.setDefaultConfiguration(config);
        Realm realm = Realm.getDefaultInstance();

        File orgsDirectory = SurveyUtils.mkdir(SurveyorApplication.get().getFilesDir(), "orgs");

        RealmResults<DBOrg> legacyOrgs = realm.allObjects(DBOrg.class);
        for (DBOrg legacyOrg : legacyOrgs) {
            // get the directory with this legacy org's submissions
            File orgSubmissionsDir = new File(getSubmissionsDirectory(), "" + legacyOrg.getId());

            // if the legacy org has submissions, find the equivalent new org and save the directory for later
            if (orgSubmissionsDir.exists()) {
                Org newOrg = findEquivalentOrg(orgs, legacyOrg);
                if (newOrg != null) {
                    Logger.d("Attaching legacy submissions for legacy org #" + legacyOrg.getId() + " to new org " + newOrg.getUuid());

                    newOrg.setLegacySubmissionsDirectory(orgSubmissionsDir.getAbsolutePath());
                    newOrg.save();
                }
            }

            // then delete legacy config directory
            File legacyDir = new File(orgsDirectory, "" + legacyOrg.getId());
            if (legacyDir.exists()) {
                FileUtils.deleteDirectory(legacyDir);
            }
        }

        // delete no longer needed realm DB
        realm.close();
        Realm.deleteRealm(realm.getConfiguration());
    }

    /**
     * Gets the legacy submissions for the given org
     *
     * @return the submission files
     */
    public static List<File> getCompleted(Org org) {
        List<File> submissions = new ArrayList<>();

        for (Flow flow : org.getFlows()) {
            submissions.addAll(getCompleted(org, flow));
        }
        return submissions;
    }

    /**
     * Gets the count of legacy submissions for the given org
     *
     * @return the count
     */
    public static int getCompletedCount(Org org) {
        return getCompleted(org).size();
    }

    /**
     * Gets the legacy submissions for the given org and flow
     *
     * @return the submission files
     */
    public static List<File> getCompleted(Org org, Flow flow) {
        Logger.d("Looking up completed legacy submissions for flow " + flow.getUuid());

        if (org.getLegacySubmissionsDirectory() == null) {
            return Collections.emptyList();
        }

        File orgDir = new File(org.getLegacySubmissionsDirectory());
        List<File> submissions = new ArrayList<>();

        if (orgDir.exists()) {
            File flowDir = new File(orgDir, flow.getUuid());
            if (flowDir.exists()) {
                submissions.addAll(Arrays.asList(flowDir.listFiles(Submission.SUBMISSION_FILTER)));
            }
        }
        return submissions;
    }

    /**
     * Gets the count of legacy submissions for the given org and flow
     *
     * @return the count
     */
    public static int getCompletedCount(Org org, Flow flow) {
        return getCompleted(org, flow).size();
    }

    /**
     * Submits a legacy submission
     *
     * @param subFile  the submission file
     * @param username the API token to submit with
     * @param username the username to submit as
     */
    public static void submit(File subFile, String token, String username) throws IOException, TembaException {
        Submission submission = Submission.load(username, subFile);
        submission.submit(token);
        submission.delete();

        // if there are no other submissions for this flow, delete its folder
        File flowDir = subFile.getParentFile();
        if (flowDir.listFiles(Submission.SUBMISSION_FILTER).length == 0) {
            FileUtils.deleteDirectory(flowDir);
        }
    }

    /**
     * Finds the equivalent new org for the given legacy org
     *
     * @param orgs      the set of all new orgs
     * @param legacyOrg the legacy org
     * @return the matching new org
     */
    private static Org findEquivalentOrg(Collection<Org> orgs, DBOrg legacyOrg) {
        for (Org org : orgs) {
            if (org.getToken().equals(legacyOrg.getToken())) {
                return org;
            }
        }
        return null;
    }
}
