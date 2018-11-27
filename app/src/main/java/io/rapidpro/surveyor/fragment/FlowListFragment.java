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

import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.adapter.FlowListAdapter;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;

/**
 * A list of flows than can be selected from
 */
public class FlowListFragment extends Fragment implements AbsListView.OnItemClickListener {

    private Container container;
    private ListAdapter adapter;

    public FlowListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Org org = container.getOrg();
        List<Flow> items = container.getListItems();

        adapter = new FlowListAdapter(getActivity(), R.layout.item_flow, org, items);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        ListView m_listView = view.findViewById(android.R.id.list);
        m_listView.setAdapter(adapter);
        m_listView.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            container = (Container) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement FlowListFragment.Container");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        container = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        container.onItemClick((Flow) adapter.getItem(position));
    }

    /**
     * Container activity should implement this to be notified when a flow is clicked
     */
    public interface Container {
        Org getOrg();

        List<Flow> getListItems();

        void onItemClick(Flow flow);
    }
}
