package io.rapidpro.surveyor;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

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

        try {
            updatePrefs();
        } catch (TembaException e) {
            resetPrefs();
        }
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
     * Gets the preferences for this app
     *
     * @return the preferences
     */
    public SharedPreferences getPreferences() {
        return s_this.getSharedPreferences(getPreferencesName(), Context.MODE_PRIVATE);
    }

    public void resetPrefs() {
        getPreferences().edit().clear().apply();
        updatePrefs();
    }

    public void updatePrefs() {
        // get rid of our cached service
        m_tembaService = null;
    }

    /**
     * Gets the base URL of the Temba instance we're connected to
     *
     * @return the base URL
     */
    public String getTembaHost() {
        return getPreferences().getString(SurveyorPreferences.TEMBA_HOST, getString(R.string.pref_default_host));
    }

    /**
     * Returns our Temba service for communicating with a Temba instance
     * @return the temba service
     */
    public TembaService getTembaService() {
        if (m_tembaService == null) {
            m_tembaService = new TembaService(getTembaHost());
        }
        return m_tembaService;
    }
}
