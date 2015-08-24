package io.rapidpro.surveyor.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.fragment.FlowListFragment;
import io.rapidpro.surveyor.fragment.OrgListFragment;

public class OrgListActivity extends BaseActivity implements OrgListFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if we don't have any orgs, take them back to the login screen
        if (getRealm().where(Org.class).findAll().size() == 0) {
            Intent i = new Intent(OrgListActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        }

        // this holds our org list fragment which shows all orgs in the db
        setContentView(R.layout.fragment_container);

        if (savedInstanceState == null) {
            Fragment fragment = new OrgListFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.fragment_container, fragment).commit();
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
        intent.putExtra(SurveyorIntent.EXTRA_ORG_ID, org.getId());
        startActivity(intent);
    }
}
