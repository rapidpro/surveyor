package io.rapidpro.surveyor.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.ui.CachedLinearLayout;
import io.rapidpro.surveyor.ui.IconTextView;

/**
 * Represents a chat bubble for either inbound or
 * outbound messages
 */
public class ChatBubbleView extends CachedLinearLayout {

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

    /**
     * Set the message to show in our chat bubble
     *
     * @param inbound true if it is coming from the flow engine
     * @param text    the message to set on the view
     */
    public void setMessage(String text, boolean inbound) {
        TextView tv = getTextView(R.id.text_message);
        tv.setText(text);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        if (!inbound) {
            tv.setBackground(getResources().getDrawable(R.drawable.chat_bubble_out));
        }

        addView(getSpacer(), inbound ? 1 : 0);
    }

    public void setThumbnail(Bitmap image, String url, int type) {
        ImageView imageView = getImageView(R.id.thumbnail);

        if (image != null) {
            imageView.setImageBitmap(image);
        } else {

        }

        View mediaView = getView(R.id.media_view);
        mediaView.setVisibility(VISIBLE);

        mediaView.setTag(R.string.tag_url, url);
        mediaView.setTag(R.string.tag_media_type, type);

        if (type == R.string.media_image) {
            getTextView(R.id.media_icon).setText(R.string.icon_photo);
        } else if (type == R.string.media_video) {
            getTextView(R.id.media_icon).setText(R.string.icon_play_arrow);
        } else if (type == R.string.media_audio || type == R.string.media_location) {
            IconTextView iconView = (IconTextView) getTextView(R.id.media_icon);

            if (type == R.string.media_audio) {
                iconView.setText(R.string.icon_volume_up);
            } else {
                iconView.setText(R.string.icon_place);
            }

            iconView.setIconColor(R.color.primary_lightest);
            getView(R.id.media_view).setBackground(null);
            final float scale = getContext().getResources().getDisplayMetrics().density;
            int pixels = (int) (70 * scale + 0.5f);
            ViewGroup.LayoutParams params = mediaView.getLayoutParams();
            params.height = pixels;
            mediaView.setLayoutParams(params);
        }

        show(R.id.media_view);
        hide(R.id.text_message);
        hide(R.id.spacer);
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
