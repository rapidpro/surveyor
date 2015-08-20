package io.rapidpro.surveyor.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class IconTextView extends TextView {
    public IconTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeface(Typefaces.get(getContext(), "material.ttf"));
    }

    public void setIconColor(int color) {
        setTextColor(getResources().getColor(color));
    }
}
