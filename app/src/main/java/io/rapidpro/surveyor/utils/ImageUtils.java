package io.rapidpro.surveyor.utils;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class ImageUtils {
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
     * Creates a thumbnail image from the given video file
     *
     * @param video the video file
     * @return the thumbnail bitmap
     */
    public static Bitmap thumbnailFromVideo(File video) {
        return ThumbnailUtils.createVideoThumbnail(video.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
    }
}
