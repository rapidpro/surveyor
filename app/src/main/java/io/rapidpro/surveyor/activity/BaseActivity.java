package io.rapidpro.surveyor.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ShareCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.greysonparrelli.permiso.Permiso;
import com.greysonparrelli.permiso.PermisoActivity;

import java.io.File;

import io.rapidpro.surveyor.BuildConfig;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.net.TembaService;
import io.rapidpro.surveyor.ui.ViewCache;

/**
 * All activities for the Surveyor app extend BaseActivity
 * which provides convenience methods for transferring state
 * between activities and the like.
 */
public class BaseActivity extends PermisoActivity {

    private Org m_org;
    private ViewCache m_viewCache;

    public Surveyor getSurveyor() {
        return (Surveyor) getApplication();
    }

    public boolean validateLogin() {
        return true;
    }

    /**
     * Logs in a user for the given orgs
     */
    public void login(String email) {
        // save email which we'll need for submissions later
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Surveyor.PREF_USERNAME, email);
        editor.apply();

        // run account selection activity
        startActivity(new Intent(this, OrgChooseActivity.class));
        finish();
    }

    public void logout() {
        logout(-1);
    }


    /**
     * Logs the user out and returns them to the login page
     */
    public void logout(int error) {

        // TODO
        // Submission.clear();
        // Org.clear();

        Intent intent = new Intent(this, LoginActivity.class);
        if (error != -1) {
            intent.putExtra(SurveyorIntent.EXTRA_ERROR, getString(error));
        }
        startActivity(intent);

        finish();
    }

    public String getUsername() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(Surveyor.PREF_USERNAME, null);
    }

    public boolean isLoggedIn() {
        return getUsername() != null;
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);

        // check if they are properly logged in
        if (validateLogin() && !isLoggedIn()) {
            logout();
        }
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

        // show the settings menu always in debug mode
        if (BuildConfig.DEBUG) {
            MenuItem menuItem = menu.findItem(R.id.action_settings);
            if (menuItem != null) {
                menuItem.setVisible(true);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        } else if (id == R.id.action_debug) {
            sendBugReport();
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendBugReport() {

        // Log our build and device details
        StringBuilder info = new StringBuilder();
        info.append("Version: " + BuildConfig.VERSION_NAME + "; " + BuildConfig.VERSION_CODE);
        info.append("\n  OS: " + System.getProperty("os.version") + " (API " + Build.VERSION.SDK_INT + ")");
        info.append("\n  Model: " + android.os.Build.MODEL + " (" + android.os.Build.DEVICE + ")");
        Surveyor.LOG.d(info.toString());

        // Generate a logcat file
        File outputFile = new File(Environment.getExternalStorageDirectory(), "surveyor-debug.txt");

        try {
            Runtime.getRuntime().exec("logcat -d -f " + outputFile.getAbsolutePath() + "  \"*:E Surveyor:*\" ");
        } catch (Throwable t) {
            Surveyor.LOG.e("Failed to generate report", t);
        }

        ShareCompat.IntentBuilder.from(this)
                .setType("message/rfc822")
                .addEmailTo("support@rapidpro.io")
                .setSubject("Surveyor Bug Report")
                .setText("Please include what you were doing prior to sending this report and specific details on the error you encountered.")
                .setStream(Uri.fromFile(outputFile))
                .setChooserTitle("Send Email")
                .startChooser();
    }

    public void showSendBugReport() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_bug_report))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        sendBugReport();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    public ViewCache getViewCache() {
        if (m_viewCache == null) {
            m_viewCache = new ViewCache(this, findViewById(android.R.id.content));
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

    public TembaService getRapidProService() {
        return getSurveyor().getRapidProService();
    }

    public AlertDialog showAlert(int title, int body) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body)
                .setIcon(android.R.drawable.ic_dialog_alert).create();

        dialog.show();
        return dialog;
    }

    public void showRationaleDialog(int body, Permiso.IOnRationaleProvided callback) {
        Permiso.getInstance().showRationaleInDialog(getString(R.string.title_permissions), getString(body), null, callback);
    }
}
