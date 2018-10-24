package io.rapidpro.surveyor.ui;

import android.app.Activity;
import android.hardware.Camera;
import android.view.Surface;

/**
 * Utility methods for dealing with camera.
 */
public class CameraUtil {

    /**
     * Determine the rotation to apply for the preview for a given camera
     *
     * @param mirror whether to account for profile camera mirroring or not
     */
    public static int getRotationDegrees(Activity activity, int cameraId, boolean mirror) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {

            degrees = (info.orientation + degrees) % 360;
            // compensate for mirrored image, useful for previews on front facing cameras
            if (mirror) {
                degrees = (360 - degrees) % 360;
            }
        } else {
            degrees = (info.orientation - degrees + 360) % 360;
        }

        return degrees;
    }
}
