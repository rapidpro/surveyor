package io.rapidpro.surveyor;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.FileProvider;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import io.rapidpro.surveyor.data.OrgService;
import io.rapidpro.surveyor.data.SubmissionService;
import io.rapidpro.surveyor.net.TembaService;
import io.rapidpro.surveyor.utils.SurveyUtils;

/**
 * Main application
 */
public class SurveyorApplication extends Application {

    /**
     * The singleton instance of this application
     */
    private static SurveyorApplication s_this;

    /**
     * Service for network operations
     */
    private TembaService tembaService = null;

    /**
     * Service for local org operations
     */
    private OrgService orgService = null;

    /**
     * Service for local submission operations
     */
    private SubmissionService submissionService = null;

    /**
     * Gets the singleton instance of this application
     *
     * @return the instance
     */
    public static SurveyorApplication get() {
        return s_this;
    }

    @Override
    public void onCreate() {
        Logger.d("Creating Surveyor application...");

        super.onCreate();

        Logger.d("External storage dir=" + Environment.getExternalStorageDirectory() + " state=" + Environment.getExternalStorageState() + " emulated=" + Environment.isExternalStorageEmulated());

        s_this = this;

        tembaService = new TembaService(getTembaHost());

        try {
            orgService = new OrgService(getOrgsDirectory());
            submissionService = new SubmissionService(getSubmissionsDirectory());
        } catch (IOException e) {
            Logger.e("Unable to create directory based services", e);
        }
    }

    /**
     * Gets the name of the preferences file
     *
     * @return the name of the preferences
     */
    public String getPreferencesName() {
        return "default";
    }

    /**
     * Gets the shared preferences for this application
     *
     * @return the preferences
     */
    public SharedPreferences getPreferences() {
        return s_this.getSharedPreferences(getPreferencesName(), Context.MODE_PRIVATE);
    }

    /**
     * Saves a string shared preference for this application
     *
     * @param key   the preference key
     * @param value the preference value
     */
    public void setPreference(String key, String value) {
        getPreferences().edit().putString(key, value).apply();
    }

    /**
     * Saves a string-set shared preference for this application
     *
     * @param key    the preference key
     * @param values the preference value
     */
    public void setPreference(String key, Set<String> values) {
        getPreferences().edit().putStringSet(key, values).apply();
    }

    /**
     * Clears a shared preference for this application
     *
     * @param key the preference key
     */
    public void clearPreference(String key) {
        getPreferences().edit().remove(key).apply();
    }

    /**
     * Gets the base URL of the Temba instance we're connected to
     *
     * @return the base URL
     */
    public String getTembaHost() {
        String host = getPreferences().getString(SurveyorPreferences.HOST, getString(R.string.pref_default_host));

        // strip any trailing slash
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        return host;
    }

    /**
     * Called when our host setting has changed
     */
    public void onTembaHostChanged() {
        String newHost = getTembaHost();

        Logger.d("Host changed to " + newHost);

        clearPreference(SurveyorPreferences.AUTH_USERNAME);
        clearPreference(SurveyorPreferences.AUTH_ORGS);
        try {
            clearSubmissions();
        } catch (IOException e) {
            Logger.e("Unable to clear submissions", e);
        }

        tembaService = new TembaService(newHost);
    }

    /**
     * Returns the Temba API service
     *
     * @return the service
     */
    public TembaService getTembaService() {
        return tembaService;
    }

    /**
     * Returns the local orgs service
     *
     * @return the service
     */
    public OrgService getOrgService() {
        return orgService;
    }

    /**
     * Returns the local submissions service
     *
     * @return the service
     */
    public SubmissionService getSubmissionService() {
        return submissionService;
    }

    /**
     * Gets the directory for org configurations
     *
     * @return the directory
     */
    public File getOrgsDirectory() throws IOException {
        return SurveyUtils.mkdir(getFilesDir(), "orgs");
    }

    /**
     * Gets the directory for user collected data
     *
     * @return the directory
     */
    public File getUserDirectory() {
        return getExternalFilesDir(null);
    }

    /**
     * Gets the submissions storage directory
     *
     * @return the directory
     */
    protected File getSubmissionsDirectory() throws IOException {
        return SurveyUtils.mkdir(getUserDirectory(), "submissions");
    }

    /**
     * Clears the submissions storage directory
     */
    public void clearSubmissions() throws IOException {
        FileUtils.deleteDirectory(getSubmissionsDirectory());
    }

    /**
     * Gets the URI for the given file using our application's file provider
     *
     * @param file the file
     * @return the URI
     */
    public Uri getUriForFile(File file) {
        return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
    }

    /**
     * Generate dump of the Android log
     *
     * @return the URI of the dump file
     */
    public Uri generateLogDump() throws IOException {
        // log our build and device details
        Logger.d("Version: " + BuildConfig.VERSION_NAME + "; " + BuildConfig.VERSION_CODE);
        Logger.d("OS: " + System.getProperty("os.version") + " (API " + Build.VERSION.SDK_INT + ")");
        Logger.d("Model: " + android.os.Build.MODEL + " (" + android.os.Build.DEVICE + ")");

        // dump log to file and return URI
        File outputFile = new File(getUserDirectory(), "bug-report.txt");
        Runtime.getRuntime().exec("logcat -d -f " + outputFile.getAbsolutePath() + " \"Surveyor:* *:E\"");
        return getUriForFile(outputFile);
    }
}
