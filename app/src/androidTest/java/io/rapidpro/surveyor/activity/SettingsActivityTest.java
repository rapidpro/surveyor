package io.rapidpro.surveyor.activity;

import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorPreferences;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.PreferenceMatchers.withKey;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;

public class SettingsActivityTest extends BaseApplicationTest {

    @Rule
    public ActivityTestRule<SettingsActivity> rule = new ActivityTestRule<>(SettingsActivity.class, true, false);

    @Test
    public void showHost() {
        rule.launchActivity(null);

        onData(withKey("host"))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withClassName(endsWith("EditText")))
                .perform(replaceText("http://test.com"))
                .perform(closeSoftKeyboard());
        onView(withText("OK")).perform(click());

        pause();

        // host should have been updated
        assertThat(getSurveyor().getTembaHost(), is("http://test.com"));

        // and user logged out
        assertThat(getSurveyor().getPreferences().getString(SurveyorPreferences.AUTH_USERNAME, ""), is(""));
        assertThat(getSurveyor().getPreferences().getStringSet(SurveyorPreferences.AUTH_ORGS, Collections.<String>emptySet()), is(Collections.<String>emptySet()));
    }
}
