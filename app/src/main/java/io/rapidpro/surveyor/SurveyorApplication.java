package io.rapidpro.surveyor;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

import io.rapidpro.surveyor.net.TembaService;

/**
 * Main application
 */
public class SurveyorApplication extends Application {

    public static final Logger LOG = new Logger();

    /**
     * The singleton instance of this application
     */
    private static SurveyorApplication s_this;

    private TembaService m_tembaService = null;

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
        m_tembaService = new TembaService(getTembaHost());
    }

    /**
     * Gets the name of the preferences file (this is a method so it can be overridden for testing)
     *
     * @return the name of the preferences
     */
    protected String getPreferencesName() {
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
        return getPreferences().getString(SurveyorPreferences.HOST, getString(R.string.pref_default_host));
    }

    /**
     * Called when our host setting has changed
     */
    public void onTembaHostChange() {
        m_tembaService = new TembaService(getTembaHost());

        clearPreference(SurveyorPreferences.AUTH_USERNAME);
        clearPreference(SurveyorPreferences.AUTH_ORGS);
    }

    /**
     * Returns the Temba API service
     * @return the service
     */
    public TembaService getTembaService() {
        return m_tembaService;
    }
}
