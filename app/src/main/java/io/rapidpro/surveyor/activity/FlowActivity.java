package io.rapidpro.surveyor.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;

import java.text.NumberFormat;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.ui.ViewCache;
import io.rapidpro.surveyor.engine.Engine;

/**
 * Home screen for a flow - shows start button and pending submissions
 */
public class FlowActivity extends BaseActivity {

    private Org org;
    private Flow flow;

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
        String orgUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_ORG_UUID);
        String flowUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_FLOW_UUID);

        try {
            org = getSurveyor().getOrgService().get(orgUUID);
            flow = org.getFlow(flowUUID);
        } catch (Exception e) {
            e.printStackTrace();
            showBugReportDialog();
            finish();
        }

        String questionString = " Questions";
        if (flow.getQuestionCount() == 1) {
            questionString = " Question";
        }

        ViewCache cache = getViewCache();
        NumberFormat nf = NumberFormat.getInstance();
        cache.setText(R.id.text_flow_name, flow.getName());
        cache.setText(R.id.text_flow_questions, nf.format(flow.getQuestionCount()) + questionString);
        cache.setText(R.id.text_flow_revision, "(v" + nf.format(flow.getRevision()) + ")");

        int submissions = getSurveyor().getSubmissionService().getPendingCount(org, flow);
        if (submissions > 0) {
            cache.show(R.id.container_pending);
            cache.setButtonText(R.id.button_pending, nf.format(submissions));
        } else {
            cache.hide(R.id.container_pending);
        }
    }

    public void onActionStart(View view) {
        if (!Engine.isSpecVersionSupported(flow.getSpecVersion())) {
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
            Intent intent = new Intent(this, RunActivity.class);
            intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, org.getUuid());
            intent.putExtra(SurveyorIntent.EXTRA_FLOW_UUID, flow.getUuid());
            startActivity(intent);
        }
    }

    public void onActionSubmit(View view) {
        // TODO
    }
}
