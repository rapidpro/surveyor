package io.rapidpro.surveyor.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.ui.CachedLinearLayout;
import io.rapidpro.surveyor.ui.IconTextView;


public class IconLinkView extends CachedLinearLayout {

    public IconLinkView(Context context) {
        super(context);
        init();
    }

    public IconLinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IconLinkView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        super.init();
    }

    public void initialize(String message, int type, String url) {

        View bubble = getView(R.id.chat_bubble);
        bubble.setTag(R.string.tag_url, url);
        bubble.setTag(R.string.tag_media_type, type);

        TextView tv = getTextView(R.id.text_message);
        tv.setText(message);

        IconTextView iconView = (IconTextView) getTextView(R.id.media_icon);

        if (type == R.string.media_audio) {
            iconView.setText(R.string.icon_volume_up);
        } else if (type == R.string.media_location) {
            iconView.setText(R.string.icon_place);
        }
    }
}
