package io.rapidpro.surveyor.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.adapter.RapidFlowListAdapter;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.fragment.FlowListFragment;
import io.rapidpro.surveyor.fragment.OrgListFragment;
import io.rapidpro.surveyor.fragment.RapidFlowsFragment;
import io.rapidpro.surveyor.net.FlowDefinition;
import io.rapidpro.surveyor.net.FlowList;
import io.realm.Realm;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class RapidFlowsActivity extends BaseActivity implements RapidFlowsFragment.RapidFlowListener {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_flow_list_pending);

        final Org org = getOrg();
        Surveyor.LOG.d("Fetching flows for " + org.getName());

        getRapidProService().getFlows(new Callback<FlowList>() {
            @Override
            public void success(FlowList flows, Response response) {
                if (!RapidFlowsActivity.this.isDestroyed()) {
                    setContentView(R.layout.activity_flow_list);
                    if (savedInstanceState == null) {
                        Fragment listFragment = RapidFlowsFragment.newInstance(org.getId());
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.add(R.id.fragment_container, listFragment).commit();
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Surveyor.LOG.e("Boom: " + error.getMessage(), error.getCause());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_flow_list, menu);
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
    public void onRapidFlowSelection(final Flow flow) {

        Surveyor.LOG.d("Flow selected: " + flow.getName());

        // save which org this flow came from
        flow.setOrgId(getOrg().getId());

        final Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealm(flow);
        realm.commitTransaction();
        finish();

        // go fetch our flow defintion async
        getRapidProService().getFlowDefinition(flow, new Callback<FlowDefinition>() {
            @Override
            public void success(FlowDefinition flowDefinition, Response response) {
                realm.beginTransaction();
                flow.setDefinition(flowDefinition.results.toString());
                realm.copyToRealmOrUpdate(flow);
                realm.commitTransaction();
            }

            @Override
            public void failure(RetrofitError error) {
                Surveyor.LOG.e("Failure fetching: " + error.getMessage() + " BODY: " + error.getBody(), error.getCause());
            }
        });

    }
}
