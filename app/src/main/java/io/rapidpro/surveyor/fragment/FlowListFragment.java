package io.rapidpro.surveyor.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.widget.ListView;

import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.adapter.ListItem;
import io.rapidpro.surveyor.adapter.ListItemAdapter;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.net.FlowList;

public class FlowListFragment extends ListItemFragment {

    public FlowListFragment() {}

    public static FlowListFragment newInstance(String orgId) {
        FlowListFragment fragment = new FlowListFragment();
        Bundle args = new Bundle();
        args.putString(SurveyorIntent.EXTRA_ORG_ID, orgId);
        fragment.setArguments(args);
        return fragment;
    }

    public List<? extends ListItem> getItems() {
        Bundle bundle = getArguments();
        String orgId = bundle.getString(SurveyorIntent.EXTRA_ORG_ID);
        return getRealm().where(Flow.class).equalTo("orgId", orgId).findAllSorted("name");
    }

    public int getLayout() {
        return R.layout.fragment_org;
    }

    public void onResume() {
        super.onResume();
        ListView list = (ListView) this.getView().findViewById(R.id.list);
        if (list != null) {
            ListItemAdapter adapter = ((ListItemAdapter) list.getAdapter());
            adapter.notifyDataSetChanged();
        }
    }

}
