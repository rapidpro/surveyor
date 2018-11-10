package io.rapidpro.surveyor.activity;

import android.content.Intent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityTestRule<LoginActivity> rule = new ActivityTestRule<>(LoginActivity.class, true, false);

    @Test
    public void showErrorIfInIntent() {
        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ERROR, "I'm an error!");

        rule.launchActivity(intent);

        onView(withId(R.id.password)).check(matches(withText("")));

        onView(withId(R.id.text_error_message)).check(matches(withText("I'm an error!")));
    }
}
