package io.rapidpro.surveyor.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.ui.CachedLinearLayout;

/**
 * Represents a chat bubble for either inbound or
 * outbound messages
 */
public class ChatBubbleView extends CachedLinearLayout {

    private ViewGroup m_root;

    public ChatBubbleView(Context context) {
        super(context);
        init();
    }

    public ChatBubbleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChatBubbleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        super.init();
        // m_root = (ViewGroup) findViewById(R.id.chat_bubble);
    }

    /**
     * Set the message to show in our chat bubble
     * @param inbound true if it is coming from the flow engine
     * @param text the message to set on the view
     */
    public void setMessage(String text, boolean inbound) {
        TextView tv = getTextView(R.id.text_message);
        tv.setText(text);

        if (!inbound) {
            tv.setBackground(getResources().getDrawable(R.drawable.chat_bubble_out));
        }

        addView(getSpacer(), inbound ? 1 : 0);
    }

    /**
     * Find and remove our spacer from the current layout
     */
    private View getSpacer() {

        // remove our spacer if it's in there
        View spacer = findViewById(R.id.spacer);
        if (spacer != null) {
            removeView(spacer);
        }
        return spacer;
    }
}
