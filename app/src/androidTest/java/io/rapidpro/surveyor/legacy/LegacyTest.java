package io.rapidpro.surveyor.legacy;

import android.content.Context;
import android.os.Environment;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import androidx.test.platform.app.InstrumentationRegistry;
import io.rapidpro.surveyor.net.TembaException;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import io.rapidpro.surveyor.test.R;
import io.rapidpro.surveyor.utils.SurveyUtils;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LegacyTest extends BaseApplicationTest {

    @Before
    public void clearStorage() {
        FileUtils.deleteQuietly(new File(Environment.getExternalStorageDirectory(), "Surveyor"));
    }

    @Test
    public void getSubmissionsCount_whenStorageDirNotExists() throws IOException {
        assertThat(Legacy.getSubmissionsCount(), is(0));
    }

    @Test
    public void getSubmissionsCount_whenSubmissionsDirNotExists() throws IOException {
        SurveyUtils.mkdir(Environment.getExternalStorageDirectory(), "Surveyor");

        assertThat(Legacy.getSubmissionsCount(), is(0));
    }

    @Test
    public void submitAll() throws IOException, TembaException, InterruptedException {
        unzipResource(R.raw.legacy_submissions, Environment.getExternalStorageDirectory());

        assertThat(Legacy.getSubmissionsCount(), is(2));

        mockServerResponse("{\"uuid\":\"d3acd0e4-8a75-419f-96cb-7699d9f5f193\"}", "application/json", 200);
        mockServerResponse("{\"location\":\"http://uploads.rapidpro.io/image.jpg\"}", "application/json", 200);
        mockServerResponse("{\"location\":\"http://uploads.rapidpro.io/audio.m4a\"}", "application/json", 200);
        mockServerResponse("{\"error\":null}", "application/json", 200);
        mockServerResponse("{\"uuid\":\"9443f80f-4a09-4505-8139-7ca934915234\"}", "application/json", 200);
        mockServerResponse("{\"error\":null}", "application/json", 200);

        Legacy.submitAll("bob@nyaruka.com");

        RecordedRequest request1 = mockServer.takeRequest();
        assertThat(request1.getRequestLine(), is("POST /api/v1/contacts.json HTTP/1.1"));
        assertThat(request1.getHeader("Authorization"), is("Token 797d44ef78f7845de0f4dbb42d5174505563dd77"));

        assertThat(mockServer.takeRequest().getRequestLine(), is("POST /api/v2/media.json HTTP/1.1"));
        assertThat(mockServer.takeRequest().getRequestLine(), is("POST /api/v2/media.json HTTP/1.1"));
        assertThat(mockServer.takeRequest().getRequestLine(), is("POST /api/v1/steps.json HTTP/1.1"));
        assertThat(mockServer.takeRequest().getRequestLine(), is("POST /api/v1/contacts.json HTTP/1.1"));
        assertThat(mockServer.takeRequest().getRequestLine(), is("POST /api/v1/steps.json HTTP/1.1"));

        assertThat(Legacy.getSubmissionsDirectory().exists(), is(false));
    }

    /**
     * Utility to unzip a raw test resource into the given target directory
     */
    private void unzipResource(int rawResId, File targetDirectory) throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream input = context.getResources().openRawResource(rawResId);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(input));

        // see https://stackoverflow.com/a/27050680
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
                }
                if (ze.isDirectory()) {
                    continue;
                }
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                } finally {
                    fout.close();
                }
            }
        } finally {
            zis.close();
        }
    }
}
