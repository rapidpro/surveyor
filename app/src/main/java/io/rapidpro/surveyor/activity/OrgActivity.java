package io.rapidpro.surveyor.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.io.File;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.adapter.FlowListAdapter;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.fragment.FlowListFragment;
import io.realm.Realm;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class OrgActivity extends BaseActivity implements FlowListFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DBOrg org = getDBOrg();

        // if we don't know our country yet, fetch it
        if (org.getCountry() == null || org.getCountry().trim().length() == 0) {
            setContentView(R.layout.activity_pending);

            getRapidProService().getOrg(new Callback<DBOrg>() {
                @Override
                public void success(DBOrg latest, Response response) {

                    // save to our database
                    Realm realm = getRealm();
                    realm.beginTransaction();

                    org.setAnonymous(latest.isAnonymous());
                    org.setCountry(latest.getCountry());
                    org.setDateStyle(latest.getDateStyle());
                    org.setPrimaryLanguage(latest.getPrimaryLanguage());
                    org.setTimezone(latest.getTimezone());

                    realm.commitTransaction();

                    // restart our activity
                    startActivity(getIntent());
                    finish();
                    overridePendingTransition(0, 0);
                }

                @Override
                public void failure(RetrofitError error) {
                    finish();
                    overridePendingTransition(0, 0);
                }
            });

        } else {
            setContentView(R.layout.fragment_container);

            setTitle(org.getName());

            getSurveyor().LOG.d("Org: " + org.getTimezone());

            if (savedInstanceState == null) {
                Fragment listFragment = FlowListFragment.newInstance(org.getId());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(R.id.fragment_container, listFragment).commit();

                // if we don't have flows, start download activity


                if (getRealm().where(DBFlow.class).equalTo("org.id", org.getId()).findFirst() == null) {
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
            FlowListAdapter adapter = ((FlowListAdapter) list.getAdapter());
            adapter.notifyDataSetChanged();
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
    public void onFragmentInteraction(DBFlow flow) {
        Intent intent = new Intent(this, ContactActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_FLOW_ID, flow.getUuid());
        startActivity(intent);
    }

    public void showFlowList(MenuItem item) {
        startActivity(getIntent(this, RapidFlowsActivity.class));
    }

    public void confirmSubmissionSending(View view) {

        final DBFlow flow = (DBFlow) view.getTag();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_send_submissions))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        File[] submissions = Submission.getPendingSubmissions(flow);


                        // Flow parsedFlow = RunnerUtil.createFlow(flow);

                        // get the adapter to notify of changes
                        final FlowListAdapter adapter = (FlowListAdapter) ((ListView)findViewById(android.R.id.list)).getAdapter();

                        for (File file : submissions) {
                            Submission.load(file).submit(new Submission.OnSubmitListener() {
                                @Override
                                public void onSuccess() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
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
}
