package io.rapidpro.surveyor.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import io.rapidpro.surveyor.data.Flow;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

/**
 * Created by eric on 8/18/15.
 */
public class FlowListAdapter extends RealmBaseAdapter<Flow> implements ListAdapter {

    private int m_resourceId;

    public FlowListAdapter(Context context, int resourceId,
                          RealmResults<Flow> realmResults,
                          boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
        m_resourceId = resourceId;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewCache cache = null;

        if (convertView == null) {
            convertView = inflater.inflate(m_resourceId, parent, false);
            cache = new ViewCache();
            cache.titleView = (TextView) convertView.findViewById(android.R.id.text1);
            convertView.setTag(cache);
        } else {
            cache = (ViewCache) convertView.getTag();
        }

        Flow flow = realmResults.get(position);
        cache.titleView.setText(flow.getName() + " " + flow.isFetching());
        return convertView;
    }

    public RealmResults<Flow> getRealmResults() {
        return realmResults;
    }

    public static class ViewCache {
        TextView titleView;
    }
}