package io.rapidpro.surveyor.activity;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.rule.ActivityTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webKeys;
import static org.hamcrest.CoreMatchers.containsString;

public class CreateAccountActivityTest extends BaseApplicationTest {

    @Rule
    public ActivityTestRule<CreateAccountActivity> rule = new ActivityTestRule<CreateAccountActivity>(CreateAccountActivity.class, true, false) {
        @Override
        protected void afterActivityLaunched() {
            // expresso-web tests require a webview with Javascript enabled
            onWebView(withId(R.id.webview)).forceJavascriptEnabled();
        }
    };

    @Test
    public void showWebView() throws IOException {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_get, "text/html", 200);

        rule.launchActivity(null);

        onWebView().withElement(findElement(Locator.ID, "id_surveyor_password"))
                .check(webMatches(getText(), containsString("")));
    }

    @Test
    public void showErrorOnIncorrectPassword() throws IOException {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_get, "text/html", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.org_surveyor_post_wrong_password, "text/html", 200);

        rule.launchActivity(null);

        onWebView().withElement(findElement(Locator.ID, "id_surveyor_password"))
                .perform(webClick())
                .perform(webKeys("wrong"));

        onWebView().withElement(findElement(Locator.CLASS_NAME, "btn"))
                .perform(webClick());

        onWebView().withElement(findElement(Locator.CSS_SELECTOR, ".errorlist li"))
                .check(webMatches(getText(), containsString("Invalid surveyor password, please check with your project leader and try again.")));

    }
}
