package io.rapidpro.surveyor.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.greysonparrelli.permiso.Permiso;
import com.nyaruka.goflow.mobile.Environment;
import com.nyaruka.goflow.mobile.Event;
import com.nyaruka.goflow.mobile.Hint;
import com.nyaruka.goflow.mobile.MsgIn;
import com.nyaruka.goflow.mobile.Resume;
import com.nyaruka.goflow.mobile.SessionAssets;
import com.nyaruka.goflow.mobile.Trigger;

import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.engine.Contact;
import io.rapidpro.surveyor.engine.Engine;
import io.rapidpro.surveyor.engine.EngineException;
import io.rapidpro.surveyor.engine.Session;
import io.rapidpro.surveyor.engine.Sprint;
import io.rapidpro.surveyor.ui.IconTextView;
import io.rapidpro.surveyor.ui.ViewCache;
import io.rapidpro.surveyor.utils.ImageUtils;
import io.rapidpro.surveyor.widget.ChatBubbleView;
import io.rapidpro.surveyor.widget.IconLinkView;

public class RunActivity extends BaseActivity {

    // the different types of requests for media
    public static final String REQUEST_IMAGE = "image";
    public static final String REQUEST_AUDIO = "audio";
    public static final String REQUEST_VIDEO = "video";
    public static final String REQUEST_GPS = "geo";

    // custom request codes passed to media capture activities
    private static final int RESULT_IMAGE = 1;
    private static final int RESULT_VIDEO = 2;
    private static final int RESULT_AUDIO = 3;
    private static final int RESULT_GPS = 4;

    private static final int MAX_IMAGE_DIMENSION = 1024;
    private static final int MAX_THUMB_DIMENSION = 600;

    private LinearLayout chatHistory;
    private IconTextView sendButtom;
    private EditText chatCompose;
    private ScrollView scrollView;

    private Session session;
    private Submission submission;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String orgUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_ORG_UUID);
        String flowUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_FLOW_UUID);

        setContentView(R.layout.activity_run);
        initUI();

        try {
            Org org = getSurveyor().getOrgService().get(orgUUID);
            SessionAssets assets = Engine.createSessionAssets(Engine.loadAssets(org.getAssets()));
            Environment environment = Engine.createEnvironment(org);

            Flow flow = org.getFlow(flowUUID);
            setTitle(flow.getName());

            Trigger trigger = Engine.createManualTrigger(environment, Contact.createEmpty(assets), flow.toReference());

            Pair<Session, Sprint> ss = Engine.getInstance().newSession(assets, trigger);
            session = ss.getLeft();
            submission = getSurveyor().getSubmissionService().newSubmission(org, flow);

            handleEngineSprint(ss.getRight());

        } catch (EngineException | IOException e) {
            handleProblem("Unable to start flow", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        initUI();
    }

    private void initUI() {
        chatHistory = findViewById(R.id.chat_history);
        chatCompose = findViewById(R.id.chat_compose);
        sendButtom = findViewById(R.id.button_send);
        scrollView = findViewById(R.id.scroll);

        // allow messages to be sent with the enter key
        chatCompose.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    onActionSend(sendButtom);
                    return true;
                }
                return false;
            }
        });

        // or the send button on the keyboard
        chatCompose.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && actionId == EditorInfo.IME_ACTION_SEND && event.getAction() == KeyEvent.ACTION_DOWN) {
                    onActionSend(sendButtom);
                    return true;
                }
                return false;
            }
        });

        // change the color of the send button when there is text in the compose box
        chatCompose.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    sendButtom.setIconColor(R.color.tertiary_light);
                } else {
                    sendButtom.setIconColor(R.color.light_gray);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onBackPressed() {
        confirmDiscardRun();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_run, menu);
        return true;
    }

    /**
     * User pressed the media request button
     */
    public void onActionMedia(View view) {
        View media = getViewCache().getView(R.id.media_icon);
        if (session.isWaiting()) {
            if (REQUEST_IMAGE.equals(media.getTag())) {
                captureImage();
            } else if (REQUEST_VIDEO.equals(media.getTag())) {
                captureVideo();
            } else if (REQUEST_AUDIO.equals(media.getTag())) {
                captureAudio();
            } else if (REQUEST_GPS.equals(media.getTag())) {
                captureLocation();
            }
        }
    }

    /**
     * Captures an image from the camera
     */
    private void captureImage() {

        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            @SuppressWarnings("ResourceType")
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    ComponentName cameraPkg = intent.resolveActivity(getPackageManager());

                    if (cameraPkg == null) {
                        handleProblem("Can't find camera device", null);
                        return;
                    }
                    Logger.d("Camera package is " + cameraPkg.toString());

                    File cameraOutput = getCameraOutput();
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, getSurveyor().getUriForFile(cameraOutput));
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivityForResult(intent, RESULT_IMAGE);
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                RunActivity.this.showRationaleDialog(R.string.permission_camera, callback);
            }

        }, Manifest.permission.CAMERA);
    }

    /**
     * Captures a video from the camera
     */
    private void captureVideo() {
        Intent intent = new Intent(this, CaptureVideoActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, getVideoOutput().getAbsolutePath());
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, RESULT_VIDEO);
    }

    /**
     * Captures an audio recording from the microphone
     */
    private void captureAudio() {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            @SuppressWarnings("ResourceType")
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    Intent intent = new Intent(RunActivity.this, CaptureAudioActivity.class);
                    intent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, getAudioOutput().getAbsolutePath());
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivityForResult(intent, RESULT_AUDIO);
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                RunActivity.this.showRationaleDialog(R.string.permission_record, callback);
            }

        }, Manifest.permission.RECORD_AUDIO);
    }

    /**
     * Captures the current location
     */
    private void captureLocation() {
        Intent intent = new Intent(this, CaptureLocationActivity.class);
        startActivityForResult(intent, RESULT_GPS);
    }

    private File getCameraOutput() {
        return new File(getSurveyor().getExternalCacheDir(), "camera.jpg");
    }

    private File getVideoOutput() {
        return new File(getSurveyor().getExternalCacheDir(), "video.mp4");
    }

    private File getAudioOutput() {
        return new File(getSurveyor().getExternalCacheDir(), "audio.m4a");
    }

    /**
     * @see android.app.Activity#onActivityResult(int, int, Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        MsgIn msg = null;

        try {
            if (requestCode == RESULT_IMAGE) {
                File output = getCameraOutput();
                if (output.exists()) {
                    Bitmap full = BitmapFactory.decodeFile(output.getAbsolutePath());
                    Bitmap scaled = ImageUtils.scaleToMax(full, MAX_IMAGE_DIMENSION);

                    // correct rotation if necessary
                    int rotation = ImageUtils.getExifRotation(output.getAbsolutePath());
                    if (rotation != 0) {
                        Logger.d("Correcting EXIF rotation of " + rotation + " degrees");

                        scaled = ImageUtils.rotateImage(scaled, rotation);
                    }

                    // encode as JPEG and save to submission
                    byte[] asJpg = ImageUtils.convertToJPEG(scaled);
                    Uri uri = submission.saveMedia(asJpg, "jpg");

                    Logger.d("Saved image capture to " + uri);

                    // create thumbnail and add to chat
                    Bitmap thumb = ImageUtils.scaleToMax(scaled, MAX_THUMB_DIMENSION);
                    addMedia(thumb, uri.toString(), R.string.media_image);

                    msg = Engine.createMsgIn("", "image/jpeg:" + uri);

                    output.delete();
                }
            } else if (requestCode == RESULT_VIDEO) {
                File output = getVideoOutput();
                if (output.exists()) {
                    Bitmap thumb = ImageUtils.thumbnailFromVideo(output);

                    Uri uri = submission.saveMedia(output);

                    addMedia(thumb, uri.toString(), R.string.media_video);

                    Logger.d("Saved video capture to " + uri);

                    msg = Engine.createMsgIn("", "video/mp4:" + uri);

                    output.delete();
                }

            } else if (requestCode == RESULT_AUDIO) {
                File output = getAudioOutput();
                if (output.exists()) {
                    Uri uri = submission.saveMedia(output);
                    Logger.d("Saved audio capture to " + uri);

                    addMediaLink(getString(R.string.made_recording), uri.toString(), R.string.media_audio);

                    msg = Engine.createMsgIn("", "audio/mp4:" + uri);

                    output.delete();
                }
            } else if (requestCode == RESULT_GPS) {
                double latitude = data.getDoubleExtra("latitude", 0.0);
                double longitude = data.getDoubleExtra("longitude", 0.0);

                String coords = "geo:" + latitude + "," + longitude;

                String url = coords + "?q=" + latitude + "," + longitude + "(Location)";
                addMediaLink(latitude + "," + longitude, url, R.string.media_location);

                msg = Engine.createMsgIn("", coords);
            }
        } catch (IOException e) {
            handleProblem("Unable capture media", e);
        }

        // if we have a message we can try to resume now...
        if (msg != null) {
            resumeSession(msg);
        }
    }

    /**
     * Something has gone wrong... show the user the big report dialog
     */
    private void handleProblem(String toastMsg, Throwable e) {
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();

        if (e != null) {
            Logger.e("Error running flow", e);
            showBugReportDialog();
        }

        finish();
    }

    private void resumeSession(MsgIn msg) {
        try {
            Resume resume = Engine.createMsgResume(null, null, msg);
            Sprint sprint = session.resume(resume);

            handleEngineSprint(sprint);

        } catch (EngineException | IOException e) {
            handleProblem("Couldn't handle message", e);
        }

        // scroll us to the bottom
        scrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollView.setSmoothScrollingEnabled(true);
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);

                // put the focus back on the chat box
                chatCompose.requestFocus();
            }
        }, 100);

        // refresh our menu
        invalidateOptionsMenu();
    }

    /**
     * User pressed the send button
     */
    public void onActionSend(View sendButton) {
        if (!session.getStatus().equals("waiting")) {
            return;
        }

        EditText chatBox = findViewById(R.id.chat_compose);
        String message = chatBox.getText().toString();

        if (message.trim().length() > 0) {
            chatBox.setText("");

            MsgIn msg = Engine.createMsgIn(message);

            addMessage(message, true);

            resumeSession(msg);
        }
    }

    /**
     * Handles new session state and events after interaction with the flow engine
     *
     * @param sprint the sprint from the engine
     */
    private void handleEngineSprint(Sprint sprint) throws IOException, EngineException {
        for (Event event : sprint.getEvents()) {
            Logger.d("Event: " + event.payload());

            JsonObject asObj = new JsonParser().parse(event.payload()).getAsJsonObject();

            if (event.type().equals("msg_created")) {
                JsonObject msg = asObj.get("msg").getAsJsonObject();
                addMessage(msg.get("text").getAsString(), false);
            }
        }

        if (!session.isWaiting()) {
            addLogMessage(R.string.log_flow_complete);

            ViewCache cache = getViewCache();
            cache.hide(R.id.chat_box, true);
            cache.hide(R.id.container_request_media);
            cache.show(R.id.completed_session_actions);
        } else {
            waitForInput(session.getWait().hint());
        }

        submission.saveSession(session);
        submission.saveNewModifiers(sprint.getModifiers());
        submission.saveNewEvents(sprint.getEvents());

        Logger.d("Persisted new events and modifiers after engine sprint");
    }

    private void waitForInput(Hint hint) {
        ViewCache vc = getViewCache();
        TextView mediaButton = vc.getTextView(R.id.media_icon);
        TextView mediaText = vc.getTextView(R.id.media_text);

        String mediaType = hint != null ? hint.type() : "";
        switch (mediaType) {
            case "image":
                mediaButton.setText(getString(R.string.icon_photo_camera));
                mediaButton.setTag(REQUEST_IMAGE);
                mediaText.setText(getString(R.string.request_image));
                vc.hide(R.id.chat_box, true);
                vc.show(R.id.container_request_media);
                break;
            case "video":
                mediaButton.setText(getString(R.string.icon_videocam));
                mediaButton.setTag(REQUEST_VIDEO);
                mediaText.setText(getString(R.string.request_video));
                vc.hide(R.id.chat_box, true);
                vc.show(R.id.container_request_media);
                break;
            case "audio":
                mediaButton.setText(getString(R.string.icon_mic));
                mediaButton.setTag(REQUEST_AUDIO);
                mediaText.setText(getString(R.string.request_audio));
                vc.hide(R.id.chat_box, true);
                vc.show(R.id.container_request_media);
                break;
            case "location":
                mediaButton.setText(getString(R.string.icon_place));
                mediaButton.setTag(REQUEST_GPS);
                mediaText.setText(getString(R.string.request_gps));
                vc.hide(R.id.chat_box, true);
                vc.show(R.id.container_request_media);
                break;
            default:
                vc.show(R.id.chat_box);
                vc.hide(R.id.container_request_media);
                break;
        }
    }

    private void addLogMessage(int message) {
        getLayoutInflater().inflate(R.layout.item_log_message, chatHistory);
        TextView view = (TextView) chatHistory.getChildAt(chatHistory.getChildCount() - 1);
        view.setText(getString(message));
    }

    private void addMessage(String text, boolean inbound) {
        getLayoutInflater().inflate(R.layout.item_chat_bubble, chatHistory);
        ChatBubbleView bubble = (ChatBubbleView) chatHistory.getChildAt(chatHistory.getChildCount() - 1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bubble.setTransitionName(getString(R.string.transition_chat));
        }

        bubble.setMessage(text, inbound);
        scrollToBottom();
    }

    private void addMedia(Bitmap image, String url, int type) {
        getLayoutInflater().inflate(R.layout.item_chat_bubble, chatHistory);
        ChatBubbleView bubble = (ChatBubbleView) chatHistory.getChildAt(chatHistory.getChildCount() - 1);
        bubble.setThumbnail(image, url, type);
        scrollToBottom();
    }

    private void addMediaLink(String title, String url, int type) {
        getLayoutInflater().inflate(R.layout.item_icon_link, chatHistory);
        IconLinkView icon = (IconLinkView) chatHistory.getChildAt(chatHistory.getChildCount() - 1);
        icon.initialize(title, type, url);
        scrollToBottom();
    }

    private void scrollToBottom() {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    /**
     * User pressed the save button
     *
     * @param view the button
     */
    public void onActionSave(View view) {
        try {
            submission.complete();

            finish();
        } catch (IOException e) {
            Logger.e("unable to complete submission", e);
        }
    }

    /**
     * User pressed the discard button - prompt user to confirm if they want to lose this submission
     *
     * @param view the button
     */
    public void onActionDiscard(View view) {
        confirmDiscardRun();
    }

    /**
     * User pressed the cancel menu item - prompt user to confirm if they want to lose this submission
     *
     * @param item the menu item
     */
    public void onActionCancel(MenuItem item) {
        confirmDiscardRun();
    }

    private void confirmDiscardRun() {
        showConfirmDialog(R.string.confirm_submission_discard, new ConfirmationListener() {
            @Override
            public void onConfirm() {
                submission.delete();
                finish();
            }
        });
    }

    public void onClickMedia(View view) {

        String url = (String) view.getTag(R.string.tag_url);
        int mediaType = (int) view.getTag(R.string.tag_media_type);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        switch (mediaType) {
            case R.string.media_image:
                intent.setDataAndType(Uri.parse(url), "image/*");
                break;
            case R.string.media_video:
                intent.setDataAndType(Uri.parse(url), "video/*");
                break;
            case R.string.media_audio:
                intent.setDataAndType(Uri.parse(url), "audio/*");
                break;
            case R.string.media_location:
                intent.setDataAndType(Uri.parse(url), null);
                break;
        }

        startActivity(intent);
    }
}
