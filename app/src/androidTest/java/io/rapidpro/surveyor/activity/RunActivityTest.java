package io.rapidpro.surveyor.activity;

import android.content.Intent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import io.rapidpro.surveyor.widget.ChatBubbleView;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.AllOf.allOf;


public class RunActivityTest extends BaseApplicationTest {

    private static final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

    @Rule
    public IntentsTestRule<RunActivity> rule = new IntentsTestRule<>(RunActivity.class, true, false);

    @Before
    public void ensureLoggedIn() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        login("bob@nyaruka.com", Collections.singleton(ORG_UUID));
    }

    @Test
    public void twoQuestions() {
        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);
        intent.putExtra(SurveyorIntent.EXTRA_FLOW_UUID, "14ca824e-6607-4c11-82f5-18e298d0bd58");

        rule.launchActivity(intent);

        onView(allOf(withParent(withId(R.id.chat_history)), withClassName(is(ChatBubbleView.class.getName()))))
                .check(matches(isDisplayed()));
        onView(allOf(withParent(withClassName(is(ChatBubbleView.class.getName()))), withId(R.id.text_message)))
                .check(matches(withText("What is your favorite beer?")));

        sendTextReply("club");

        onView(allOf(withId(R.id.text_message), withText("club")))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.text_message), withText("Club is a great beer! What is your favorite color?")))
                .check(matches(isDisplayed()));

        sendTextReply("red");

        onView(allOf(withId(R.id.text_message), withText("red")))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.text_message), withText("Ok let's go get some Red Club!")))
                .check(matches(isDisplayed()));

        // check session is now complete
        onView(withText("Flow complete")).check(matches(isDisplayed()));
        onView(withText("Save")).check(matches(isDisplayed()));
        onView(withText("Discard")).check(matches(isDisplayed()));
    }

    private void sendTextReply(String text) {
        onView(withId(R.id.chat_compose)).perform(click(), typeText(text));
        onView(withId(R.id.button_send)).perform(click());
    }
}
