package io.rapidpro.surveyor.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Flow;


public class RapidFlowListAdapter extends ArrayAdapter {

    private int resource;
    private Context context;

    public RapidFlowListAdapter(Context context, int resource, List<Flow> items) {
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

        Flow flow = (Flow) getItem(position);
        cache.titleView.setText(flow.getName());

        return row;
    }

    public static class ViewCache {
        TextView titleView;
    }
}