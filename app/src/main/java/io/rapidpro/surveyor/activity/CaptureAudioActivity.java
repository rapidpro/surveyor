package io.rapidpro.surveyor.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.ui.IconTextView;

/**
 * Activity for capturing an audio recording
 */
public class CaptureAudioActivity extends BaseActivity {

    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_capture_audio);
    }

    @Override
    public boolean requireLogin() {
        return false;
    }

    public void recordAudio() {
        isRecording = true;

        String output = getIntent().getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE);

        Logger.d("Recording audio to " + output + "...");

        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(output);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.prepare();

        } catch (Exception e) {
            Logger.e("Unable to create media recorder", e);
        }

        mediaRecorder.start();
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
        }
        releaseMediaRecorder();
        isRecording = false;

        Intent returnIntent = new Intent();
        returnIntent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, getIntent().getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE));
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    public void toggleRecording(View view) {
        if (!isRecording) {
            Resources res = getResources();

            IconTextView button = (IconTextView) getViewCache().getView(R.id.button_capture);
            button.setTextColor(res.getColor(R.color.recording));

            TextView instructions = (TextView) getViewCache().getView(R.id.text_instructions);
            instructions.setText(R.string.tap_to_stop);

            getViewCache().getView(R.id.content_view).setBackgroundColor(res.getColor(R.color.warning));
            recordAudio();
        } else {
            stopRecording();
        }
    }
}
