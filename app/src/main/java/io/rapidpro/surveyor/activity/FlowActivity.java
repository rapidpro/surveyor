package io.rapidpro.surveyor.activity;

import android.os.Bundle;
import android.view.View;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;

public class FlowActivity extends BaseActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String orgUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_ORG_UUID);
        String flowUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_FLOW_UUID);

        setContentView(R.layout.activity_flow);
    }

    @Override
    protected void onResume() {
        super.onResume();

        refresh();
    }

    public void refresh() {
        //setFlowDetails(getDBFlow());
    }

    public void onActionStart(View view) {
        // TODO
    }

    public void onActionSubmit(View view) {
        // TODO
    }
}
