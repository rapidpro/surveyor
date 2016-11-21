package io.rapidpro.surveyor.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.data.DBOrg;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

public class OrgListAdapter extends RealmBaseAdapter<DBOrg> implements ListAdapter {

    private int m_resourceId;

    public OrgListAdapter(Context context, int resourceId,
                     RealmResults<DBOrg> realmResults,
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
            cache.titleView = (TextView) convertView.findViewById(R.id.text_org);
            convertView.setTag(cache);
        } else {
            cache = (ViewCache) convertView.getTag();
        }

        DBOrg org = realmResults.get(position);
        cache.titleView.setText(org.getName());
        return convertView;
    }

    public static class ViewCache {
        TextView titleView;
    }
}