package io.rapidpro.surveyor.activity;

import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;

import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.ui.BlockingProgress;
import retrofit.RetrofitError;

/**
 * AsyncTask for sending submissions to the server
 */
class SubmitSubmissions extends AsyncTask<String, Void, Void> {

    private BaseActivity m_activity;
    private File[] m_submissions;
    private BlockingProgress m_progress;
    private int m_error;

    public SubmitSubmissions(BaseActivity activity, File[] submissions, BlockingProgress progress) {
        m_activity = activity;
        m_submissions = submissions;
        m_progress = progress;
    }

    @Override
    protected Void doInBackground(String... params) {

        for (File submission : m_submissions) {
            Submission sub = Submission.load(submission);
            if (sub != null) {
                try {
                    sub.submit();
                } catch (RetrofitError e) {
                    Surveyor.LOG.e("Failed to submit flow run", e);
                    m_error = m_activity.getRapidProService().getErrorMessage(e);
                    return null;
                }
            }

            m_progress.incrementProgressBy(1);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        m_progress.dismiss();
        m_activity.refresh();

        if (m_error > 0) {
            Toast.makeText(m_activity, m_error, Toast.LENGTH_SHORT).show();
        }
    }
}
