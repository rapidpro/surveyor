package io.rapidpro.surveyor.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import androidx.test.rule.ActivityTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.is;

/**
 * Only works on newer emulators with microphone emulation
 */
@Ignore
public class CaptureAudioActivityTest extends BaseApplicationTest {
    @Rule
    public ActivityTestRule<CaptureAudioActivity> rule = new ActivityTestRule<>(CaptureAudioActivity.class, true, false);

    @Test
    public void capture() throws InterruptedException {
        File output = new File(getSurveyor().getExternalCacheDir(), "audio.m4a");

        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, output.getAbsolutePath());
        rule.launchActivity(intent);

        // start recording
        onView(withId(R.id.button_capture))
                .check(matches(isDisplayed()))
                .perform(click());

        Thread.sleep(2000);

        // stop recording
        onView(withId(R.id.button_capture)).perform(click());

        Instrumentation.ActivityResult result = rule.getActivityResult();

        assertThat(result.getResultCode(), is(Activity.RESULT_OK));
    }
}
