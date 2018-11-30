package io.rapidpro.surveyor.legacy;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import java.text.NumberFormat;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.activity.BaseActivity;
import io.rapidpro.surveyor.ui.BlockingProgress;
import io.rapidpro.surveyor.ui.ViewCache;

/**
 * Allows users to submit legacy submissions created with the previous flow engine
 */
public class LegacySubmissionsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this holds our flow list fragment which shows all available flows
        setContentView(R.layout.activity_legacy);

        int pending = Legacy.getSubmissionsCount();
        ViewCache cache = getViewCache();
        cache.setVisible(R.id.container_pending, pending > 0);
        cache.setButtonText(R.id.button_pending, NumberFormat.getInstance().format(pending));
    }

    /**
     * User clicked the submit button
     *
     * @param view the button
     */
    public void onActionSubmit(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_send_submissions))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        doSubmit();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    private void doSubmit() {
        final BlockingProgress progressModal = new BlockingProgress(this, R.string.one_moment, R.string.submit_body);
        progressModal.show();

        new SubmitSubmissionsTask(new SubmitSubmissionsTask.Listener() {
            @Override
            public void onProgress(int percent) {
                progressModal.setProgress(percent);
            }

            @Override
            public void onComplete() {
                progressModal.dismiss();

                finish();
            }

            @Override
            public void onFailure() {
                progressModal.dismiss();

                showToast(R.string.error_legacy_submissions_submit);
            }
        }).execute(getUsername());
    }
}
