package io.rapidpro.surveyor.ui;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * A blocking progress dialog
 */
public class BlockingProgress extends ProgressDialog {

    public BlockingProgress(Context context, int title, int message) {
        super(context);

        setTitle(title);
        setMessage(getContext().getString(message));
        setIndeterminate(false);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        setProgress(0);
        setMax(100);
    }
}
