package io.rapidpro.surveyor.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.data.DBFlow;


public class RapidFlowListAdapter extends ArrayAdapter {

    private int m_resource;
    private Context m_context;

    public RapidFlowListAdapter(Context context, int resource, List<DBFlow> items) {
        super(context, resource, items);
        m_resource = resource;
        m_context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewCache cache;

        if(row == null) {
            LayoutInflater inflater = ((Activity)m_context).getLayoutInflater();
            row = inflater.inflate(m_resource, parent, false);

            cache = new ViewCache();
            cache.titleView = (TextView)row.findViewById(R.id.text_flow_name);
            cache.questionView = (TextView)row.findViewById(R.id.text_flow_questions);

            row.setTag(cache);
        } else {
            cache = (ViewCache)row.getTag();
        }

        DBFlow flow = (DBFlow) getItem(position);
        cache.titleView.setText(flow.getName());

        String questionString = "Questions";
        if (flow.getQuestionCount() == 1) {
            questionString = "Question";
        }

        cache.questionView.setText(flow.getQuestionCount() + " " + questionString);

        return row;
    }

    public static class ViewCache {
        TextView titleView;
        TextView questionView;
    }
}