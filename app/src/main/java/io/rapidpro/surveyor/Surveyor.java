package io.rapidpro.surveyor;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import io.rapidpro.surveyor.net.TembaService;

public class Surveyor extends Application {

    public static String BASE_URL = null;
    public static Logger LOG = new Logger();
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

    public static Surveyor get() {
        return s_this;
    }

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
        BASE_URL = getPreferences().getString("pref_key_host", getString(R.string.pref_default_host));
        m_tembaService = null;

        // try to create our accessor
        getRapidProService();
    }

    public TembaService getRapidProService(String host) {
        return new TembaService(host);
    }

    public TembaService getRapidProService() {
        if (m_tembaService == null) {
            m_tembaService = getRapidProService(Surveyor.BASE_URL);
        }
        return m_tembaService;
    }
}
