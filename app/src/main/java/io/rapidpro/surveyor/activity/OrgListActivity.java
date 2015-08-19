package io.rapidpro.surveyor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Org;
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
        setContentView(R.layout.activity_org_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_org_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

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
