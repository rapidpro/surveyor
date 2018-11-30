package io.rapidpro.surveyor.legacy;

import android.os.Environment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.net.TembaException;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class Legacy {

    /**
     * Gets whether a migration is needed - i.e. there's an existing 11.x installation
     *
     * @return true if we have an existing 11.x installation
     */
    public static boolean isMigrationNeeded() {
        return new File(SurveyorApplication.get().getFilesDir(), "default.realm").exists();
    }

    public static File getStorageDirectory() throws IOException {
        return new File(Environment.getExternalStorageDirectory(), "Surveyor");
    }

    public static File getSubmissionsDirectory() throws IOException {
        return new File(getStorageDirectory(), "submissions");
    }

    /**
     * Loads orgs from a legacy Realm database and copies their API tokens into their submissions
     * directory so they can be submitted later without the database
     */
    public static void migrate() throws IOException {
        RealmConfiguration config = new RealmConfiguration.Builder(SurveyorApplication.get()).build();
        Realm.setDefaultConfiguration(config);
        Realm realm = Realm.getDefaultInstance();

        File orgsDirectory = SurveyorApplication.get().getOrgsDirectory();

        RealmResults<DBOrg> orgs = realm.allObjects(DBOrg.class);
        for (DBOrg legacyOrg : orgs) {
            // get the directory with this legacy org's submissions
            File orgSubmissionsDir = new File(getSubmissionsDirectory(), "" + legacyOrg.getId());

            if (orgSubmissionsDir.exists()) {
                // save the org token in there so we can submit it in future without the org
                File tokenFile = new File(orgSubmissionsDir, "token");
                FileUtils.write(tokenFile, legacyOrg.getToken());
            }

            // then delete legacy directory
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
     * Gets the count of legacy submissions across all orgs
     *
     * @return the count
     */
    public static int getSubmissionsCount() throws IOException {
        File submissionsDir = getSubmissionsDirectory();
        int total = 0;

        if (submissionsDir.exists()) {
            for (File orgDir : submissionsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
                for (File flowDir : orgDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
                    total += flowDir.listFiles(Submission.SUBMISSION_FILTER).length;
                }
            }
        }
        return total;
    }

    /**
     * Submits (and deletes) all legacy submissions
     *
     * @param username the username to submit as
     */
    public static void submitAll(String username) throws IOException, TembaException {
        File submissionsDir = getSubmissionsDirectory();

        if (submissionsDir.exists()) {
            for (File orgDir : submissionsDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
                File tokenFile = new File(orgDir, "token");
                String apiToken = FileUtils.readFileToString(tokenFile);

                for (File flowDir : orgDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
                    for (File subFile : flowDir.listFiles(Submission.SUBMISSION_FILTER)) {
                        Submission submission = Submission.load(username, subFile);
                        submission.submit(apiToken);
                        submission.delete();
                    }
                    FileUtils.deleteDirectory(flowDir);
                }
                FileUtils.deleteDirectory(orgDir);
            }
            FileUtils.deleteDirectory(submissionsDir);
        }
    }
}
