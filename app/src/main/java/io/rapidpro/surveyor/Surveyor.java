package io.rapidpro.surveyor;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import io.rapidpro.surveyor.net.RapidProService;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class Surveyor extends Application {

    public static String BASE_URL = null;
    public static Logger LOG = new Logger();
    private static Surveyor s_this;

    private SharedPreferences m_prefs = null;
    private RapidProService m_rapidProService = null;

    @Override
    public void onCreate() {
        super.onCreate();
        s_this = this;
        updatePrefs();

        // Temporary: nuke our db on every start until the schema is ironed out
        RealmConfiguration config = new RealmConfiguration.Builder(this).build();
        Realm.deleteRealm(config);

        // set our default database config
        Realm.setDefaultConfiguration(config);
    }

    public SharedPreferences getPreferences() {
        if (m_prefs == null) {
            m_prefs = PreferenceManager.getDefaultSharedPreferences(s_this);
        }
        return m_prefs;
    }

    public void updatePrefs() {
        BASE_URL = getPreferences().getString("pref_key_host", null);
        m_rapidProService = null;
    }

    public RapidProService getRapidProService() {
        if (m_rapidProService == null) {
            m_rapidProService = new RapidProService();
        }
        return m_rapidProService;
    }
}
