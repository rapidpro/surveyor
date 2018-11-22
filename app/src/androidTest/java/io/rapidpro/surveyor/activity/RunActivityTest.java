package io.rapidpro.surveyor.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import io.rapidpro.surveyor.utils.ImageUtils;
import io.rapidpro.surveyor.widget.ChatBubbleView;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;


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
    public void twoQuestions() throws Exception {
        launchForFlow("14ca824e-6607-4c11-82f5-18e298d0bd58");

        onView(allOf(withParent(withId(R.id.chat_history)), withClassName(is(ChatBubbleView.class.getName()))))
                .check(matches(isDisplayed()));
        onView(allOf(withParent(withClassName(is(ChatBubbleView.class.getName()))), withId(R.id.text_message)))
                .check(matches(withText("What is your favorite beer?")));

        sendTextReply("club");

        onView(allOf(withId(R.id.text_message), withText("club")))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.text_message), withText("Club is a great beer! What is your favorite color?")))
                .check(matches(isDisplayed()));

        // press back but cancel rather than lose this submission
        onView(withId(R.id.chat_compose)).perform(closeSoftKeyboard());
        onView(isRoot()).perform(pressBack());
        onView(withText("No")).perform(click());

        // do same from options menu
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.action_cancel)).perform(click());
        onView(withText("No")).perform(click());

        sendTextReply("red");

        onView(allOf(withId(R.id.text_message), withText("red")))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.text_message), withText("Ok let's go get some Red Club!")))
                .check(matches(isDisplayed()));

        // check session is now complete
        onView(withText("Flow complete")).check(matches(isDisplayed()));
        onView(withText("Save")).check(matches(isDisplayed()));
        onView(withText("Discard")).check(matches(isDisplayed()));

        // we should have a pending submission
        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        assertThat(getSurveyor().getSubmissionService().getPendingCount(org), is(1));

        // unless we discard it
        onView(withText("Discard")).perform(click());
        onView(withText("Yes")).perform(click());

        assertThat(getSurveyor().getSubmissionService().getPendingCount(org), is(0));
    }

    @Test
    public void multimedia() throws IOException {
        launchForFlow("585958f3-ee7a-4f81-b4c2-fda374155681");

        mockImageCaptureActivity(R.mipmap.ic_launcher);

        onView(allOf(withParent(withId(R.id.chat_history)), withClassName(is(ChatBubbleView.class.getName()))))
                .check(matches(isDisplayed()));
        onView(allOf(withParent(withClassName(is(ChatBubbleView.class.getName()))), withId(R.id.text_message)))
                .check(matches(withText("Hi there, please send a selfie")));

        onView(withId(R.id.container_request_media))
                .check(matches(isDisplayed()))
                .perform(click());
    }

    private void launchForFlow(String flowUuid) {
        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);
        intent.putExtra(SurveyorIntent.EXTRA_FLOW_UUID, flowUuid);

        rule.launchActivity(intent);
    }

    private void sendTextReply(String text) {
        onView(withId(R.id.chat_compose)).perform(click(), typeText(text));
        onView(withId(R.id.button_send)).perform(click());
    }

    private void mockImageCaptureActivity(int imageResId) throws IOException {
        // create a bitmap we can use for our simulated camera image
        Bitmap bmp = BitmapFactory.decodeResource(getInstrumentation().getTargetContext().getResources(), imageResId);

        byte[] asJpg = ImageUtils.convertToJPEG(bmp);
        File camerOutput = new File(getSurveyor().getStorageDirectory(), "camera.jpg");
        FileUtils.writeByteArrayToFile(camerOutput, asJpg);

        // create an activity result to look like the camera returning an image
        Intent resultData = new Intent();
        resultData.putExtra("data", bmp);
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);

        intending(toPackage("com.android.camera2")).respondWith(result);
    }
}
