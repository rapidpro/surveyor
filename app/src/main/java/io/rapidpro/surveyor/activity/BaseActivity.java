package io.rapidpro.surveyor.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.net.RapidProService;
import io.rapidpro.surveyor.ui.ViewCache;
import io.realm.Realm;

/**
 * All activities for the Surveyor app extend BaseActivity
 * which provides convenience methods for transferring state
 * between activities and the like.
 */
public class BaseActivity extends AppCompatActivity {

    // our logging tag
    private static String TAG = "Surveyor";

    private DBOrg m_org;
    private DBFlow m_flow;
    private ViewCache m_viewCache;

    private Realm m_realm;

    public Surveyor getSurveyor() {
        return (Surveyor)getApplication();
    }


    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
    }

    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
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

            PreferenceManager.getDefaultSharedPreferences(this).edit().remove(SurveyorIntent.PREF_LOGGED_IN).commit();

            Submission.clear();

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

    public ViewCache getViewCache() {
        if (m_viewCache == null) {
            m_viewCache = new ViewCache(findViewById(android.R.id.content));
        }
        return m_viewCache;
    }

    public void refresh() {

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

    public DBFlow getDBFlow() {
        if (m_flow == null) {
            String flowId = getIntent().getStringExtra(SurveyorIntent.EXTRA_FLOW_ID);
            if (flowId != null) {
                m_flow = getRealm().where(DBFlow.class).equalTo("uuid", flowId).findFirst();
            }
        }
        return m_flow;
    }

    public DBOrg getDBOrg() {
        if (m_org == null) {
            int orgId = getIntent().getIntExtra(SurveyorIntent.EXTRA_ORG_ID, 0);
            m_org = getRealm().where(DBOrg.class).equalTo("id", orgId).findFirst();
        }
        return m_org;
    }

    public Intent getIntent(Activity from, Class to) {
        Intent intent = new Intent(from, to);
        DBOrg org = getDBOrg();
        if (org !=null) {
            intent.putExtra(SurveyorIntent.EXTRA_ORG_ID, org.getId());
        }

        DBFlow flow = getDBFlow();
        if (flow != null) {
            intent.putExtra(SurveyorIntent.EXTRA_FLOW_ID, flow.getUuid());
        }

        return intent;
    }

    public AlertDialog showAlert(int title, int body) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setIcon(android.R.drawable.ic_dialog_alert).create();

        dialog.show();
        return dialog;

    }
}
