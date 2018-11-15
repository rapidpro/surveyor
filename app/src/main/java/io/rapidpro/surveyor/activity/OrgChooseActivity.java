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
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.fragment.OrgListFragment;

/**
 * Let's the user select one of the orgs they have access to
 */
public class OrgChooseActivity extends BaseActivity implements OrgListFragment.Container {

    private List<Org> cachedOrgs;

    private List<Org> getOrgs() {
        if (this.cachedOrgs == null) {
            try {
                this.cachedOrgs = Org.loadAll();

                SurveyorApplication.LOG.d("Loaded " + cachedOrgs.size() + " orgs");
            } catch (IOException e) {
                SurveyorApplication.LOG.e("Error loading orgs", e);
            }
        }
        return cachedOrgs;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<Org> orgs = getOrgs();

        // if we don't have any orgs, take us back to the login screen
        if (orgs == null || orgs.size() == 0) {
            logout();

            overridePendingTransition(0, 0);
        }
        // if we have access to a single org, then skip this entire activity
        else if (orgs.size() == 1) {
            SurveyorApplication.LOG.d("One org found, shortcutting chooser to: " + orgs.get(0).getName());
            showOrg(orgs.get(0));
            finish();
            overridePendingTransition(0, 0);
        } else {

            // this holds our org list fragment which shows all available orgs
            setContentView(R.layout.activity_org_choose);

            if (savedInstanceState == null) {
                Fragment fragment = new OrgListFragment();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(R.id.fragment_container, fragment).commit();
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

    /**
     * @see OrgListFragment.Container#getListItems()
     */
    @Override
    public List<Org> getListItems() {
        return getOrgs();
    }

    /**
     * @see OrgListFragment.Container#onItemClick(Org)
     */
    @Override
    public void onItemClick(Org org) {
        showOrg(org);
    }

    private void showOrg(Org org) {
        Intent intent = new Intent(OrgChooseActivity.this, OrgActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, org.getUuid());
        startActivity(intent);
    }
}
