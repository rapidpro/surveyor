package io.rapidpro.surveyor.activity;

import android.content.Intent;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.rule.ActivityTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.SurveyorPreferences;
import io.rapidpro.surveyor.test.BaseActivityTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
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
        onView(withId(R.id.email)).check(matches(isCompletelyDisplayed()));
        onView(withId(R.id.password)).check(matches(isCompletelyDisplayed()));
    }

    @Test
    public void showPrevUsernameIfSet() {
        setPreference(SurveyorPreferences.PREV_USERNAME, "bob@nyaruka.com");

        rule.launchActivity(null);

        onView(withId(R.id.email)).check(matches(withText("bob@nyaruka.com")));
    }

    @Test
    public void showErrorIfInIntent() {
        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ERROR, "I'm an error!");

        rule.launchActivity(intent);

        onView(withId(R.id.text_error_message)).check(matches(isCompletelyDisplayed()));
        onView(withId(R.id.text_error_message)).check(matches(withText("I'm an error!")));
    }
}
