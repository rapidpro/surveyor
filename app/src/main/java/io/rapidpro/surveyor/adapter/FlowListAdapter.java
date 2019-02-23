package io.rapidpro.surveyor.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.legacy.Legacy;

public class FlowListAdapter extends ArrayAdapter<Flow> {

    private Org org;

    public FlowListAdapter(Context context, int resourceId, Org org, List<Flow> flows) {
        super(context, resourceId, flows);

        this.org = org;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewCache cache;

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (row == null) {
            row = inflater.inflate(R.layout.item_flow, parent, false);

            cache = new ViewCache();
            cache.titleView = row.findViewById(R.id.text_flow_name);
            cache.questionView = row.findViewById(R.id.text_flow_questions);
            cache.pendingSubmissions = row.findViewById(R.id.text_pending_submissions);

            row.setTag(cache);
        } else {
            cache = (ViewCache) row.getTag();
        }

        Flow flow = getItem(position);
        cache.titleView.setText(flow.getName());

        int pending = SurveyorApplication.get().getSubmissionService().getCompletedCount(org, flow);
        pending += Legacy.getCompletedCount(org, flow);

        NumberFormat nf = NumberFormat.getInstance();
        cache.pendingSubmissions.setText(nf.format(pending));
        cache.pendingSubmissions.setTag(flow);
        cache.pendingSubmissions.setVisibility(pending > 0 ? View.VISIBLE : View.GONE);

        int numQuestions = flow.getQuestionCount();
        String questionsString = getContext().getResources().getQuantityString(R.plurals.questions, numQuestions, numQuestions);

        cache.questionView.setText(questionsString + " (v" + nf.format(flow.getRevision()) + ")");
        return row;
    }

    public static class ViewCache {
        TextView titleView;
        TextView questionView;
        TextView pendingSubmissions;
    }
}