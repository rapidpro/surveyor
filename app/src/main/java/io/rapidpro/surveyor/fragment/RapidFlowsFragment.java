package io.rapidpro.surveyor.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.activity.BaseActivity;
import io.rapidpro.surveyor.adapter.RapidFlowListAdapter;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.net.FlowList;

public class RapidFlowsFragment extends BaseFragment implements AbsListView.OnItemClickListener {

    private RapidFlowListener m_listener;

    /**
     * The fragment's ListView/GridView.
     */
    private ListView m_listView;
    private RapidFlowListAdapter m_adapter;

    public RapidFlowsFragment() {}

    public static RapidFlowsFragment newInstance(int orgId) {
        RapidFlowsFragment fragment = new RapidFlowsFragment();
        Bundle args = new Bundle();
        args.putInt(SurveyorIntent.EXTRA_ORG_ID, orgId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_adapter = new RapidFlowListAdapter(getActivity(), R.layout.item_flow, getItems());
    }

    public List<DBFlow> getItems() {

        // look up our existing DBFlows
        Bundle bundle = getArguments();
        int orgId = bundle.getInt(SurveyorIntent.EXTRA_ORG_ID);
        List<DBFlow> existing = getRealm().where(DBFlow.class).equalTo("org.id", orgId).findAllSorted("name");

        // create a quick lookup for existing DBFlows
        Map<String,Integer> existingIds = new HashMap<>();
        for (DBFlow flow : existing) {
            existingIds.put(flow.getUuid(), flow.getRevision());
        }

        // exclude any DBFlows that are already in our database
        FlowList flowList = ((BaseActivity)getActivity()).getRapidProService().getLastFlows();
        List<DBFlow> dbFlows = new ArrayList<>();
        if (flowList != null) {
            for (DBFlow newFlow : flowList.results) {

                if (!existingIds.keySet().contains(newFlow.getUuid())) {
                    dbFlows.add(newFlow);
                }
            }
        }

        return dbFlows;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list, container, false);

        // Set the adapter
        m_listView = (ListView) view.findViewById(android.R.id.list);

        ViewGroup header = (ViewGroup)inflater.inflate(R.layout.header_flow, m_listView, false);
        m_listView.addHeaderView(header, null, false);
        m_listView.setAdapter(m_adapter);

        // Set OnItemClickListener so we can be notified on item clicks
        m_listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            m_listener = (RapidFlowListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        m_listener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != m_listener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            m_listener.onRapidFlowSelection((DBFlow) m_adapter.getItem(position - 1));
        }
    }

    public interface RapidFlowListener {
        // TODO: Update argument type and name
        public void onRapidFlowSelection(DBFlow flow);
    }

}

