package io.rapidpro.surveyor;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.File;
import java.io.IOException;

import io.rapidpro.surveyor.net.RapidProService;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class Surveyor extends Application {

    public static String BASE_URL = null;
    public static Logger LOG = new Logger();
    private static Surveyor s_this;

    private SharedPreferences m_prefs = null;
    private RapidProService m_rapidProService = null;
    private RealmConfiguration m_realmConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        s_this = this;
        updatePrefs();

        m_realmConfig = new RealmConfiguration.Builder(this).build();

        // Testing: nuke our db on every start until the schema is ironed out
        // Realm.deleteRealm(config);

        // set our default database config
        Realm.setDefaultConfiguration(m_realmConfig);

        AndroidThreeTen.init(this);
    }

    public static Surveyor get() {
        return s_this;
    }

    public RealmConfiguration getRealmConfig() {
        return m_realmConfig;
    }

    public SharedPreferences getPreferences() {
        if (m_prefs == null) {
            m_prefs = PreferenceManager.getDefaultSharedPreferences(s_this);
        }
        return m_prefs;
    }

    public void updatePrefs() {
        BASE_URL = getPreferences().getString("pref_key_host", getString(R.string.pref_default_host));
        m_rapidProService = null;
    }

    public RapidProService getRapidProService() {
        if (m_rapidProService == null) {
            m_rapidProService = new RapidProService();
        }
        return m_rapidProService;
    }
}
