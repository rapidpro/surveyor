package io.rapidpro.surveyor.legacy;

import android.os.Environment;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import androidx.test.filters.FlakyTest;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.net.TembaException;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import io.rapidpro.surveyor.test.R;
import io.rapidpro.surveyor.utils.SurveyUtils;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

@FlakyTest(detail = "Seems near impossible to get it to work on Travis due to the need for a WRITE_EXTERNAL_STORAGE permission.")
public class LegacyTest extends BaseApplicationTest {

    private static final String ORG_UUID = "dc8123a1-168c-4962-ab9e-f784f3d804a2";

    @Before
    public void setup() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        FileUtils.deleteQuietly(new File(Environment.getExternalStorageDirectory(), "Surveyor"));
    }

    @Test
    public void cleanup() throws IOException {
        unzipResource(R.raw.legacy_files, getSurveyor().getFilesDir());

        File orgsDir = new File(getSurveyor().getFilesDir(), "orgs");
        assertThat(orgsDir.exists(), is(true));

        File org1Dir = new File(orgsDir, "1");

        assertThat(org1Dir.exists(), is(true));
        assertThat(Legacy.isCleanupNeeded(), is(true));

        Legacy.getStorageDirectory().mkdirs();
        Legacy.getSubmissionsDirectory().mkdirs();
        File orgSubmissionsDir = SurveyUtils.mkdir(Legacy.getSubmissionsDirectory(), "1");

        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        Legacy.cleanupOrgs(Collections.singleton(org));

        assertThat(org.getLegacySubmissionsDirectory(), endsWith("/Surveyor/submissions/1"));

        // org config directory should be deleted
        assertThat(org1Dir.exists(), is(false));

        // but org submissions directory still exists
        assertThat(orgSubmissionsDir.exists(), is(true));
    }

    @Test
    public void getCompletedCount_whenStorageDirNotExists() throws IOException {
        Org org = getSurveyor().getOrgService().get(ORG_UUID);

        assertThat(Legacy.getCompletedCount(org), is(0));
    }

    @Test
    public void getCompletedCount_whenSubmissionsDirNotExists() throws IOException {
        SurveyUtils.mkdir(Environment.getExternalStorageDirectory(), "Surveyor");

        Org org = getSurveyor().getOrgService().get(ORG_UUID);

        assertThat(Legacy.getCompletedCount(org), is(0));
    }

    @Test
    public void submit() throws IOException, TembaException, InterruptedException {
        unzipResource(R.raw.legacy_files, getSurveyor().getFilesDir());
        unzipResource(R.raw.legacy_submissions, Environment.getExternalStorageDirectory());

        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        Legacy.cleanupOrgs(Collections.singleton(org));

        assertThat(org.getLegacySubmissionsDirectory(), endsWith("/Surveyor/submissions/1"));
        assertThat(Legacy.getCompletedCount(org), is(2));

        List<File> legacySubs = Legacy.getCompleted(org);
        assertThat(legacySubs, hasSize(2));
        assertThat(legacySubs.get(0).getName(), is("1_6bfd671d-6aac-4d9e-b72d-045c31e450c9.json"));
        assertThat(legacySubs.get(1).getName(), is("2_1ea17864-06a5-4f76-a405-9a605ae89098.json"));

        mockServerResponse("{\"uuid\":\"d3acd0e4-8a75-419f-96cb-7699d9f5f193\"}", "application/json", 200);
        mockServerResponse("{\"error\":null}", "application/json", 200);

        // submit for Two Questions flow (no media)
        Legacy.submit(legacySubs.get(0), "797d44ef78f7845de0f4dbb42d5174505563dd77", "bob@nyaruka.com");

        RecordedRequest request1 = mockServer.takeRequest();
        assertThat(request1.getRequestLine(), is("POST /api/v1/contacts.json HTTP/1.1"));
        assertThat(request1.getHeader("Authorization"), is("Token 797d44ef78f7845de0f4dbb42d5174505563dd77"));
        assertThat(mockServer.takeRequest().getRequestLine(), is("POST /api/v1/steps.json HTTP/1.1"));

        // submission file and parent folder should have been deleted
        assertThat(legacySubs.get(0).exists(), is(false));
        assertThat(legacySubs.get(0).getParentFile().exists(), is(false));

        mockServerResponse("{\"uuid\":\"d3acd0e4-8a75-419f-96cb-7699d9f5f193\"}", "application/json", 200);
        mockServerResponse("{\"location\":\"http://uploads.rapidpro.io/image.jpg\"}", "application/json", 200);
        mockServerResponse("{\"location\":\"http://uploads.rapidpro.io/audio.m4a\"}", "application/json", 200);
        mockServerResponse("{\"error\":null}", "application/json", 200);

        // submit for Multimedia flow (image and audio media)
        Legacy.submit(legacySubs.get(1), "797d44ef78f7845de0f4dbb42d5174505563dd77", "bob@nyaruka.com");

        assertThat(mockServer.takeRequest().getRequestLine(), is("POST /api/v1/contacts.json HTTP/1.1"));
        assertThat(mockServer.takeRequest().getRequestLine(), is("POST /api/v2/media.json HTTP/1.1"));
        assertThat(mockServer.takeRequest().getRequestLine(), is("POST /api/v2/media.json HTTP/1.1"));
        assertThat(mockServer.takeRequest().getRequestLine(), is("POST /api/v1/steps.json HTTP/1.1"));

        // submission file should have been deleted
        assertThat(legacySubs.get(1).exists(), is(false));
        assertThat(legacySubs.get(0).getParentFile().exists(), is(false));
    }
}
