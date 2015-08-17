package io.rapidpro.surveyor.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;


public class ListItemAdapter<T extends ListItem> extends ArrayAdapter {

    private int resource;
    private Context context;

    public ListItemAdapter(Context context, int resource, List<T> items) {
        super(context, resource, items);
        this.resource = resource;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewCache cache;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(resource, parent, false);

            cache = new ViewCache();
            cache.titleView = (TextView)row.findViewById(android.R.id.text1);

            row.setTag(cache);
        } else {
            cache = (ViewCache)row.getTag();
        }

        T item = (T) getItem(position);
        cache.titleView.setText(item.getName());

        return row;
    }

    public static class ViewCache {
        TextView titleView;
    }
}
