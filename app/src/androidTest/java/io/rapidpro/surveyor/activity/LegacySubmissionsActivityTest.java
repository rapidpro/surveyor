package io.rapidpro.surveyor.activity;

import android.os.Environment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.legacy.LegacySubmissionsActivity;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class LegacySubmissionsActivityTest extends BaseApplicationTest {

    private static final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

    @Rule
    public IntentsTestRule<LegacySubmissionsActivity> rule = new IntentsTestRule<>(LegacySubmissionsActivity.class, true, false);

    @Before
    public void ensureLoggedIn() {
        login("bob@nyaruka.com", Collections.singleton(ORG_UUID));
    }

    @Test
    public void showPendingSubmissions() throws IOException {
        unzipResource(io.rapidpro.surveyor.test.R.raw.legacy_submissions, Environment.getExternalStorageDirectory());

        rule.launchActivity(null);

        onView(withId(R.id.container_pending)).check(matches(isDisplayed()));
        onView(withId(R.id.button_pending)).check(matches(withText("2")));
    }
}
