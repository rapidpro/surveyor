package io.rapidpro.surveyor;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import io.rapidpro.surveyor.net.TembaService;

public class Surveyor extends Application {

    /**
     * RapidPro host we are connected to
     */
    public static String PREF_HOST = "pref_key_host";

    /**
     * Username/email we are logged in as. If this is set, we are logged in
     */
    public static String PREF_AUTH_USERNAME = "surveyor.pref.auth_username";

    /**
     * Username/email we were previously logged in as - used to prepopulate login form
     */
    public static String PREF_PREV_USERNAME = "surveyor.pref.prev_username";

    /**
     * UUIDs of the orgs this user has access to
     */
    public static String PREF_AUTH_ORGS = "surveyor.pref.auth_orgs";

    public static Logger LOG = new Logger();

    /**
     * The singleton instance of this app
     */
    private static Surveyor s_this;

    private TembaService m_tembaService = null;

    @Override
    public void onCreate() {
        super.onCreate();
        s_this = this;

        Surveyor.LOG.d("Surveyor.onCreate");

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
        return PreferenceManager.getDefaultSharedPreferences(s_this);
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

    public TembaService getRapidProService() {
        if (m_tembaService == null) {
            m_tembaService = new TembaService(getHost());
        }
        return m_tembaService;
    }
}
