package io.rapidpro.surveyor.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.adapter.FlowListAdapter;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.fragment.FlowListFragment;
import io.rapidpro.surveyor.net.FlowDefinition;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class OrgActivity extends BaseActivity implements FlowListFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);

        Org org = getOrg();

        if (org == null) {
            Toast.makeText(this, R.string.error_org_missing, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            setTitle(org.getName());

            if (savedInstanceState == null) {
                Fragment listFragment = FlowListFragment.newInstance(org.getId());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(R.id.fragment_container, listFragment).commit();

                // if we don't have flows, start download activity
                if (getRealm().where(Flow.class).equalTo("orgId", org.getId()).findFirst() == null) {
                    startActivity(getIntent(OrgActivity.this, RapidFlowsActivity.class));
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ListView list = (ListView) findViewById(android.R.id.list);
        if (list != null) {
            // FlowListAdapter adapter = ((FlowListAdapter) list.getAdapter());
            // adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_org, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Flow flow) {
        Surveyor.LOG.d("Flow: " + flow.getDefinition());
        startActivity(new Intent(this, FlowRunActivity.class));
    }

    public void showFlowList(MenuItem item) {
        startActivity(getIntent(this, RapidFlowsActivity.class));
    }
}
