package io.rapidpro.surveyor.activity;

import android.os.Bundle;
import android.view.View;

import java.text.NumberFormat;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.ui.ViewCache;

public class FlowActivity extends BaseActivity {

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
            Org org = getSurveyor().getOrgService().get(orgUUID);
            Flow flow = org.getFlow(flowUUID);

            String questionString = " Questions";
            if (flow.getQuestionCount() == 1) {
                questionString = " Question";
            }

            ViewCache cache = getViewCache();
            NumberFormat nf = NumberFormat.getInstance();
            cache.setText(R.id.text_flow_name, flow.getName());
            cache.setText(R.id.text_flow_questions, nf.format(flow.getQuestionCount()) + questionString);
            cache.setText(R.id.text_flow_revision, "(v" + nf.format(flow.getRevision()) + ")");

            /*int submissions = Submission.getPendingSubmissionCount(flow);
            if (submissions > 0) {
                cache.show(R.id.container_pending);
                cache.setButtonText(R.id.button_pending, nf.format(submissions));
            } else {
                cache.hide(R.id.container_pending);
            }*/

        } catch (Exception e) {
            e.printStackTrace();
            showBugReportDialog();
            finish();
        }
    }

    public void onActionStart(View view) {
        // TODO
    }

    public void onActionSubmit(View view) {
        // TODO
    }
}
