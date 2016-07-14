package io.rapidpro.surveyor.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.text.NumberFormat;

import io.rapidpro.flows.definition.Flow;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.net.Definitions;
import io.rapidpro.surveyor.net.FlowDefinition;
import io.rapidpro.surveyor.ui.BlockingProgress;
import io.rapidpro.surveyor.ui.ViewCache;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class FlowActivity extends BaseActivity {

    private BlockingProgress m_refreshProgress;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        setFlowDetails(getDBFlow());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_flow, menu);
        return true;
    }

    public void setFlowDetails(DBFlow flow){

        String questionString = " Questions";
        if (flow.getQuestionCount() == 1) {
            questionString = " Question";
        }

        ViewCache cache = getViewCache();
        NumberFormat nf = NumberFormat.getInstance();
        cache.setText(R.id.text_flow_name, flow.getName());
        cache.setText(R.id.text_flow_questions, nf.format(flow.getQuestionCount()) + questionString);
        cache.setText(R.id.text_flow_revision, "(v" + nf.format(flow.getRevision()) + ")");

        int submissions = Submission.getPendingSubmissionCount(flow);
        if (submissions > 0) {
            cache.show(R.id.container_pending);
            cache.setButtonText(R.id.button_pending, nf.format(submissions));
        } else {
            cache.hide(R.id.container_pending);
        }

    }

    public void onConfirmFlowRefresh(View view) {

        final DBFlow flow = getDBFlow();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_flow_refresh))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        m_refreshProgress = new BlockingProgress(FlowActivity.this,
                                R.string.refresh_title, R.string.refresh_flow, 1);
                        m_refreshProgress.show();

                        // go fetch our DBFlow definition async
                        getRapidProService().getFlowDefinition(flow, new Callback<Definitions>() {
                            @Override
                            public void onResponse(Call<Definitions> call, Response<Definitions> response) {

                                if (response.isSuccessful()) {
                                    Realm realm = getRealm();
                                    realm.beginTransaction();

                                    Definitions definitions = response.body();

                                    for (FlowDefinition def : definitions.flows) {
                                        if (def.metadata.uuid.equals(flow.getUuid())) {
                                            flow.setRevision(def.metadata.revision);
                                            flow.setName(def.metadata.name);
                                            flow.setQuestionCount(def.rule_sets.size());
                                        }
                                    }

                                    flow.setDefinition(definitions.toString());
                                    realm.commitTransaction();
                                    refresh();

                                    m_refreshProgress.incrementProgressBy(1);
                                    m_refreshProgress.hide();
                                    m_refreshProgress = null;
                                } else {
                                    //
                                    new FetchLegacyDefinition(FlowActivity.this, flow.getUuid(), m_refreshProgress).execute();
                                }
                            }

                            @Override
                            public void onFailure(Call<Definitions> call, Throwable t) {
                                Surveyor.LOG.e("Failure fetching flow", t);
                                m_refreshProgress.hide();
                                m_refreshProgress = null;
                                Toast.makeText(FlowActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();

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

    public void onConfirmPendingSubmissions(View view) {

        final DBFlow flow = getDBFlow();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_send_submissions))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        final File[] submissions = Submission.getPendingSubmissions(flow);

                        final BlockingProgress progress = new BlockingProgress(FlowActivity.this,
                                R.string.submit_title, R.string.submit_body, submissions.length);

                        progress.show();

                        new SubmitSubmissions(FlowActivity.this, submissions, progress).execute();
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

    public void onStartFlow(View view) {
        if (Flow.SPEC_VERSIONS.contains(getDBFlow().getSpecVersion())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.unsupported_version))
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=io.rapidpro.surveyor")));
                            } catch (android.content.ActivityNotFoundException e) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=io.rapidpro.surveyor")));
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
        } else {
            startActivity(getIntent(this, FlowRunActivity.class));
        }
    }


    public void onConfirmDelete(MenuItem item) {
        final DBFlow flow = getDBFlow();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final Realm realm = getRealm();
        realm.beginTransaction();

        final String uuid = flow.getUuid();
        final int orgId = flow.getOrg().getId();

        builder.setMessage(getString(R.string.confirm_flow_delete))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        final BlockingProgress progress = new BlockingProgress(FlowActivity.this,
                                R.string.one_moment, R.string.delete_body, 2);
                        progress.show();

                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                flow.removeFromRealm();
                                progress.incrementProgressBy(1);
                                Submission.deleteFlowSubmissions(orgId, uuid);
                                progress.incrementProgressBy(1);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                realm.commitTransaction();
                                progress.hide();
                                finish();
                            }
                        }.execute();
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
