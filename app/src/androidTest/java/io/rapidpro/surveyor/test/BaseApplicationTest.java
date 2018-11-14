package io.rapidpro.surveyor.test;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorPreferences;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;

/**
 * Base for all the instrumented tests
 */
@RunWith(AndroidJUnit4.class)
public abstract class BaseApplicationTest {

    protected MockWebServer mockServer;

    @Before
    public void startMockServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        String mockServerURL = mockServer.url("/").toString();
        SurveyorApplication.LOG.d("mock server started at " + mockServerURL);

        getSurveyor().setPreference(SurveyorPreferences.HOST, mockServerURL);
        getSurveyor().onTembaHostChanged();
    }

    @After
    public void stopMockServer() throws IOException {
        mockServer.shutdown();

        SurveyorApplication.LOG.d("mock server stopped after " + mockServer.getRequestCount() + " requests");
    }

    /**
     * Clears all shared preferences after each test
     */
    @After
    public void clearPreferences() {
        SharedPreferences.Editor editor = getSurveyor().getPreferences().edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Clears the orgs directory after each test
     */
    @After
    public void clearOrgsDirectory() {
        FileUtils.deleteQuietly(getSurveyor().getOrgsDirectory());
    }

    protected SurveyorApplication getSurveyor() {
        return SurveyorApplication.get();
    }

    /**
     * Utility to appear logged in as the given user
     * @param email the email
     * @param orgUUIDs the set of accessible org UUIDs
     */
    protected void login(String email, Set<String> orgUUIDs) {
        getSurveyor().setPreference(SurveyorPreferences.AUTH_USERNAME, email);
        getSurveyor().setPreference(SurveyorPreferences.PREV_USERNAME, email);
        getSurveyor().setPreference(SurveyorPreferences.AUTH_ORGS, orgUUIDs);
    }

    /**
     * Utility to create an org directory
     * @param uuid the org UUID
     * @param detailsResId the resource ID of the details file
     */
    protected void installOrg(String uuid, int detailsResId) throws IOException {
        // create org directory
        File dir = new File(getSurveyor().getOrgsDirectory(), uuid);
        dir.mkdirs();

        // install details.json
        String detailsJSON = readRawResource(detailsResId);
        File detailsFile = new File(dir, "details.json");

        FileUtils.writeStringToFile(detailsFile, detailsJSON);
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
        mockServerResponse(readRawResource(rawResId), mimeType, code);
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

    private String readRawResource(int rawResId) throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream input = context.getResources().openRawResource(rawResId);
        return IOUtils.toString(input, StandardCharsets.UTF_8);
    }
}
