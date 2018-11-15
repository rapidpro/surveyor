package io.rapidpro.surveyor.ui;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * A blocking progress dialog
 */
public class BlockingProgress extends ProgressDialog {

    public BlockingProgress(Context context, int title, int message, int total) {
        super(context);

        setTitle(title);
        setMessage(getContext().getString(message));
        setIndeterminate(false);
        setMax(total);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        setProgress(0);
    }
}
