package io.rapidpro.surveyor.ui;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder m_surfaceHolder;
    private Camera m_camera;
    private int m_cameraId;
    private Activity m_activity;
    private boolean m_initialized;
    private int m_lastRotation;

    public CameraPreview(Activity context) {
        super(context);
        m_activity = context;
    }

    public void init() {
        if (!m_initialized) {
            m_surfaceHolder = getHolder();
            m_surfaceHolder.addCallback(this);
            m_initialized = true;

            final WindowManager windowManager = (WindowManager) m_activity.getSystemService(Context.WINDOW_SERVICE);
            m_lastRotation = -1;

            // Half rotations will change the layout from portrait to landscape and vice-versa. However,
            // this won't account for 180 degree rotations since they remain in the same configuration. We
            // need to listen for 180 orientation changes and update our camera orientation accordingly as well.
            OrientationEventListener orientationEventListener = new OrientationEventListener(m_activity, SensorManager.SENSOR_DELAY_NORMAL) {
                @Override
                public void onOrientationChanged(int orientation) {

                    Display display = windowManager.getDefaultDisplay();
                    int rotation = display.getRotation();
                    if ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && rotation != m_lastRotation) {
                        try {
                            updateOrientation();
                        } catch (Exception e) {}
                        m_lastRotation = rotation;
                    }
                }
            };

            if (orientationEventListener.canDetectOrientation()) {
                orientationEventListener.enable();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // create the surface and start camera preview
            if (m_camera != null) {
                m_camera.setDisplayOrientation(CameraUtil.getRotationDegrees(m_activity, m_cameraId, true));
                m_camera.setPreviewDisplay(holder);
                m_camera.startPreview();
            }
        } catch (IOException e) {
            Log.d(VIEW_LOG_TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // inform the camera of our current orientation
        updateOrientation();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        m_camera.release();
    }

    private void updateOrientation() {
        m_camera.setDisplayOrientation(CameraUtil.getRotationDegrees(m_activity, m_cameraId, true));
    }

    /**
     * Refresh the camera according to it's properties. Use for toggling between
     * front and rear cameras
     */
    public void refreshCamera(Camera camera, int cameraId) {

        if (m_surfaceHolder.getSurface() == null) {
            return;
        }

        if (m_camera != null) {
            try {
                m_camera.stopPreview();
            } catch (Exception e) {
                // we may already be released
            }
        }

        m_camera = camera;
        m_cameraId = cameraId;

        m_camera.setDisplayOrientation(CameraUtil.getRotationDegrees(m_activity, m_cameraId, true));

        try {
            m_camera.setPreviewDisplay(m_surfaceHolder);
            m_camera.startPreview();
        } catch (Exception e) {
            Surveyor.LOG.e("Error starting camera preview: " + e.getMessage(), e);
        }
    }

}