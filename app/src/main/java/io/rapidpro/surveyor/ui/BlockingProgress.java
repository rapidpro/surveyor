package io.rapidpro.surveyor.ui;

import android.app.ProgressDialog;
import android.content.Context;

import io.rapidpro.surveyor.R;

/**
 * Created by eric on 9/21/15.
 */
public class BlockingProgress extends ProgressDialog {

    public BlockingProgress(Context context, int title, int message, int total) {
        super(context);

        setTitle(title);
        setMessage(getContext().getString(message));

        setIndeterminate(false);
        setMax(total);
        setCancelable(false);
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        setCanceledOnTouchOutside(false);
        setProgress(0);
    }

    @Override
    public void incrementProgressBy(int diff) {
        super.incrementProgressBy(diff);

        // check if we are ready to dismiss
        if (getProgress() == getMax()) {
            dismiss();
        }
    }
}
