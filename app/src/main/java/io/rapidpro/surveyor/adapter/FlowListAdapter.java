package io.rapidpro.surveyor.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.text.NumberFormat;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.Submission;
import io.realm.RealmBaseAdapter;
import io.realm.RealmResults;

public class FlowListAdapter extends RealmBaseAdapter<DBFlow> implements ListAdapter {

    private int m_resourceId;

    public FlowListAdapter(Context context, int resourceId,
                           RealmResults<DBFlow> realmResults,
                           boolean automaticUpdate) {
        super(context, realmResults, automaticUpdate);
        m_resourceId = resourceId;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewCache cache;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(m_resourceId, parent, false);

            cache = new ViewCache();
            cache.titleView = (TextView) row.findViewById(R.id.text_flow_name);
            cache.questionView = (TextView) row.findViewById(R.id.text_flow_questions);
            cache.pendingSubmissions = (TextView) row.findViewById(R.id.text_pending_submissions);

            row.setTag(cache);
        } else {
            cache = (ViewCache) row.getTag();
        }

        DBFlow flow = getItem(position);
        cache.titleView.setText(flow.getName());

        NumberFormat nf = NumberFormat.getInstance();
        int submissions = Submission.getPendingSubmissionCount(flow);
        cache.pendingSubmissions.setText(nf.format(submissions));
        cache.pendingSubmissions.setTag(flow);

        if (submissions > 0) {
            cache.pendingSubmissions.setVisibility(View.VISIBLE);
        } else {
            cache.pendingSubmissions.setVisibility(View.GONE);
        }

        String questionString = "Questions";
        if (flow.getQuestionCount() == 1) {
            questionString = "Question";
        }

        cache.questionView.setText(nf.format(flow.getQuestionCount()) + " " + questionString + " (v" + nf.format(flow.getRevision()) + ")");
        return row;
    }

    public static class ViewCache {
        TextView titleView;
        TextView questionView;
        TextView pendingSubmissions;
    }
}