package io.rapidpro.surveyor.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import androidx.test.espresso.intent.ActivityResultFunction;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import io.rapidpro.surveyor.utils.ImageUtils;
import io.rapidpro.surveyor.widget.ChatBubbleView;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
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
        launchForFlow("bdd61538-5f50-4836-a8fb-acaafd64ddb1");

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
        openOptionsMenu();
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

        // we should have a incomplete submission
        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        assertThat(getSurveyor().getSubmissionService().getCompletedCount(org), is(0));

        // unless we save it
        onView(withText("Save")).perform(click());

        assertThat(getSurveyor().getSubmissionService().getCompletedCount(org), is(1));
    }

    @Test
    public void multimedia() {
        launchForFlow("e54809ba-2f28-439b-b90b-c623eafa05ae");

        mockMediaCapturing();

        onView(allOf(withId(R.id.text_message), withText("Hi there, please send a selfie")))
                .check(matches(isDisplayed()));

        onView(withId(R.id.container_request_media))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(allOf(withId(R.id.text_message), withText("Now send a video")))
                .check(matches(isDisplayed()));

        onView(withId(R.id.container_request_media))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(allOf(withId(R.id.text_message), withText("Now send an audio recording")))
                .check(matches(isDisplayed()));

        onView(withId(R.id.container_request_media))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(allOf(withId(R.id.text_message), withText("Finally please send your location")))
                .check(matches(isDisplayed()));

        // TODO mock location capturing
    }

    private void launchForFlow(String flowUuid) {
        Intent intent = new Intent();
        intent.putExtra(SurveyorIntent.EXTRA_ORG_UUID, ORG_UUID);
        intent.putExtra(SurveyorIntent.EXTRA_FLOW_UUID, flowUuid);

        BaseActivity activity = rule.launchActivity(intent);

        assertThat(activity, is(notNullValue()));
    }

    private void sendTextReply(String text) {
        onView(withId(R.id.chat_compose)).perform(click(), typeText(text));
        onView(withId(R.id.button_send)).perform(click());
    }

    private void mockMediaCapturing() {
        final int imageResId = io.rapidpro.surveyor.test.R.raw.capture_image;
        final int videoResId = io.rapidpro.surveyor.test.R.raw.capture_video;
        final int audioResId = io.rapidpro.surveyor.test.R.raw.capture_audio;
        final Context context = getInstrumentation().getContext();

        ActivityResultFunction mockCamera = new ActivityResultFunction() {
            @Override
            public Instrumentation.ActivityResult apply(Intent intent) {
                SurveyorApplication.LOG.d("Handling mocked image capture intent");

                // create a bitmap we can use for our simulated camera image
                Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), imageResId);

                byte[] asJpg = ImageUtils.convertToJPEG(bmp);

                try {
                    File output = new File(getSurveyor().getExternalCacheDir(), "camera.jpg");
                    FileUtils.writeByteArrayToFile(output, asJpg);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // create an activity result to look like the camera returning an image
                Intent resultData = new Intent();
                resultData.putExtra("data", bmp);
                return new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
            }
        };

        intending(toPackage("com.android.camera")).respondWithFunction(mockCamera);
        intending(toPackage("com.android.camera2")).respondWithFunction(mockCamera);

        intending(hasComponent(CaptureVideoActivity.class.getName())).respondWithFunction(new ActivityResultFunction() {
            @Override
            public Instrumentation.ActivityResult apply(Intent intent) {
                SurveyorApplication.LOG.d("Handling mocked video capture intent");

                InputStream input = context.getResources().openRawResource(videoResId);

                try {
                    File output = new File(getSurveyor().getExternalCacheDir(), "video.mp4");
                    FileUtils.copyInputStreamToFile(input, output);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent resultData = new Intent();
                return new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
            }
        });

        intending(hasComponent(CaptureAudioActivity.class.getName())).respondWithFunction(new ActivityResultFunction() {
            @Override
            public Instrumentation.ActivityResult apply(Intent intent) {
                SurveyorApplication.LOG.d("Handling mocked audio capture intent");

                InputStream input = context.getResources().openRawResource(audioResId);

                try {
                    File output = new File(getSurveyor().getExternalCacheDir(), "audio.m4a");
                    FileUtils.copyInputStreamToFile(input, output);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent resultData = new Intent();
                return new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);
            }
        });
    }
}
