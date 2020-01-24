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
import io.rapidpro.surveyor.SurveyorPreferences;
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
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;

public class OrgActivityTest extends BaseApplicationTest {

    private static final String ORG_UUID = "dc8123a1-168c-4962-ab9e-f784f3d804a2";

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

        onView(withText("No"))
                .check(matches(isDisplayed()))
                .perform(click());

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
    public void showRefreshConfirmationIfAssetsOutOfDate() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows_v12, io.rapidpro.surveyor.test.R.raw.org1_assets);

        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);

        rule.launchActivity(intent);

        onView(withText(R.string.confirm_org_refresh_old)).check(matches(isDisplayed()));
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
                        hasExtra(SurveyorIntent.EXTRA_FLOW_UUID, "bdd61538-5f50-4836-a8fb-acaafd64ddb1")
                )
        );
    }

    @Test
    public void showSubmitIfHasSubmissions() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        Flow flow1 = org.getFlow("bdd61538-5f50-4836-a8fb-acaafd64ddb1");

        SubmissionService svc = getSurveyor().getSubmissionService();
        Submission sub1 = svc.newSubmission(org, flow1);
        sub1.complete();
        Submission sub2 = svc.newSubmission(org, flow1);
        sub2.complete();
        svc.newSubmission(org, flow1);

        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);

        rule.launchActivity(intent);

        onView(withId(R.id.container_pending)).check(matches(isDisplayed()));
        onView(withId(R.id.button_pending)).check(matches(withText("2")));
    }

    @Test
    public void showLogoutConfirmationIfHasSubmissions() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        Flow flow1 = org.getFlow("bdd61538-5f50-4836-a8fb-acaafd64ddb1");

        SubmissionService svc = getSurveyor().getSubmissionService();
        Submission sub1 = svc.newSubmission(org, flow1);
        sub1.complete();
        Submission sub2 = svc.newSubmission(org, flow1);
        sub2.complete();
        svc.newSubmission(org, flow1);

        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);

        rule.launchActivity(intent);

        openOptionsMenu();

        onView(withText("Logout")).perform(click());
        onView(withText("No")).perform(click());

        // check that we're still logged in
        assertThat(getSurveyor().getPreferences().getString(SurveyorPreferences.AUTH_USERNAME, ""), is("bob@nyaruka.com"));

        // clear all submissions
        svc.clearAll();

        openOptionsMenu();

        onView(withText("Logout")).perform(click());

        // check that we're logged out
        assertThat(getSurveyor().getPreferences().getString(SurveyorPreferences.AUTH_USERNAME, ""), is(""));
    }
}
