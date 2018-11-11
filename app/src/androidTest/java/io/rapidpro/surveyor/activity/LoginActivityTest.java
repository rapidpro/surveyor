package io.rapidpro.surveyor.activity;

import android.content.Intent;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.rule.ActivityTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.SurveyorPreferences;
import io.rapidpro.surveyor.test.BaseActivityTest;
import okhttp3.mockwebserver.MockResponse;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

public class LoginActivityTest extends BaseActivityTest {

    @Rule
    public ActivityTestRule<LoginActivity> rule = new ActivityTestRule<>(LoginActivity.class, true, false);

    @Test
    public void showLoginForm() {
        rule.launchActivity(null);

        onView(withId(R.id.text_error_message)).check(matches(not(isDisplayed())));
        onView(withId(R.id.email)).check(matches(isDisplayed()));
        onView(withId(R.id.password)).check(matches(isDisplayed()));
    }

    @Test
    public void showErrorIfAuthenticationFails() {
        rule.launchActivity(null);

        m_server.enqueue(new MockResponse().setResponseCode(502));
        m_server.enqueue(new MockResponse().setResponseCode(500));
        m_server.enqueue(new MockResponse().setResponseCode(404));
        m_server.enqueue(new MockResponse().setResponseCode(403));

        onView(withId(R.id.email)).perform(click()).perform(typeText("bob@nyaruka.com"));
        onView(withId(R.id.password)).perform(click()).perform(typeText("Qwerty123")).perform(closeSoftKeyboard());
        onView(withId(R.id.email_sign_in_button)).perform(click());

        onView(withId(R.id.text_error_message))
                .check(matches(isDisplayed()))
                .check(matches(withText("Could not contact server. Check your network connection and try again.")));

        onView(withId(R.id.email_sign_in_button)).perform(click());

        onView(withId(R.id.text_error_message))
                .check(matches(withText("Server error, please try again later.")));

        onView(withId(R.id.email_sign_in_button)).perform(click());

        onView(withId(R.id.text_error_message))
                .check(matches(withText("Server is unavailable. Please check your settings or try again later.")));

        onView(withId(R.id.email_sign_in_button)).perform(click());

        onView(withId(R.id.text_error_message))
                .check(matches(withText("Invalid login. Check your username and password and try again.")));
    }

    @Test
    public void showPrevUsernameIfSet() {
        getSurveyor().setPreference(SurveyorPreferences.PREV_USERNAME, "bob@nyaruka.com");

        rule.launchActivity(null);

        onView(withId(R.id.email)).check(matches(withText("bob@nyaruka.com")));
    }

    @Test
    public void showErrorIfInIntent() {
        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ERROR, "I'm an error!");

        rule.launchActivity(intent);

        onView(withId(R.id.text_error_message))
                .check(matches(isDisplayed()))
                .check(matches(withText("I'm an error!")));
    }
}
