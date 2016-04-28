package io.rapidpro.surveyor.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.greysonparrelli.permiso.Permiso;
import com.greysonparrelli.permiso.PermisoActivity;

import java.io.IOException;

import io.rapidpro.surveyor.BuildConfig;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.fragment.OrgListFragment;
import io.rapidpro.surveyor.ui.CameraPreview;
import io.rapidpro.surveyor.ui.CameraUtil;
import io.rapidpro.surveyor.ui.IconTextView;

/**
 * Activity for capturing videos
 */
public class VideoCaptureActivity extends PermisoActivity {

    // camera settings
    public static final int CAMERA_QUALITY = CamcorderProfile.QUALITY_480P;
    public static final int MAX_DURATION = 600000; // 60s max duration
    public static final int MAX_FILESIZE = 50000000; // 50MB max filesize

    private int m_cameraId;
    private Camera m_camera;
    private CameraPreview m_preview;
    private MediaRecorder m_mediaRecorder;
    private IconTextView m_toggleCameraButton;
    private IconTextView m_recordButton;

    private int m_cameraDirection = -1;
    private boolean m_recording = false;

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        m_cameraDirection = bundle.getInt(SurveyorIntent.EXTRA_CAMERA_DIRECTION, -1);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt(SurveyorIntent.EXTRA_CAMERA_DIRECTION, m_cameraDirection);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        m_recordButton = (IconTextView) findViewById(R.id.button_capture);
        m_toggleCameraButton = (IconTextView) findViewById(R.id.button_switch);
        m_preview = new CameraPreview(this);
        ((LinearLayout) findViewById(R.id.camera_preview)).addView(m_preview);
    }

    public void onResume() {
        super.onResume();
        if (!hasCamera(this)) {
            Toast toast = Toast.makeText(this, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }

        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    if (m_camera == null) {

                        m_preview.init();

                        // if the front facing camera does not exist
                        if (getFrontCamera() < 0) {
                            Toast.makeText(VideoCaptureActivity.this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                            m_toggleCameraButton.setVisibility(View.GONE);
                        } else if (m_cameraDirection == CameraInfo.CAMERA_FACING_FRONT) {
                            m_cameraId = getFrontCamera();
                        }

                        // default to the back camera if one isn't set
                        if (m_cameraId == -1) {
                            m_cameraId = getBackCamera();
                            m_cameraDirection = CameraInfo.CAMERA_FACING_BACK;
                        }

                        try {
                            Surveyor.LOG.d("Opening camera: " + m_cameraId);
                            m_camera = Camera.open(m_cameraId);
                            m_preview.refreshCamera(m_camera, m_cameraId);

                            if (m_camera != null) {
                                Camera.Parameters params = m_camera.getParameters();
                                if (params.getSupportedFocusModes().contains(
                                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                                }
                                m_camera.setParameters(params);
                            }
                        } catch (Exception e) {
                            Surveyor.LOG.e("Failed to open camera", e);
                            finish();
                        }
                    }
                } else {
                    // didn't grant us permission
                    finish();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                VideoCaptureActivity.this.showRationaleDialog(R.string.permission_camera, callback);
            }
        }, Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "android.permission.READ_PROFILE");

    }

    /**
     * Flips from front to back camera
     */
    public void toggleCamera() {

        releaseCamera();

        // if the camera preview is the front
        if (m_cameraDirection == CameraInfo.CAMERA_FACING_FRONT) {
            m_cameraId = getBackCamera();
            if (m_cameraId >= 0) {
                m_camera = Camera.open(m_cameraId);
                m_cameraDirection = CameraInfo.CAMERA_FACING_BACK;
                m_preview.refreshCamera(m_camera, m_cameraId);

                m_toggleCameraButton.setText(getString(R.string.icon_camera_front));
            }
        } else {
            m_cameraId = getFrontCamera();
            if (m_cameraId >= 0) {
                m_camera = Camera.open(m_cameraId);
                m_cameraDirection = CameraInfo.CAMERA_FACING_FRONT;
                m_preview.refreshCamera(m_camera, m_cameraId);
                m_toggleCameraButton.setText(getString(R.string.icon_camera_rear));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // free up camera for other apps
        releaseCamera();
    }

    private boolean hasCamera(Context context) {
        // check if the device has camera
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * creates a new MediaRecorder
     */
    private MediaRecorder createMediaRecorder() {

        MediaRecorder mediaRecorder = new MediaRecorder();
        m_camera.unlock();
        mediaRecorder.setCamera(m_camera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        try {
            if (BuildConfig.DEBUG) {
                mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
            } else {
                mediaRecorder.setProfile(CamcorderProfile.get(CAMERA_QUALITY));
            }
        } catch (Exception e) {}

        // set our recorder to use the output file provided by our caller
        String filename = getIntent().getExtras().getString(SurveyorIntent.EXTRA_MEDIA_FILE);
        mediaRecorder.setOutputFile(filename);

        // set video maximums
        mediaRecorder.setMaxDuration(MAX_DURATION);
        mediaRecorder.setMaxFileSize(MAX_FILESIZE);

        // set our orientation hint according ot our rotation
        mediaRecorder.setOrientationHint(CameraUtil.getRotationDegrees(this, m_cameraId, false));

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            releaseMediaRecorder();
        }
        return mediaRecorder;
    }

    private void releaseMediaRecorder() {
        if (m_mediaRecorder != null) {
            m_mediaRecorder.reset(); // clear recorder configuration
            m_mediaRecorder.release(); // release the recorder object
            m_mediaRecorder = null;
            m_camera.lock(); // lock camera for later use
        }
    }

    private void releaseCamera() {
        // stop and release camera
        if (m_camera != null) {
            m_camera.release();
            m_camera = null;
        }
    }

    private int getFrontCamera() {
        return findCamera(CameraInfo.CAMERA_FACING_FRONT);
    }

    private int getBackCamera() {
        return findCamera(CameraInfo.CAMERA_FACING_BACK);
    }

    private int findCamera(int direction) {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == direction) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Start recording using our MediaRecorder
     */
    private void startRecording() {

        m_mediaRecorder = createMediaRecorder();
        if (m_mediaRecorder == null) {
            Toast.makeText(VideoCaptureActivity.this, "Can't record video", Toast.LENGTH_LONG).show();
            finish();
        }

        // show our record button as red
        m_recordButton.setTextColor(getResources().getColor(R.color.recording));
        m_recordButton.setText(getString(R.string.icon_stop));

        // work on UiThread for better performance
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    // and go!
                    m_mediaRecorder.start();
                } catch (Exception ignored) {
                }
            }
        });

        m_recording = true;
    }

    /**
     * Stops recording, finishing our activity
     */
    private void stopRecording() {

        if (m_mediaRecorder != null) {
            try {
                m_mediaRecorder.stop();
            } catch (Exception e) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }
        releaseMediaRecorder();
        m_recording = false;

        Intent returnIntent = new Intent();
        returnIntent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, getIntent().getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE));
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    public void toggleRecording(View view) {
        if (!m_recording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    public void toggleCameras(View view) {
        if (!m_recording) {
            if (Camera.getNumberOfCameras() > 1) {
                toggleCamera();
            } else {
                Toast toast = Toast.makeText(VideoCaptureActivity.this,
                        "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    public void showRationaleDialog(int body, Permiso.IOnRationaleProvided callback) {
        Permiso.getInstance().showRationaleInDialog(getString(R.string.title_permissions), getString(body), null, callback);
    }
}