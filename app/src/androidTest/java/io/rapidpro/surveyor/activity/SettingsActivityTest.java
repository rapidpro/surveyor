package io.rapidpro.surveyor.activity;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.rule.ActivityTestRule;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
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

        onView(withClassName(endsWith("EditText"))).perform(replaceText("http://test.com"));
        onView(withText("OK")).perform(click());

        assertThat(getSurveyor().getTembaHost(), is("http://test.com"));
    }
}
