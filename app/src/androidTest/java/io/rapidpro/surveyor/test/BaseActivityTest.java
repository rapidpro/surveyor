package io.rapidpro.surveyor.test;

import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorPreferences;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
public abstract class BaseActivityTest {

    protected MockWebServer m_server;

    @Before
    public void startMockServer() throws IOException {
        m_server = new MockWebServer();
        m_server.start();

        String mockServerURL = m_server.url("/").toString();
        SurveyorApplication.LOG.d("mock server started at " + mockServerURL);

        getSurveyor().setPreference(SurveyorPreferences.HOST, mockServerURL);
        getSurveyor().onTembaHostChange();
    }

    @After
    public void stopMockServer() throws IOException {
        m_server.shutdown();
    }

    @After
    public void clearPreferences() {
        SharedPreferences.Editor editor = getSurveyor().getPreferences().edit();
        editor.clear();
        editor.apply();
    }

    protected SurveyorApplication getSurveyor() {
        return SurveyorApplication.get();
    }
}
