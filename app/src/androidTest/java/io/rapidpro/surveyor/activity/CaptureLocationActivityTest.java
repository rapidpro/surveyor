package io.rapidpro.surveyor.activity;

import android.app.Instrumentation;

import org.junit.Rule;
import org.junit.Test;

import androidx.test.filters.FlakyTest;
import androidx.test.rule.ActivityTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class CaptureLocationActivityTest extends BaseApplicationTest {

    @Rule
    public ActivityTestRule<CaptureLocationActivity> rule = new ActivityTestRule<>(CaptureLocationActivity.class);

    @FlakyTest(detail = "This test can only work after the Maps app has been opened and the T&C accepted")
    @Test
    public void capture() {
        onView(withId(R.id.button_capture))
                .check(matches(isDisplayed()))
                .perform(click());

        Instrumentation.ActivityResult result = rule.getActivityResult();
        assertThat(result.getResultData(), is(not(nullValue())));

        // emulators always return Google HQ
        assertThat(result.getResultData().getDoubleExtra("latitude", 0.0d), is(37.4219983));
        assertThat(result.getResultData().getDoubleExtra("longitude", 0.0d), is(-122.084000));
    }
}
