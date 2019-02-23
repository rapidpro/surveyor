package io.rapidpro.surveyor.activity;

import android.Manifest;
import android.app.Activity;
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
import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.ui.CameraPreview;
import io.rapidpro.surveyor.ui.CameraUtil;
import io.rapidpro.surveyor.ui.IconTextView;

/**
 * Activity for capturing a video message
 */
public class CaptureVideoActivity extends PermisoActivity {

    // camera settings
    public static final int CAMERA_QUALITY = CamcorderProfile.QUALITY_480P;
    public static final int MAX_DURATION = 600000; // 60s max duration
    public static final int MAX_FILESIZE = 50000000; // 50MB max filesize

    private int cameraId;
    private Camera camera;
    private CameraPreview preview;
    private MediaRecorder mediaRecorder;
    private IconTextView toggleCameraButton;
    private IconTextView recordButton;

    private int cameraDirection = -1;
    private boolean recording = false;

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        
        cameraDirection = bundle.getInt(SurveyorIntent.EXTRA_CAMERA_DIRECTION, -1);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt(SurveyorIntent.EXTRA_CAMERA_DIRECTION, cameraDirection);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_capture_video);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        recordButton = findViewById(R.id.button_capture);
        toggleCameraButton = findViewById(R.id.button_switch);
        preview = new CameraPreview(this);
        ((LinearLayout) findViewById(R.id.camera_preview)).addView(preview);
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
                    startCamera();
                } else {
                    // didn't grant us permission
                    finish();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                CaptureVideoActivity.this.showRationaleDialog(R.string.permission_camera, callback);
            }
        }, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS, "android.permission.READ_PROFILE");

    }

    private void startCamera() {
        if (camera == null) {

            preview.init();

            // if the front facing camera does not exist
            if (getFrontCamera() < 0) {
                Toast.makeText(CaptureVideoActivity.this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                toggleCameraButton.setVisibility(View.GONE);
            } else if (cameraDirection == CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = getFrontCamera();
            }

            // default to the back camera if one isn't set
            if (cameraId == -1) {
                cameraId = getBackCamera();
                cameraDirection = CameraInfo.CAMERA_FACING_BACK;
            }

            try {
                Logger.d("Opening camera: " + cameraId);
                camera = Camera.open(cameraId);
                preview.refreshCamera(camera, cameraId);

                if (camera != null) {
                    Camera.Parameters params = camera.getParameters();
                    if (params.getSupportedFocusModes().contains(
                            Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                    camera.setParameters(params);
                }
            } catch (Exception e) {
                Logger.e("Failed to open camera", e);
                finish();
            }
        }
    }

    /**
     * Flips from front to back camera
     */
    public void toggleCamera() {

        releaseCamera();

        // if the camera preview is the front
        if (cameraDirection == CameraInfo.CAMERA_FACING_FRONT) {
            cameraId = getBackCamera();
            if (cameraId >= 0) {
                camera = Camera.open(cameraId);
                cameraDirection = CameraInfo.CAMERA_FACING_BACK;
                preview.refreshCamera(camera, cameraId);

                toggleCameraButton.setText(getString(R.string.icon_camera_front));
            }
        } else {
            cameraId = getFrontCamera();
            if (cameraId >= 0) {
                camera = Camera.open(cameraId);
                cameraDirection = CameraInfo.CAMERA_FACING_FRONT;
                preview.refreshCamera(camera, cameraId);
                toggleCameraButton.setText(getString(R.string.icon_camera_rear));
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
        camera.unlock();
        mediaRecorder.setCamera(camera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        try {
            if (BuildConfig.DEBUG) {
                mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
            } else {
                mediaRecorder.setProfile(CamcorderProfile.get(CAMERA_QUALITY));
            }
        } catch (Exception e) {
        }

        // set our recorder to use the output file provided by our caller
        String filename = getIntent().getExtras().getString(SurveyorIntent.EXTRA_MEDIA_FILE);
        mediaRecorder.setOutputFile(filename);

        // set video maximums
        mediaRecorder.setMaxDuration(MAX_DURATION);
        mediaRecorder.setMaxFileSize(MAX_FILESIZE);

        // set our orientation hint according ot our rotation
        mediaRecorder.setOrientationHint(CameraUtil.getRotationDegrees(this, cameraId, false));

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            releaseMediaRecorder();
        }
        return mediaRecorder;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            camera.lock(); // lock camera for later use
        }
    }

    private void releaseCamera() {
        // stop and release camera
        if (camera != null) {
            camera.release();
            camera = null;
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

        mediaRecorder = createMediaRecorder();
        if (mediaRecorder == null) {
            Toast.makeText(CaptureVideoActivity.this, "Can't record video", Toast.LENGTH_LONG).show();
            finish();
        }

        // show our record button as red
        recordButton.setTextColor(getResources().getColor(R.color.recording));
        recordButton.setText(getString(R.string.icon_stop));

        // work on UiThread for better performance
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    // and go!
                    mediaRecorder.start();
                } catch (Exception ignored) {
                }
            }
        });

        recording = true;
    }

    /**
     * Stops recording, finishing our activity
     */
    private void stopRecording() {

        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception e) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }
        releaseMediaRecorder();
        recording = false;

        Intent returnIntent = new Intent();
        returnIntent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, getIntent().getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE));
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    public void toggleRecording(View view) {
        if (!recording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    public void toggleCameras(View view) {
        if (!recording) {
            if (Camera.getNumberOfCameras() > 1) {
                toggleCamera();
            } else {
                Toast toast = Toast.makeText(CaptureVideoActivity.this,
                        "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    public void showRationaleDialog(int body, Permiso.IOnRationaleProvided callback) {
        Permiso.getInstance().showRationaleInDialog(getString(R.string.title_permissions), getString(body), null, callback);
    }
}