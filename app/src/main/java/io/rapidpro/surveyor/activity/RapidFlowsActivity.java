package io.rapidpro.surveyor.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBOrg;
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

        setContentView(R.layout.activity_pending);

        final DBOrg org = getOrg();
        Surveyor.LOG.d("Fetching flows for " + org.getName());


        getRapidProService().getFlows(new Callback<FlowList>() {
            @Override
            public void success(FlowList flows, Response response) {
                if (!RapidFlowsActivity.this.isDestroyed()) {
                    setContentView(R.layout.fragment_container);
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
    public void onRapidFlowSelection(final DBFlow flow) {

        Surveyor.LOG.d("DBFlow selected: " + flow.getName());

        // save which org this DBFlow came from
        flow.setOrg(getOrg());

        final Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealm(flow);
        realm.commitTransaction();
        finish();

        // go fetch our DBFlow defintion async
        getRapidProService().getFlowDefinition(flow, new Callback<FlowDefinition>() {
            @Override
            public void success(FlowDefinition flowDefinition, Response response) {
                realm.beginTransaction();
                flow.setDefinition("{\"flow\":" + flowDefinition.results.toString() + "}");
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
