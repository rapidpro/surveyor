package io.rapidpro.surveyor;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.util.Set;

import io.rapidpro.surveyor.data.OrgService;
import io.rapidpro.surveyor.data.SubmissionService;
import io.rapidpro.surveyor.net.TembaService;
import io.rapidpro.surveyor.utils.SurveyUtils;

/**
 * Main application
 */
public class SurveyorApplication extends Application {

    public static final Logger LOG = new Logger();

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
        LOG.d("SurveyorApplication.onCreate");

        super.onCreate();

        s_this = this;

        tembaService = new TembaService(getTembaHost(), LOG);
        orgService = new OrgService(getOrgsDirectory(), LOG);
        submissionService = new SubmissionService(getSubmissionsDirectory(), LOG);
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
     * @param key   the preference key
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
        clearPreference(SurveyorPreferences.AUTH_USERNAME);
        clearPreference(SurveyorPreferences.AUTH_ORGS);

        tembaService = new TembaService(getTembaHost(), LOG);
    }

    /**
     * Returns the Temba API service
     * @return the service
     */
    public TembaService getTembaService() {
        return tembaService;
    }

    /**
     * Returns the local orgs service
     * @return the service
     */
    public OrgService getOrgService() {
        return orgService;
    }

    /**
     * Returns the local submissions service
     * @return the service
     */
    public SubmissionService getSubmissionService() {
        return submissionService;
    }

    /**
     * Gets the org storage directory
     * @return the directory
     */
    public File getOrgsDirectory() {
        File dir = new File(getFilesDir(), "orgs");
        dir.mkdirs();
        return dir;
    }

    /**
     * Gets the external storage directory
     * @return the directory
     */
    public File getStorageDirectory() {
        return SurveyUtils.mkdir(Environment.getExternalStorageDirectory(), "Surveyor");
    }

    /**
     * Gets the submissions storage directory
     * @return the directory
     */
    private File getSubmissionsDirectory() {
        return SurveyUtils.mkdir(getStorageDirectory(), "submissions");
    }

    /**
     * Gets the URI for the given file using our application's file provider
     * @param file the file
     * @return the URI
     */
    public Uri getUriForFile(File file) {
        return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
    }
}
