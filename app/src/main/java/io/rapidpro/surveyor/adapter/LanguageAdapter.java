package io.rapidpro.surveyor.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.neovisionaries.i18n.LanguageAlpha3Code;

import java.util.List;

public class LanguageAdapter extends ArrayAdapter<LanguageAlpha3Code> {

    private int m_resource;
    private Context m_context;

    public LanguageAdapter(Context context, int resource, List<LanguageAlpha3Code> items) {
        super(context, resource, items);
        m_resource = resource;
        m_context = context;

        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewCache cache;

        if(row == null) {
            LayoutInflater inflater = ((Activity)m_context).getLayoutInflater();
            row = inflater.inflate(m_resource, parent, false);

            cache = new ViewCache();
            cache.text = (TextView)row;

            row.setTag(cache);
        } else {
            cache = (ViewCache)row.getTag();
        }

        LanguageAlpha3Code lang = getItem(position);
        cache.text.setText(lang.getName());

        return row;
    }

    public static class ViewCache {
        TextView text;
    }
}
