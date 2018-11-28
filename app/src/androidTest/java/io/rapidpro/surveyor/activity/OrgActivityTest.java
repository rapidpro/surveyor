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
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.data.SubmissionService;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;

public class OrgActivityTest extends BaseApplicationTest {

    private static final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

    @Rule
    public IntentsTestRule<OrgActivity> rule = new IntentsTestRule<>(OrgActivity.class, true, false);

    @Before
    public void ensureLoggedIn() {
        login("bob@nyaruka.com", Collections.singleton(ORG_UUID));
    }

    @Test
    public void showDownloadConfirmationIfAssetsNotDownloaded() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, 0, 0);

        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);

        rule.launchActivity(intent);

        onView(withText(R.string.confirm_org_download)).check(matches(isDisplayed()));

        /*

        TODO test refresh and list update, maybe https://medium.com/azimolabs/wait-for-it-idlingresource-and-conditionwatcher-602055f32356

        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_org_get, "application/json", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_fields_get_page_1, "application/json", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_fields_get_page_2, "application/json", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_groups_get, "application/json", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_flows_get, "application/json", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_definitions_get, "application/json", 200);

        onView(withText("Yes"))
                .check(matches(isDisplayed()))
                .perform(click());
        */
    }

    @Test
    public void dontShowDownloadConfirmationIfAssetsDownloaded() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);

        rule.launchActivity(intent);

        onView(withText(R.string.confirm_org_download)).check(doesNotExist());

        // check the flows are already listed
        onView(withText("Contact Details")).check(matches(isDisplayed()));
        onView(withText("Two Questions")).check(matches(isDisplayed()));
        onView(withText("Multimedia")).check(matches(isDisplayed()));

        // we have no pending submissions at this point so no submit button
        onView(withId(R.id.button_pending)).check(matches(not(isDisplayed())));

        // check that clicking a flow launches the flow activity
        onView(withText("Two Questions")).perform(click());

        intended(
                allOf(
                        hasComponent(FlowActivity.class.getName()),
                        hasExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID),
                        hasExtra(SurveyorIntent.EXTRA_FLOW_UUID, "14ca824e-6607-4c11-82f5-18e298d0bd58")
                )
        );
    }

    @Test
    public void showSubmitIfHasSubmissions() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        Flow flow1 = org.getFlow("14ca824e-6607-4c11-82f5-18e298d0bd58");

        SubmissionService svc = getSurveyor().getSubmissionService();
        svc.newSubmission(org, flow1);
        svc.newSubmission(org, flow1);

        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);

        rule.launchActivity(intent);

        onView(withId(R.id.container_pending)).check(matches(isDisplayed()));
        onView(withId(R.id.button_pending)).check(matches(withText("2")));
    }
}
