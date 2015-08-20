package io.rapidpro.surveyor;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import io.rapidpro.surveyor.net.RapidProService;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class Surveyor extends Application {

    public static String BASE_URL = null;
    private static SharedPreferences s_prefs = null;
    private static RapidProService s_rapidProService = null;

    public static Logger LOG = new Logger();

    @Override
    public void onCreate() {
        super.onCreate();
        updatePrefs();

        Realm.deleteRealm(getRealConfiguration());
    }

    public static SharedPreferences getPreferences(Context context) {
        if (s_prefs == null) {
            s_prefs = context.getSharedPreferences(context.getString(R.string.preference_key_file), Context.MODE_PRIVATE);
        }
        return s_prefs;
    }

    public void updatePrefs() {
        SharedPreferences prefs = Surveyor.getPreferences(this);
        BASE_URL = prefs.getString(getString(R.string.pref_key_server), getString(R.string.pref_default_server));
    }

    public RapidProService getRapidProService() {
        if (s_rapidProService == null) {
            s_rapidProService = new RapidProService(this);
        }
        return s_rapidProService;
    }

    public RealmConfiguration getRealConfiguration() {
        return new RealmConfiguration.Builder(this).build();
    }

    public Realm getRealm() {
        return Realm.getInstance(getRealConfiguration());
    }
}
