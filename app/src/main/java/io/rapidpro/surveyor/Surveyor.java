package io.rapidpro.surveyor;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import io.rapidpro.surveyor.net.TembaService;

public class Surveyor extends Application {

    public static String PREF_HOST = "pref_key_host";
    public static String PREF_CURRENT_ORG = "surveyor.pref.current_org";
    public static String PREF_USERNAME = "surveyor.pref.username";
    public static Logger LOG = new Logger();

    /**
     * The singleton instance of this app
     */
    private static Surveyor s_this;

    private SharedPreferences m_prefs = null;
    private TembaService m_tembaService = null;

    @Override
    public void onCreate() {
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
    public static Surveyor get() {
        return s_this;
    }

    /**
     * Gets the preferences for this app
     * @return the preferences
     */
    public SharedPreferences getPreferences() {
        if (m_prefs == null) {
            m_prefs = PreferenceManager.getDefaultSharedPreferences(s_this);
        }
        return m_prefs;
    }

    public void resetPrefs() {
        getPreferences().edit().clear().apply();
        updatePrefs();
    }

    public void updatePrefs() {
        // re-create our service
        m_tembaService = null;
        getRapidProService();
    }

    public String getHost() {
        return getPreferences().getString(PREF_HOST, getString(R.string.pref_default_host));
    }

    public TembaService getRapidProService(String host) {
        return new TembaService(host);
    }

    public TembaService getRapidProService() {
        if (m_tembaService == null) {
            m_tembaService = getRapidProService(getHost());
        }
        return m_tembaService;
    }
}
