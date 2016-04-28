package io.rapidpro.surveyor.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.ui.IconTextView;

public class AudioCaptureActivity extends BaseActivity {

    private boolean m_isRecording = false;
    private MediaRecorder m_mediaRecorder;
    private IconTextView m_recordButton;
    private TextView m_instructions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        m_recordButton = (IconTextView) findViewById(R.id.button_capture);
        m_instructions = (TextView) findViewById(R.id.text_instructions);
    }

    public void recordAudio () {
        m_isRecording = true;

        try {
            m_mediaRecorder = new MediaRecorder();
            m_mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            m_mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            m_mediaRecorder.setOutputFile(getIntent().getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE));
            m_mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            m_mediaRecorder.prepare();

        } catch (Exception e) {
            e.printStackTrace();
        }

        m_mediaRecorder.start();
    }

    private void releaseMediaRecorder() {
        if (m_mediaRecorder != null) {
            m_mediaRecorder.reset();
            m_mediaRecorder.release();
            m_mediaRecorder = null;
        }
    }

    private void stopRecording() {
        if (m_mediaRecorder != null) {
            m_mediaRecorder.stop();
        }
        releaseMediaRecorder();
        m_isRecording = false;

        Intent returnIntent = new Intent();
        returnIntent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, getIntent().getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE));
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    public void toggleRecording(View view) {
        if (!m_isRecording) {
            Resources res = getResources();
            m_recordButton.setTextColor(res.getColor(R.color.recording));
            m_instructions.setText(getString(R.string.tap_to_stop));
            getViewCache().getView(R.id.content_view).setBackgroundColor(res.getColor(R.color.warning));
            recordAudio();
        } else {
            stopRecording();
        }
    }

}
