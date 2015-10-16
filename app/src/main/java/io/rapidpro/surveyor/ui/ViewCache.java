package io.rapidpro.surveyor.ui;

import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import io.rapidpro.surveyor.R;

/**
 * Simple cache management for view lookups
 */
public class ViewCache {

    private SparseArray<View> m_cache;
    private View m_parent;

    public ViewCache(View parent) {
        m_cache = new SparseArray<>();
        m_parent = parent;
    }

    private View getCachedView(int id) {
        View view = m_cache.get(id);
        if (view == null){
            view = m_parent.findViewById(id);
            m_cache.put(id, view);
        }
        return view;

    }

    public TextView getTextView(int id) {
        return (TextView) getCachedView(id);
    }

    public Object getSelectedItem(int id) {
        return ((Spinner)getCachedView(id)).getSelectedItem();
    }

    public String getText(int id) {
        return getTextView(id).getText().toString().trim();
    }

    public void setError(int id, int errorMessage) {
        getTextView(id).setError(m_parent.getContext().getString(errorMessage));
    }

    public void clearError(int id) {
        getTextView(id).setError(null);
    }

    public String getRequiredText(int id) {
        clearError(id);
        String text = getText(id);
        if (text.length() == 0) {
            setError(id, R.string.error_field_required);
            return null;
        }
        return text;
    }

    public void hide(int id) {
        getCachedView(id).setVisibility(View.GONE);
    }

    public void show(int id) {
        getCachedView(id).setVisibility(View.VISIBLE);
    }

    public void setText(int id, String text) {
        getTextView(id).setText(text);
    }

    public void setVisible(int id, boolean visible) {
        View view = getCachedView(id);
        if (view != null) {
            if (visible) {
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.GONE);
            }
        }
    }

    public ListView getListView(int id) {
        return (ListView)getCachedView(id);
    }

    public ListAdapter getListViewAdapter(int list) {
        ListView listView = getListView(list);
        if (listView != null) {
            return listView.getAdapter();
        }
        return null;
    }

    public Button getButton(int id) {
        return (Button)getCachedView(id);
    }

    public void setButtonText(int id, String text) {
        Button button = getButton(id);
        if (button != null) {
            button.setText(text);
        }
    }
}
