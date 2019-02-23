package io.rapidpro.surveyor.utils;

import android.graphics.Bitmap;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import io.rapidpro.surveyor.test.BaseApplicationTest;
import io.rapidpro.surveyor.test.R;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class ImageUtilsTest extends BaseApplicationTest {
    @Test
    public void thumbnailFromVideo() throws IOException {
        File temp = File.createTempFile("video", "", getSurveyor().getCacheDir());
        copyResource(R.raw.capture_video, temp);

        Bitmap thumbnail = ImageUtils.thumbnailFromVideo(temp);

        assertThat(thumbnail.getWidth(), is(512));
        assertThat(thumbnail.getHeight(), lessThan(384));
    }
}
