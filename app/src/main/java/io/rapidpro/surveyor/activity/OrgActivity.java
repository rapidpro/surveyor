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
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.FlowSummary;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.fragment.FlowListFragment;
import io.rapidpro.surveyor.task.RefreshOrgTask;
import io.rapidpro.surveyor.ui.BlockingProgress;


public class OrgActivity extends BaseActivity implements FlowListFragment.Container {

    // progress dialog while we are refreshing org details
    private BlockingProgress m_refreshProgress;

    private Org org;

    private Org getOrg() {
        if (this.org == null) {
            String orgUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_ORG_UUID);

            try {
                this.org = Org.load(orgUUID, true);

                SurveyorApplication.LOG.d("Loaded org " + orgUUID);
            } catch (IOException e) {
                SurveyorApplication.LOG.e("Error loading org " + orgUUID, e);

                e.printStackTrace();
                finish();
            }
        }
        return this.org;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Org org = getOrg();
        if (org == null) {
            finish();
            return;
        }

        setTitle(org.getName());

        if (!org.hasAssets()) {
            confirmRefreshOrg(true);
        }

        // this holds our flow list fragment which shows all available flows
        setContentView(R.layout.activity_org);

        if (savedInstanceState == null) {
            Fragment fragment = new FlowListFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.fragment_container, fragment).commit();
        }

        /*final DBOrg org = getDBOrg();

        // if we don't know our timezone yet, fetch it
        if (org.getTimezone() == null || org.getTimezone().trim().length() == 0) {
            setContentView(R.layout.activity_pending);
            new FetchOrgData().execute();
        } else {

            setTitle(org.getName());

            if (savedInstanceState == null) {
                Fragment listFragment = FlowListFragment.newInstance(org.getId());
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(R.id.activity_org_choose, listFragment).commit();

                // if we don't have flows, start download activity
                if (getRealm().where(DBFlow.class).equalTo("org.id", org.getId()).findFirst() == null) {
                    startActivity(getIntent(OrgActivity.this, RapidFlowsActivity.class));
                }
            }
        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        /*FlowListAdapter adapter = (FlowListAdapter) getViewCache().getListViewAdapter(android.R.id.list);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        int pending = Submission.getPendingSubmissions(getDBOrg().getId()).length;
        ViewCache cache = getViewCache();
        cache.setVisible(R.id.container_pending, pending > 0);
        cache.setButtonText(R.id.button_pending, NumberFormat.getInstance().format(pending));*/

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

    public void onActionRefresh(MenuItem item) {
        confirmRefreshOrg(false);
    }

    public void confirmRefreshOrg(boolean initial) {
        int msgId = initial ? R.string.confirm_org_download : R.string.confirm_org_refresh;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(msgId))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        doRefresh();
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    protected void doRefresh() {
        m_refreshProgress = new BlockingProgress(OrgActivity.this, R.string.one_moment, R.string.refresh_org, 3);
        m_refreshProgress.show();

        new RefreshOrgTask(new RefreshOrgTask.RefreshOrgListener() {
            @Override
            public void onProgress(int percent) {
                m_refreshProgress.setProgress(percent);
            }

            @Override
            public void onComplete() {
                m_refreshProgress.dismiss();
                m_refreshProgress = null;
            }

            @Override
            public void onFailure() {
                m_refreshProgress.dismiss();
                m_refreshProgress = null;

                Toast.makeText(OrgActivity.this, getString(R.string.error_org_refresh), Toast.LENGTH_SHORT).show();
            }
        }).execute(getOrg());
    }

    public void onClickSubmit(View view) {
        SurveyorApplication.LOG.d("Clicked on submit..");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_send_all_submissions))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        // TODO
                        /* final File[] submissions = Submission.getPendingSubmissions(getDBOrg().getId());

                        final BlockingProgress progress = new BlockingProgress(OrgActivity.this,
                                R.string.submit_title, R.string.submit_body, submissions.length);
                        progress.show();

                        new SubmitSubmissions(OrgActivity.this, submissions, progress).execute();*/
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

    @Override
    public List<FlowSummary> getFlowItems() {
        return getOrg().getFlows();
    }

    @Override
    public void onFlowClick(FlowSummary flow) {
        //Intent intent = new Intent(this, FlowActivity.class);
        //intent.putExtra(SurveyorIntent.EXTRA_FLOW_ID, flow.getUuid());
        //startActivity(intent);
    }
}
