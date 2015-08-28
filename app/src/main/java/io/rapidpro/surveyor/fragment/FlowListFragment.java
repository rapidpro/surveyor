package io.rapidpro.surveyor.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.adapter.FlowListAdapter;
import io.rapidpro.surveyor.data.DBFlow;
import io.realm.RealmResults;


public class FlowListFragment extends BaseFragment implements AbsListView.OnItemClickListener {

    private OnFragmentInteractionListener m_listener;

    private ListView m_listView;
    private ListAdapter m_adapter;

    public FlowListFragment() {}

    public static FlowListFragment newInstance(int orgId) {
        FlowListFragment fragment = new FlowListFragment();
        Bundle args = new Bundle();
        args.putInt(SurveyorIntent.EXTRA_ORG_ID, orgId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_adapter = new FlowListAdapter(getActivity(), R.layout.item_flow_downloaded, getItems(), true);
    }

    public RealmResults<DBFlow> getItems() {
        int orgId = getArguments().getInt(SurveyorIntent.EXTRA_ORG_ID);
        return getRealm().where(DBFlow.class).equalTo("orgId", orgId).findAllSorted("name");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list, container, false);

        // Set the adapter
        m_listView = (ListView) view.findViewById(android.R.id.list);
        m_listView.setAdapter(m_adapter);

        // Set OnItemClickListener so we can be notified on item clicks
        m_listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            m_listener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
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
            m_listener.onFragmentInteraction((DBFlow) m_adapter.getItem(position));
        }
    }

    public void setEmptyText(CharSequence emptyText) {
        View emptyView = m_listView.getEmptyView();
        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(DBFlow flow);
    }

}
