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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;

public class FlowActivityTest extends BaseApplicationTest {

    private static final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";
    private static final String FLOW_UUID = "bdd61538-5f50-4836-a8fb-acaafd64ddb1";

    @Rule
    public IntentsTestRule<FlowActivity> rule = new IntentsTestRule<>(FlowActivity.class, true, false);

    @Before
    public void ensureLoggedIn() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        login("bob@nyaruka.com", Collections.singleton(ORG_UUID));
    }

    @Test
    public void showFlowDetails() {
        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);
        intent.putExtra(SurveyorIntent.EXTRA_FLOW_UUID, FLOW_UUID);

        rule.launchActivity(intent);

        onView(withId(R.id.text_flow_name)).check(matches(withText("Two Questions")));
        onView(withId(R.id.text_flow_revision)).check(matches(withText("(v24)")));
        onView(withId(R.id.text_flow_questions)).check(matches(withText("2 Questions")));

        // check that clicking start launches the run activity
        onView(withText("Start Flow")).perform(click());

        intended(
                allOf(
                        hasComponent(RunActivity.class.getName()),
                        hasExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID),
                        hasExtra(SurveyorIntent.EXTRA_FLOW_UUID, FLOW_UUID)
                )
        );
    }
}
