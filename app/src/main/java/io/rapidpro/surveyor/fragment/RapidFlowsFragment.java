package io.rapidpro.surveyor.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.activity.BaseActivity;
import io.rapidpro.surveyor.adapter.RapidFlowListAdapter;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.net.FlowList;

public class RapidFlowsFragment extends BaseFragment implements AbsListView.OnItemClickListener {

    private RapidFlowListener m_listener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView m_listView;
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
        m_adapter = new RapidFlowListAdapter(getActivity(), android.R.layout.simple_list_item_1, getItems());
    }

    public List<Flow> getItems() {

        // look up our existing flows
        Bundle bundle = getArguments();
        int orgId = bundle.getInt(SurveyorIntent.EXTRA_ORG_ID);
        List<Flow> existing = getRealm().where(Flow.class).equalTo("orgId", orgId).findAllSorted("name");

        // create a quick lookup for existing flows
        Set<String> existingIds = new HashSet<String>();
        for (Flow flow : existing) {
            existingIds.add(flow.getUuid());
        }

        // exclude any flows that are already in our database
        FlowList flowList = ((BaseActivity)getActivity()).getRapidProService().getLastFlows();
        List<Flow> flows = new ArrayList<>();
        if (flowList != null) {
            for (Flow flow : flowList.results) {
                if (!existingIds.contains(flow.getUuid())) {
                    flows.add(flow);
                }
            }
        }

        return flows;
    }

    public int getLayout() {
        return R.layout.fragment_org;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(getLayout(), container, false);

        // Set the adapter
        m_listView = (AbsListView) view.findViewById(android.R.id.list);
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
            m_listener.onRapidFlowSelection((Flow) m_adapter.getItem(position));
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = m_listView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface RapidFlowListener {
        // TODO: Update argument type and name
        public void onRapidFlowSelection(Flow flow);
    }

}

