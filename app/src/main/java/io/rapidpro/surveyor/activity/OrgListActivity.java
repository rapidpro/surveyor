package io.rapidpro.surveyor.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.fragment.OrgListFragment;

public class OrgListActivity extends BaseActivity implements OrgListFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (isLoggedIn()) {
            List<Org> orgs = new ArrayList<>();
            try {
                orgs = Org.loadAll();
            } catch (IOException e) {
                // TODO
                e.printStackTrace();
            }

            // if we don't have any accounts, take us back to the login screen
            if (orgs.size() == 0) {
                Intent i = new Intent(OrgListActivity.this, LoginActivity.class);

                // if we are logged in, show an error
                i.putExtra(SurveyorIntent.EXTRA_ERROR, getString(R.string.error_no_orgs));

                startActivity(i);
                finish();
                overridePendingTransition(0, 0);
            }
            // if we have access to a single account, then skip selection
            else if (orgs.size() == 1) {
                getSurveyor().LOG.d("One account found, shortcutting: " + orgs.get(0).getName());
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
    public void onFragmentInteraction(Org org) {
        getRapidProService().setToken(org.getToken());
        Intent intent = new Intent(OrgListActivity.this, OrgActivity.class);
        startActivity(intent);
    }
}
