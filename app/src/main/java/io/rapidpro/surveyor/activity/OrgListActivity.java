package io.rapidpro.surveyor.activity;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import com.greysonparrelli.permiso.Permiso;

import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.fragment.OrgListFragment;

public class OrgListActivity extends BaseActivity implements OrgListFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    if (isLoggedIn()) {
                        // if we don't have any orgs, take us back to the login screen
                        List<DBOrg> orgs = getRealm().where(DBOrg.class).findAll();
                        if (orgs.size() == 0) {
                            Intent i = new Intent(OrgListActivity.this, LoginActivity.class);

                            // if we are logged in, show an error
                            i.putExtra(SurveyorIntent.EXTRA_ERROR, getString(R.string.error_no_orgs));

                            startActivity(i);
                            finish();
                            overridePendingTransition(0, 0);
                        }
                        // if it's a single org, skip our activity
                        else if (orgs.size() == 1) {
                            getSurveyor().LOG.d("One org found, shortcutting: " + orgs.get(0).getName());
                            onFragmentInteraction(orgs.get(0));
                            finish();
                            overridePendingTransition(0, 0);
                        } else {

                            // this holds our org list fragment which shows all orgs in the db
                            setContentView(R.layout.fragment_container);

                            if (savedInstanceState == null) {
                                Fragment fragment = new OrgListFragment();
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                ft.add(R.id.fragment_container, fragment).commit();
                            }
                        }
                    }
                } else {
                    finish();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                OrgListActivity.this.showRationaleDialog(R.string.permission_storage, callback);
            }
        }, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(DBOrg org) {
        getRapidProService().setToken(org.getToken());
        Intent intent = new Intent(OrgListActivity.this, OrgActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_ORG_ID, org.getId());
        startActivity(intent);
    }
}
