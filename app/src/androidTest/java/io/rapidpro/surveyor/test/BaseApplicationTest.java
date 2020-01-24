package io.rapidpro.surveyor.test;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorPreferences;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;

/**
 * Base for all the instrumented tests
 */
public abstract class BaseApplicationTest {

    @Rule
    public TestRule logger = new TestWatcher() {
        protected void starting(Description description) {
            Logger.d("========= Starting test: " + description.getClassName() + "#" + description.getMethodName() + " =========");
        }
    };
    protected MockWebServer mockServer;

    @Before
    public void startMockServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        String mockServerURL = mockServer.url("/").toString();
        Logger.d("Mock server started at " + mockServerURL);

        getSurveyor().setPreference(SurveyorPreferences.HOST, mockServerURL);
        getSurveyor().onTembaHostChanged();
    }

    @After
    public void stopMockServer() throws IOException {
        mockServer.shutdown();

        Logger.d("Mock server stopped after " + mockServer.getRequestCount() + " requests");
    }

    /**
     * Clears the preferences and file system after each test
     */
    @After
    public void clearData() throws IOException {
        SharedPreferences.Editor editor = getSurveyor().getPreferences().edit();
        editor.clear();
        editor.apply();

        FileUtils.deleteQuietly(getSurveyor().getOrgsDirectory());
        FileUtils.deleteQuietly(getSurveyor().getUserDirectory());

        getSurveyor().getOrgService().clearCache();
    }

    protected SurveyorApplication getSurveyor() {
        return SurveyorApplication.get();
    }

    /**
     * Utility to appear logged in as the given user
     *
     * @param email    the email
     * @param orgUUIDs the set of accessible org UUIDs
     */
    protected void login(String email, Set<String> orgUUIDs) {
        getSurveyor().setPreference(SurveyorPreferences.AUTH_USERNAME, email);
        getSurveyor().setPreference(SurveyorPreferences.PREV_USERNAME, email);
        getSurveyor().setPreference(SurveyorPreferences.AUTH_ORGS, orgUUIDs);
    }

    /**
     * Utility to create an org directory
     *
     * @param uuid         the org UUID
     * @param detailsResId the resource ID of the details file
     */
    protected void installOrg(String uuid, int detailsResId, int flowsResId, int assetsResId) throws IOException {
        // create org directory
        File dir = new File(getSurveyor().getOrgsDirectory(), uuid);
        dir.mkdirs();

        // install details.json
        String detailsJSON = readResourceAsString(detailsResId);
        FileUtils.writeStringToFile(new File(dir, "details.json"), detailsJSON);

        if (flowsResId > 0) {
            // install flows.json
            String flowsJSON = readResourceAsString(flowsResId);
            FileUtils.writeStringToFile(new File(dir, "flows.json"), flowsJSON);
        } else {
            // a valid org must have details.json and flows.json
            FileUtils.writeStringToFile(new File(dir, "flows.json"), "[]");
        }

        if (assetsResId > 0) {
            // install assets.json
            String assetsJSON = readResourceAsString(assetsResId);
            FileUtils.writeStringToFile(new File(dir, "assets.json"), assetsJSON);
        }
    }

    protected void openOptionsMenu() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        openActionBarOverflowOrOptionsMenu(context);

        // especially on Travis, we need to give the emulator a bit of time to actually open the menu
        pause();
    }

    /**
     * Enqueues a response on the mock HTTP server from the given body, MIME type and status code
     */
    protected void mockServerResponse(String body, String mimeType, int code) {
        MockResponse response = new MockResponse()
                .setBody(body)
                .setResponseCode(code)
                .addHeader("Content-Type", mimeType + "; charset=utf-8")
                .addHeader("Cache-Control", "no-cache");

        mockServer.enqueue(response);
    }

    /**
     * Enqueues a response on the mock HTTP server from the given resource file and MIME type and status code
     */
    protected void mockServerResponse(int rawResId, String mimeType, int code) throws IOException {
        mockServerResponse(readResourceAsString(rawResId), mimeType, code);
    }

    /**
     * Enqueues a redirect response on the mock HTTP server
     */
    protected void mockServerRedirect(String location) {
        MockResponse response = new MockResponse()
                .setResponseCode(HTTP_MOVED_TEMP)
                .setHeader("Location", location);

        mockServer.enqueue(response);
    }

    /**
     * Copies a raw test resource to the given file (more efficient than loading into memory first)
     */
    protected void copyResource(int rawResId, File dest) throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream input = context.getResources().openRawResource(rawResId);
        FileUtils.copyInputStreamToFile(input, dest);
    }

    protected String readResourceAsString(int rawResId) throws IOException {
        return IOUtils.toString(readResource(rawResId), "UTF-8");
    }

    protected byte[] readResource(int rawResId) throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream input = context.getResources().openRawResource(rawResId);
        return IOUtils.toByteArray(input);
    }

    /**
     * Utility to unzip a raw test resource into the given target directory
     */
    protected void unzipResource(int rawResId, File targetDirectory) throws IOException {
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

    /**
     * Pauses the testing thread for a configurable amount of time to allow UI changes in a different
     * thread to complete.
     */
    protected void pause() {
        Logger.d("Pausing test for " + TestRunner.PAUSE_MILLIS + " milliseconds...");

        try {
            Thread.sleep(TestRunner.PAUSE_MILLIS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
