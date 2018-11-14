package io.rapidpro.surveyor.activity;

import android.content.Intent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import androidx.test.rule.ActivityTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class OrgActivityTest extends BaseApplicationTest {

    private static final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

    @Rule
    public ActivityTestRule<OrgActivity> rule = new ActivityTestRule<>(OrgActivity.class, true, false);

    @Before
    public void setup() throws IOException {
        login("bob@nyaruka.com", Collections.singleton(ORG_UUID));

        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details);
    }

    @Test
    public void showDownloadConfirmationIfAssetsNotDownloaded() {
        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);

        rule.launchActivity(intent);

        onView(withText(R.string.confirm_org_download)).check(matches(isDisplayed()));
    }
}
