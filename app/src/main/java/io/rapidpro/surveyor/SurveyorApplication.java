package io.rapidpro.surveyor;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import io.rapidpro.surveyor.net.TembaService;

public class SurveyorApplication extends Application {

    /**
     * Name of the preferences file (overridden in tests)
     */
    public static String PREFS_NAME = "default";

    public static final Logger LOG = new Logger();

    /**
     * The singleton instance of this app
     */
    private static SurveyorApplication s_this;

    private TembaService m_tembaService = null;

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
     * Gets the singleton instance of this app
     * @return the instance
     */
    public static SurveyorApplication get() {
        return s_this;
    }

    /**
     * Gets the preferences for this app
     * @return the preferences
     */
    public SharedPreferences getPreferences() {
        return s_this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void resetPrefs() {
        getPreferences().edit().clear().apply();
        updatePrefs();
    }

    public void updatePrefs() {
        // get rid of our cached service
        m_tembaService = null;
    }

    public String getHost() {
        return getPreferences().getString(SurveyorPrefs.HOST, getString(R.string.pref_default_host));
    }

    public TembaService getRapidProService() {
        if (m_tembaService == null) {
            m_tembaService = new TembaService(getHost());
        }
        return m_tembaService;
    }
}
