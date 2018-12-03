package io.rapidpro.surveyor.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.task.SubmitSubmissionsTask;
import io.rapidpro.surveyor.ui.BlockingProgress;

/**
 * Base for activities that have submissions ((org and flow views)
 */
public abstract class BaseSubmissionsActivity extends BaseActivity {

    /**
     * User has clicked a submit button
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

    /**
     * Does the actual invoking of the submissions task
     */
    private void doSubmit() {
        final BlockingProgress progressModal = new BlockingProgress(this, R.string.one_moment, R.string.submit_body);
        progressModal.show();

        final List<Submission> pending = getPendingSubmissions();
        final Submission[] asArray = pending.toArray(new Submission[0]);
        final Resources res = getResources();

        SubmitSubmissionsTask task = new SubmitSubmissionsTask(new SubmitSubmissionsTask.Listener() {
            @Override
            public void onProgress(int percent) {
                progressModal.setProgress(percent);
            }

            @Override
            public void onComplete(int total) {
                refresh();

                progressModal.dismiss();

                CharSequence toast = res.getQuantityString(R.plurals.submissions_sent, total, total);
                Toast.makeText(BaseSubmissionsActivity.this, toast, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int numFailed) {
                progressModal.dismiss();

                Toast.makeText(BaseSubmissionsActivity.this, getString(R.string.error_submissions_send), Toast.LENGTH_SHORT).show();
            }
        });

        task.setUsername(getUsername());
        task.setLegacyToken(getOrg().getToken());
        task.includeLegacySubmissions(getLegacySubmissions());
        task.execute(asArray);
    }

    protected abstract List<Submission> getPendingSubmissions();

    protected abstract List<File> getLegacySubmissions();

    protected abstract Org getOrg();

    protected abstract void refresh();
}
