package io.rapidpro.surveyor.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import io.rapidpro.surveyor.Logger;

import android.media.ThumbnailUtils;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageUtils {

    /**
     * Encodes the given bitmap as a JPEG
     * @param bm the bitmap
     * @return the JSON data
     */
    public static byte[] convertToJPEG(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return baos.toByteArray();
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

    /**
     * Rotates a bitmap by the given number of degrees
     */
    public static Bitmap rotateImage(Bitmap img, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    /**
     * Gets the EXIF orientation (if set) in degrees
     */
    public static int getExifRotation(String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
            }
        } catch (IOException e) {
            Logger.d("Unable to read EXIF data from " + path);
        }
        return 0;
    }

    /**
     * Creates a thumbnail image from the given video file
     *
     * @param video the video file
     * @return the thumbnail bitmap
     */
    public static Bitmap thumbnailFromVideo(File video) {
        return ThumbnailUtils.createVideoThumbnail(video.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
    }
}
