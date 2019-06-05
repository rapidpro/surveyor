package io.rapidpro.surveyor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.text.NumberFormat;
import java.util.List;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.ui.ViewCache;

/**
 * Home screen for a flow - shows start button and pending submissions
 */
public class FlowActivity extends BaseSubmissionsActivity {

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

    protected void refresh() {
        String orgUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_ORG_UUID);
        String flowUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_FLOW_UUID);

        try {
            org = getSurveyor().getOrgService().get(orgUUID);
            flow = org.getFlow(flowUUID);
        } catch (Exception e) {
            Logger.e("Unable to load org or flow", e);
            showBugReportDialog();
            finish();
            return;
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

        int pending = getSurveyor().getSubmissionService().getCompletedCount(org, flow);

        cache.setVisible(R.id.container_pending, pending > 0);
        cache.setButtonText(R.id.button_pending, nf.format(pending));
    }

    public void onActionStart(View view) {
        Intent intent = new Intent(this, RunActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, org.getUuid());
        intent.putExtra(SurveyorIntent.EXTRA_FLOW_UUID, flow.getUuid());
        startActivity(intent);
    }

    /**
     * @see BaseSubmissionsActivity#getPendingSubmissions()
     */
    @Override
    protected List<Submission> getPendingSubmissions() {
        return getSurveyor().getSubmissionService().getCompleted(org, flow);
    }

    @Override
    protected Org getOrg() {
        return org;
    }
}
