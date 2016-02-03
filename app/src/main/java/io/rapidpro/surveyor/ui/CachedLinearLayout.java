package io.rapidpro.surveyor.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * LinearLayout with a ViewCache
 */
public class CachedLinearLayout extends LinearLayout {

    private ViewCache m_cache;

    public CachedLinearLayout(Context context) {
        super(context);
        init();
    }

    public CachedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CachedLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        m_cache = new ViewCache(getContext(), this);
    }

    public TextView getTextView(int id) {
        return m_cache.getTextView(id);
    }
}
