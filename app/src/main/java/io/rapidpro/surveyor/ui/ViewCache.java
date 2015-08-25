package io.rapidpro.surveyor.ui;

import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

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
}
