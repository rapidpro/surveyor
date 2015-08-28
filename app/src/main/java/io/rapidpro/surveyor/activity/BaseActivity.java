package io.rapidpro.surveyor.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.net.RapidProService;
import io.realm.Realm;

public class BaseActivity extends AppCompatActivity {

    // our logging tag
    private static String TAG = "Surveyor";

    private DBOrg m_org;
    private DBFlow m_flow;
    private Realm m_realm;

    public Surveyor getSurveyor() {
        return (Surveyor)getApplication();
    }


    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            Realm realm = getRealm();
            realm.beginTransaction();
            realm.clear(DBFlow.class);
            realm.clear(DBOrg.class);
            realm.commitTransaction();
            finish();
            startActivity(new Intent(this, LoginActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onResume() {
        super.onResume();
        Surveyor.LOG.d(getClass().getSimpleName() + ".onResume()");
    }

    protected void onPause() {
        super.onPause();
        if (m_realm != null) {
            m_realm.close();
            m_realm = null;
        }
        Surveyor.LOG.d(getClass().getSimpleName() + ".onPause()");
    }

    public SharedPreferences getPreferences() {
        return getSurveyor().getPreferences();
    }

    public void saveString(int key, String value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(getString(key), value);
        editor.apply();
    }

    public void saveInt(int key, int value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt(getString(key), value);
        editor.apply();
    }

    public String getPreferenceString(int key, String def) {
        return getPreferences().getString(getString(key), def);
    }

    public int getPreferenceInt(int key, int def) {
        return getPreferences().getInt(getString(key), def);
    }


    public Realm getRealm() {
        if (m_realm == null){
            m_realm = Realm.getDefaultInstance();
        }
        return m_realm;
    }

    public RapidProService getRapidProService() {
        return getSurveyor().getRapidProService();
    }

    public DBFlow getFlow() {
        if (m_flow == null) {
            String flowId = getIntent().getStringExtra(SurveyorIntent.EXTRA_FLOW_ID);
            m_flow = getRealm().where(DBFlow.class).equalTo("uuid", flowId).findFirst();
        }
        return m_flow;
    }

    public DBOrg getOrg() {
        if (m_org == null) {
            int orgId = getIntent().getIntExtra(SurveyorIntent.EXTRA_ORG_ID, 0);
            m_org = getRealm().where(DBOrg.class).equalTo("id", orgId).findFirst();
        }
        return m_org;
    }

    public DBFlow getFlow(String id) {
        return getRealm().where(DBFlow.class).equalTo("uuid", id).findFirst();
    }

    public Intent getIntent(Activity from, Class to) {
        Intent intent = new Intent(from, to);
        DBOrg org = getOrg();
        if (org !=null) {
            intent.putExtra(SurveyorIntent.EXTRA_ORG_ID, org.getId());
        }
        return intent;
    }
}
