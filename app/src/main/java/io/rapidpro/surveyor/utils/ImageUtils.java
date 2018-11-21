package io.rapidpro.surveyor.utils;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

public class ImageUtils {
    public static byte[] convertToJPEG(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return baos.toByteArray();
    }

    public static Bitmap scaleToWidth(Bitmap bitmap, int width) {
        double ratio = (double) width / (double) bitmap.getWidth();
        return Bitmap.createScaledBitmap(bitmap, width, (int) ((double) bitmap.getHeight() * ratio), false);
    }

    /**
     * Scales a bitmap so that it's longest dimension is provided value
     */
    public static Bitmap scaleToMax(Bitmap bitmap, int max) {

        // landscape photos
        if (bitmap.getWidth() > bitmap.getHeight()) {
            double ratio = (double) max / (double) bitmap.getWidth();
            return Bitmap.createScaledBitmap(bitmap, max, (int) ((double) bitmap.getHeight() * ratio), false);

        } else {
            double ratio = (double) max / (double) bitmap.getHeight();
            return Bitmap.createScaledBitmap(bitmap, (int) ((double) bitmap.getWidth() * ratio), max, false);
        }
    }
}
