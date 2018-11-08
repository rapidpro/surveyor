package io.rapidpro.surveyor.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.adapter.OrgListAdapter;
import io.rapidpro.surveyor.data.Org;


public class OrgListFragment extends Fragment implements AbsListView.OnItemClickListener {

    private OnFragmentInteractionListener m_listener;
    private ListView m_listView;
    private ListAdapter m_adapter;

    public OrgListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<Org> items = null;
        try {
            items = Org.loadAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
        m_adapter = new OrgListAdapter(getActivity(), R.layout.item_org, items);
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
            m_listener.onFragmentInteraction((Org) m_adapter.getItem(position));
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Org org);
    }
}
