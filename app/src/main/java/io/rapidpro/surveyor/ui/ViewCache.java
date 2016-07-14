package io.rapidpro.surveyor.ui;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
    private Context m_context;

    public ViewCache(Context context, View parent) {
        m_cache = new SparseArray<>();
        m_parent = parent;
        m_context = context;
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

    public ImageView getImageView(int id) {
        return (ImageView) getCachedView(id);
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
        hide(id, false);
    }

    public void hide(int id, boolean hideKeyboard) {
        View view = getCachedView(id);
        view.setVisibility(View.GONE);

        if (hideKeyboard) {
            InputMethodManager imm = (InputMethodManager)m_context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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

    public View getView(int id) {
        return getCachedView(id);
    }
}
