package io.rapidpro.surveyor;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.jakewharton.threetenabp.AndroidThreeTen;
import io.rapidpro.surveyor.net.TembaService;
import android.text.Editable;
import android.widget.Toast;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import io.rapidpro.surveyor.activity.LoginActivity;
import io.rapidpro.surveyor.activity.OrgListActivity;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.net.TembaService;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class Surveyor extends Application {

    public static String BASE_URL = null;
    public static Logger LOG = new Logger();
    private static Surveyor s_this;

    private SharedPreferences m_prefs = null;
    private TembaService m_tembaService = null;
    private RealmConfiguration m_realmConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        s_this = this;

        try {
            updatePrefs();
        } catch (TembaException e) {
            resetPrefs();
        }

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

    public Realm getRealm() {
        try {
            return Realm.getDefaultInstance();
        } catch (Throwable t) {
            Surveyor.LOG.d("Invalid database, reinstall required");
            Realm.deleteRealm(Surveyor.get().getRealmConfig());
            return Realm.getDefaultInstance();
        }
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

    public void resetPrefs() {
        getPreferences().edit().clear().commit();
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
