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

import java.text.NumberFormat;
import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.adapter.FlowListAdapter;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.fragment.FlowListFragment;
import io.rapidpro.surveyor.task.RefreshOrgTask;
import io.rapidpro.surveyor.ui.BlockingProgress;
import io.rapidpro.surveyor.ui.ViewCache;


public class OrgActivity extends BaseActivity implements FlowListFragment.Container {

    private Org getOrg() {
        String orgUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_ORG_UUID);

        try {
            return getSurveyor().getOrgService().get(orgUUID);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Org org = getOrg();
        if (org == null) {
            showBugReportDialog();
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        refresh();
    }

    private void refresh() {
        FlowListAdapter adapter = (FlowListAdapter) getViewCache().getListViewAdapter(android.R.id.list);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        int pending = getSurveyor().getSubmissionService().getPendingCount(getOrg());
        ViewCache cache = getViewCache();
        cache.setVisible(R.id.container_pending, pending > 0);
        cache.setButtonText(R.id.button_pending, NumberFormat.getInstance().format(pending));
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
        final BlockingProgress progressModal = new BlockingProgress(OrgActivity.this, R.string.one_moment, R.string.refresh_org, 3);
        progressModal.show();

        new RefreshOrgTask(new RefreshOrgTask.RefreshOrgListener() {
            @Override
            public void onProgress(int percent) {
                progressModal.setProgress(percent);
            }

            @Override
            public void onComplete() {
                refresh();

                progressModal.dismiss();
            }

            @Override
            public void onFailure() {
                progressModal.dismiss();

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

    /**
     * @see FlowListFragment.Container#getListItems()
     */
    @Override
    public List<Flow> getListItems() {
        return getOrg().getFlows();
    }

    /**
     * @see FlowListFragment.Container#onItemClick(Flow)
     */
    @Override
    public void onItemClick(Flow flow) {
        Intent intent = new Intent(this, FlowActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, getOrg().getUuid());
        intent.putExtra(SurveyorIntent.EXTRA_FLOW_UUID, flow.getUuid());
        startActivity(intent);
    }
}
