package io.rapidpro.surveyor.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import io.rapidpro.flows.runner.Field;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.adapter.FlowListAdapter;
import io.rapidpro.surveyor.data.DBAlias;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBLocation;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.data.OrgDetails;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.fragment.FlowListFragment;
import io.rapidpro.surveyor.net.FlowDefinition;
import io.rapidpro.surveyor.net.RapidProService;
import io.rapidpro.surveyor.ui.BlockingProgress;
import io.realm.Realm;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

public class OrgActivity extends BaseActivity implements FlowListFragment.OnFragmentInteractionListener {

    // progress dialog while we are refreshing org details
    private BlockingProgress m_refreshProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DBOrg org = getDBOrg();

        // if we don't know our country yet, fetch it
        if (org.getCountry() == null || org.getCountry().trim().length() == 0) {
            setContentView(R.layout.activity_pending);
            new FetchOrgData().execute();
        } else {
            setContentView(R.layout.activity_org);
            setTitle(org.getName());

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
        refreshFlowList();
    }

    private void refreshFlowList() {
        FlowListAdapter adapter = (FlowListAdapter)getViewCache().getListViewAdapter(android.R.id.list);
        if (adapter != null){
            adapter.notifyDataSetChanged();
        }

        boolean pending = Submission.getPendingSubmissions().length > 0;
        getViewCache().setVisible(R.id.bottom_options, pending);
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
        Intent intent = new Intent(this, FlowRunActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_FLOW_ID, flow.getUuid());
        startActivity(intent);
    }

    public void showFlowList(MenuItem item) {
        startActivity(getIntent(this, RapidFlowsActivity.class));
    }

    public void confirmRefreshOrg(MenuItem item){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_org_refresh))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        m_refreshProgress = new BlockingProgress(OrgActivity.this,
                                R.string.refresh_title, R.string.refresh_org, 3);
                        m_refreshProgress.show();
                        new FetchOrgData().execute();
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

    public void confirmFlowRefresh(View view) {

        final DBFlow flow = (DBFlow) view.getTag();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_flow_refresh))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        m_refreshProgress = new BlockingProgress(OrgActivity.this,
                                R.string.refresh_title, R.string.refresh_flow, 1);
                        m_refreshProgress.show();

                        // go fetch our DBFlow definition async
                        getRapidProService().getFlowDefinition(flow, new Callback<FlowDefinition>() {
                            @Override
                            public void success(FlowDefinition definition, Response response) {
                                Realm realm = getRealm();
                                realm.beginTransaction();
                                flow.setDefinition(new String(((TypedByteArray) response.getBody()).getBytes()));
                                flow.setRevision(definition.metadata.revision);
                                flow.setName(definition.metadata.name);
                                realm.copyToRealmOrUpdate(flow);
                                realm.commitTransaction();

                                m_refreshProgress.incrementProgressBy(1);
                                m_refreshProgress.hide();
                                m_refreshProgress = null;
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Surveyor.LOG.e("Failure fetching: " + error.getMessage() + " BODY: " + error.getBody(), error.getCause());
                            }
                        });
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

    public void confirmSubmissionSending(View view) {

        final DBFlow flow = (DBFlow) view.getTag();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_send_submissions))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        final File[] submissions = Submission.getPendingSubmissions(flow);

                        final BlockingProgress progress = new BlockingProgress(OrgActivity.this,
                                R.string.submit_title, R.string.submit_body, submissions.length);

                        progress.show();

                        // get the adapter to notify of changes
                        final FlowListAdapter adapter = (FlowListAdapter) ((ListView)findViewById(android.R.id.list)).getAdapter();
                        new SubmitSubmissions(submissions, progress, adapter).execute();
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

    public void onClickSubmit(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_send_all_submissions))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        final File[] submissions = Submission.getPendingSubmissions();

                        final BlockingProgress progress = new BlockingProgress(OrgActivity.this,
                                R.string.submit_title, R.string.submit_body, submissions.length);

                        progress.show();

                        // get the adapter to notify of changes
                        final FlowListAdapter adapter = (FlowListAdapter) ((ListView)findViewById(android.R.id.list)).getAdapter();
                        new SubmitSubmissions(submissions, progress, adapter).execute();
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

    private class SubmitSubmissions extends AsyncTask<String, Void, Void> {

        private File[] m_submissions;
        private FlowListAdapter m_adapter;
        private BlockingProgress m_progress;
        private int m_error;

        public SubmitSubmissions(File[] submissions, BlockingProgress progress, FlowListAdapter toNotify) {
            m_submissions = submissions;
            m_adapter = toNotify;
            m_progress = progress;
        }

        @Override
        protected Void doInBackground(String... params) {

            for (File submission : m_submissions) {
                Submission sub = Submission.load(submission);
                if (sub != null){
                    try {
                        sub.submit();
                    } catch (RetrofitError e) {
                        Surveyor.LOG.e("Failed to submit flow run", e);
                        m_error = getRapidProService().getErrorMessage(e);
                        return null;
                    }
                }

                m_progress.incrementProgressBy(1);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            m_progress.dismiss();

            refreshFlowList();

            if (m_error > 0) {
                Toast.makeText(OrgActivity.this, m_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void incrementProgress() {
        if (m_refreshProgress != null) {
            m_refreshProgress.incrementProgressBy(1);
        }
    }

    private class FetchOrgData extends AsyncTask<String, Void, Void> {

        private int m_error;

        @Override
        protected Void doInBackground(String... params) {

            try {
                RapidProService rapid = getRapidProService();

                // get our database
                Realm realm = Realm.getDefaultInstance();

                int orgId = getIntent().getIntExtra(SurveyorIntent.EXTRA_ORG_ID, 0);
                DBOrg org = realm.where(DBOrg.class).equalTo("id", orgId).findFirst();

                // save the org properties to the database
                DBOrg latest = rapid.getOrg();
                realm.beginTransaction();
                org.setAnonymous(latest.isAnonymous());
                org.setCountry(latest.getCountry());
                org.setDateStyle(latest.getDateStyle());

                if (latest.getPrimaryLanguage() == null) {
                    org.setPrimaryLanguage("base");
                } else {
                    org.setPrimaryLanguage(latest.getPrimaryLanguage());
                }

                org.setTimezone(latest.getTimezone());
                incrementProgress();


                // now go fetch the locations
                List<DBLocation> results = rapid.getLocations();
                for (DBLocation location : results) {
                    location.setOrg(org);

                    // create a composite primary key
                    location.setId(org.getId() + ":" + location.getBoundary());

                    realm.copyToRealmOrUpdate(location);

                    for (String aliasName : location.getAliases()) {
                        DBAlias alias = new DBAlias();

                        // alias gets a composite primary key too
                        alias.setId(location.getId() + ":" + aliasName);

                        alias.setName(aliasName);
                        alias.setLocation(location);
                        realm.copyToRealmOrUpdate(alias);
                    }
                }
                incrementProgress();

                // finally the fields for our org
                List<Field> fields = rapid.getFields();
                OrgDetails details = OrgDetails.load(org);

                if (details != null) {
                    details.setFields(fields);
                    details.save();
                }


                realm.commitTransaction();
                realm.close();
                incrementProgress();

            }
            catch (RetrofitError e) {
                m_error = getRapidProService().getErrorMessage(e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            overridePendingTransition(0, 0);

            if (m_error > 0) {
                Toast.makeText(OrgActivity.this, m_error, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                if (m_refreshProgress == null){
                    finish();
                    startActivity(getIntent());
                }
            }

            if (m_refreshProgress != null && m_refreshProgress.isShowing()) {
                m_refreshProgress.hide();
                m_refreshProgress = null;
            }
        }
    }
}
