package io.rapidpro.surveyor.fragment;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.activity.BaseActivity;
import io.rapidpro.surveyor.adapter.ListItem;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.net.FlowList;

public class RapidFlowsFragment extends ListItemFragment {

    public RapidFlowsFragment() {}

    public static RapidFlowsFragment newInstance(String orgId) {
        RapidFlowsFragment fragment = new RapidFlowsFragment();
        Bundle args = new Bundle();
        args.putString(SurveyorIntent.EXTRA_ORG_ID, orgId);
        fragment.setArguments(args);
        return fragment;
    }

    public List<? extends ListItem> getItems() {

        // look up our existing flows
        Bundle bundle = getArguments();
        String orgId = bundle.getString(SurveyorIntent.EXTRA_ORG_ID);
        List<Flow> existing = getRealm().where(Flow.class).equalTo("orgId", orgId).findAllSorted("name");

        // create a quick lookup for existing flows
        Set<String> existingIds = new HashSet<String>();
        for (Flow flow : existing) {
            existingIds.add(flow.getId());
        }

        // exclude any flows that are already in our database
        FlowList flowList = ((BaseActivity)getActivity()).getRapidProService().getLastFlows();
        List<Flow> flows = new ArrayList<>();
        if (flowList != null) {
            for (Flow flow : flowList.results) {
                if (!existingIds.contains(flow.getId())) {
                    flows.add(flow);
                }
            }
        }

        return flows;
    }

    public int getLayout() {
        return R.layout.fragment_org;
    }

}
