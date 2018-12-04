package io.rapidpro.surveyor.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.web.webdriver.Locator;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import okhttp3.mockwebserver.RecordedRequest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webContent;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.matcher.DomMatchers.hasElementWithId;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webKeys;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;


public class CreateAccountActivityTest extends BaseApplicationTest {

    @Rule
    public IntentsTestRule<CreateAccountActivity> rule = new IntentsTestRule<CreateAccountActivity>(CreateAccountActivity.class, true, false) {
        @Override
        protected void afterActivityLaunched() {
            // expresso-web tests require a webview with Javascript enabled
            onWebView(withId(R.id.webview)).forceJavascriptEnabled();
        }
    };

    @Before
    public void startTrackingIntents() {
        Intents.init();
    }

    @After
    public void stopTrackingIntents() {
        Intents.release();
    }

    @Test
    public void showWebView() throws IOException, InterruptedException {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_get, "text/html", 200);

        rule.launchActivity(null);

        RecordedRequest request1 = mockServer.takeRequest();
        assertThat(request1.getPath(), is("/org/surveyor/"));
        RecordedRequest request2 = mockServer.takeRequest();
        assertThat(request2.getPath(), is("/favicon.ico")); // mockserver automatically stubs this

        onWebView().check(webContent(hasElementWithId("id_surveyor_password")));
    }

    @Test
    public void showErrorOnIncorrectPassword() throws IOException, InterruptedException {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_get, "text/html", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_post_wrong_password, "text/html", 200);

        rule.launchActivity(null);

        RecordedRequest request1 = mockServer.takeRequest();
        assertThat(request1.getPath(), is("/org/surveyor/"));
        RecordedRequest request2 = mockServer.takeRequest();
        assertThat(request2.getPath(), is("/favicon.ico")); // mockserver automatically stubs this

        onWebView().withElement(findElement(Locator.ID, "id_surveyor_password"))
                .perform(webClick())
                .perform(webKeys("wrong"));
        onWebView().withElement(findElement(Locator.CLASS_NAME, "btn"))
                .perform(webClick());

        RecordedRequest request3 = mockServer.takeRequest();
        assertThat(request3.getPath(), is("/org/surveyor/"));

        onWebView().withElement(findElement(Locator.CSS_SELECTOR, ".errorlist li"))
                .check(webMatches(getText(), containsString("Invalid surveyor password, please check with your project leader and try again.")));
    }

    @Test
    public void showStep2OnCorrectPassword() throws IOException, InterruptedException {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_get, "text/html", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_post_correct_password, "text/html", 200);

        rule.launchActivity(null);

        RecordedRequest request1 = mockServer.takeRequest();
        assertThat(request1.getPath(), is("/org/surveyor/"));
        RecordedRequest request2 = mockServer.takeRequest();
        assertThat(request2.getPath(), is("/favicon.ico")); // mockserver automatically stubs this

        onWebView().withElement(findElement(Locator.ID, "id_surveyor_password"))
                .perform(webClick())
                .perform(webKeys("surv3ys"));
        onWebView().withElement(findElement(Locator.CLASS_NAME, "btn"))
                .perform(webClick());

        RecordedRequest request3 = mockServer.takeRequest();
        assertThat(request3.getPath(), is("/org/surveyor/"));

        onWebView().check(webContent(hasElementWithId("id_first_name")));
        onWebView().check(webContent(hasElementWithId("id_last_name")));
    }

    @Test
    public void loginIfStep2Successful() throws IOException, InterruptedException {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_get, "text/html", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_post_correct_password, "text/html", 200);

        rule.launchActivity(null);

        // step 1

        onWebView().withElement(findElement(Locator.ID, "id_surveyor_password"))
                .perform(webClick())
                .perform(webKeys("surv3ys"));
        onWebView().withElement(findElement(Locator.CLASS_NAME, "btn"))
                .perform(webClick());

        RecordedRequest request1 = mockServer.takeRequest();
        assertThat(request1.getPath(), is("/org/surveyor/"));
        RecordedRequest request2 = mockServer.takeRequest();
        assertThat(request2.getPath(), is("/favicon.ico"));
        RecordedRequest request3 = mockServer.takeRequest();
        assertThat(request3.getPath(), is("/org/surveyor/"));

        // step 2

        mockServerRedirect("/org/surveyor/?org=Nyaruka&token=abc123&user=bob@nyaruka.com&uuid=dc8123a1-168c-4962-ab9e-f784f3d804a2");
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_get, "text/html", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_org_get, "text/html", 200);

        onWebView().withElement(findElement(Locator.ID, "id_first_name"))
                .perform(webClick())
                .perform(webKeys("Bob"));
        onWebView().withElement(findElement(Locator.ID, "id_last_name"))
                .perform(webClick())
                .perform(webKeys("Smith"));
        onWebView().withElement(findElement(Locator.ID, "id_email"))
                .perform(webClick())
                .perform(webKeys("bob@nyaruka.com"));
        onWebView().withElement(findElement(Locator.ID, "id_password"))
                .perform(webClick())
                .perform(webKeys("Qwerty123"));

        onWebView().withElement(findElement(Locator.CLASS_NAME, "btn"))
                .perform(webClick());

        RecordedRequest request4 = mockServer.takeRequest();
        assertThat(request4.getPath(), is("/org/surveyor/"));
        RecordedRequest request5 = mockServer.takeRequest();
        assertThat(request5.getPath(), is("/org/surveyor/?org=Nyaruka&token=abc123&user=bob@nyaruka.com&uuid=dc8123a1-168c-4962-ab9e-f784f3d804a2"));
        RecordedRequest request6 = mockServer.takeRequest();
        assertThat(request6.getPath(), is("/api/v2/org.json"));

        intended(hasComponent(OrgActivity.class.getName()));
    }

    /**
     * @see BaseActivity#sendBugReport()
     * <p>
     * tested here because we need a IntentsTestRule based test
     */
    @Test
    public void sendBugReport() throws IOException {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_get, "text/html", 200);

        // mock the intent to pick an app to send the bug report too
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        rule.launchActivity(null);

        openOptionsMenu();
        onView(withText("Bug Report"))
                .perform(click());

        // check intent was launched
        intended(hasAction(Intent.ACTION_CHOOSER));
    }
}
