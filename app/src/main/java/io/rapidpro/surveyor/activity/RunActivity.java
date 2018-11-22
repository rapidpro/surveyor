package io.rapidpro.surveyor.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.greysonparrelli.permiso.Permiso;
import com.nyaruka.goflow.mobile.Environment;
import com.nyaruka.goflow.mobile.Event;
import com.nyaruka.goflow.mobile.MsgIn;
import com.nyaruka.goflow.mobile.Resume;
import com.nyaruka.goflow.mobile.SessionAssets;
import com.nyaruka.goflow.mobile.Trigger;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.engine.Engine;
import io.rapidpro.surveyor.engine.EngineException;
import io.rapidpro.surveyor.engine.Session;
import io.rapidpro.surveyor.ui.IconTextView;
import io.rapidpro.surveyor.ui.ViewCache;
import io.rapidpro.surveyor.utils.ImageUtils;
import io.rapidpro.surveyor.widget.ChatBubbleView;
import io.rapidpro.surveyor.widget.IconLinkView;

public class RunActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // the different types of requests for media
    public static final String REQUEST_IMAGE = "image";
    public static final String REQUEST_AUDIO = "audio";
    public static final String REQUEST_VIDEO = "video";
    public static final String REQUEST_GPS = "geo";

    // custom request codes passed to media capture activities
    private static final int RESULT_IMAGE = 1;
    private static final int RESULT_VIDEO = 2;
    private static final int RESULT_AUDIO = 3;

    private LinearLayout chatHistory;
    private IconTextView sendButtom;
    private EditText chatCompose;
    private ScrollView scrollView;

    private Org org;
    private Session session;
    private Submission submission;

    private GoogleApiClient m_googleApi;
    private android.location.Location m_lastLocation;
    private boolean m_connected;
    private LocationRequest m_locationRequest;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String orgUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_ORG_UUID);
        String flowUUID = getIntent().getStringExtra(SurveyorIntent.EXTRA_FLOW_UUID);

        setContentView(R.layout.activity_run);
        initUI();

        if (m_googleApi == null) {
            m_googleApi = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            m_googleApi.connect();
        }

        try {
            org = getSurveyor().getOrgService().get(orgUUID);
            SessionAssets assets = Engine.createSessionAssets(Engine.loadAssets(org.getAssets()));
            Environment environment = Engine.createEnvironment(org);

            Flow flow = org.getFlow(flowUUID);
            setTitle(flow.getName());

            Trigger trigger = Engine.createManualTrigger(environment, Engine.createEmptyContact(), flow.toReference());

            session = new Session(assets);
            submission = getSurveyor().getSubmissionService().newSubmission(org, flow);

            List<Event> events = session.start(trigger);
            handleEngineOutput(events);

        } catch (EngineException | IOException e) {
            handleProblem("Unable to start flow", e);
        }
    }

    private void initUI() {
        chatHistory = findViewById(R.id.chat_history);
        chatCompose = findViewById(R.id.chat_compose);
        sendButtom = findViewById(R.id.button_send);
        scrollView = findViewById(R.id.scroll);

        // allow messages to be sent with the enter key
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
    }

    @Override
    public void onBackPressed() {
        confirmDiscardRun();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_run, menu);
        // TODO
        //menu.findItem(R.id.action_cancel).setVisible(m_submission != null && !m_submission.isCompleted());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_cancel) {
            confirmDiscardRun();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();

        stopLocationUpdates();
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
                requestVideo();
            } else if (REQUEST_AUDIO.equals(media.getTag())) {
                requestAudio();
            } else if (REQUEST_GPS.equals(media.getTag())) {
                requestLocation();
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

                    if (intent.resolveActivity(getPackageManager()) == null) {
                        handleProblem("Can't find camera device", null);
                        return;
                    }
                    File cameraOutput = getCameraOutput();
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, getSurveyor().getUriForFile(cameraOutput));
                    startActivityForResult(intent, RESULT_IMAGE);
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                RunActivity.this.showRationaleDialog(R.string.permission_camera, callback);
            }

        }, Manifest.permission.CAMERA);
    }

    private void requestVideo() {
        Intent intent = new Intent(this, VideoCaptureActivity.class);

        // TODO
        //intent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, m_submission.createMediaFile("mp4").getAbsolutePath());
        startActivityForResult(intent, RESULT_VIDEO);
    }

    private void requestAudio() {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            @SuppressWarnings("ResourceType")
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    Intent intent = new Intent(RunActivity.this, AudioCaptureActivity.class);

                    // TODO
                    //intent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, m_submission.createMediaFile("m4a").getAbsolutePath());
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
     * Get the current location
     */
    private void requestLocation() {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            @SuppressWarnings("ResourceType")
            public void onPermissionResult(Permiso.ResultSet resultSet) {

                if (resultSet.areAllPermissionsGranted()) {

                    if (m_connected) {
                        m_lastLocation = LocationServices.FusedLocationApi.getLastLocation(m_googleApi);
                        startLocationUpdates();
                        if (m_lastLocation != null) {
                            try {

                                double latitude = m_lastLocation.getLatitude();
                                double longitude = m_lastLocation.getLongitude();

                                String location = latitude + "," + longitude;

                                // TODO
                                //m_runner.resume(m_runState, Input.of("geo", location));

                                String url = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(Location)";
                                addMediaLink(latitude + "," + longitude, url, R.string.media_location);
                                //addMessages(m_runState);
                                //saveSteps();

                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            Toast.makeText(RunActivity.this, R.string.location_unavailable, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(RunActivity.this, R.string.location_unavailable, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                RunActivity.this.showRationaleDialog(R.string.permission_location, callback);
            }
        }, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    protected LocationRequest getLocationRequest() {
        if (m_locationRequest == null) {
            m_locationRequest = new LocationRequest();
            m_locationRequest.setInterval(10000);
            m_locationRequest.setFastestInterval(5000);
            m_locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
        return m_locationRequest;
    }

    /**
     * Start updating location until they exit this run
     */
    @SuppressWarnings("ResourceType")
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(m_googleApi, getLocationRequest(), new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
                m_lastLocation = location;
            }
        });
    }

    /**
     * Stop getting location updates
     */
    protected void stopLocationUpdates() {
        if (m_googleApi.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(m_googleApi, new LocationListener() {
                @Override
                public void onLocationChanged(android.location.Location location) {

                }
            });
        }
    }

    private File getCameraOutput() {
        return new File(getSurveyor().getStorageDirectory(), "camera.jpg");
    }

    /**
     * @see android.app.Activity#onActivityResult(int, int, Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == RESULT_IMAGE) {
            File cameraOutput = getCameraOutput();
            if (cameraOutput.exists()) {

                Bitmap full = BitmapFactory.decodeFile(cameraOutput.getAbsolutePath());
                Bitmap scaled = ImageUtils.scaleToMax(full, 1024);
                Bitmap thumb = ImageUtils.scaleToMax(scaled, 600);

                byte[] asJpg = ImageUtils.convertToJPEG(scaled);

                try {
                    Uri uri = submission.saveMedia(asJpg, "jpg");

                    addMedia(thumb, uri.toString(), R.string.media_image);

                    SurveyorApplication.LOG.d("Saved image capture to " + uri);

                    MsgIn msg = Engine.createMsgIn("", "image/jpeg:" + uri);
                    resumeSession(msg);
                } catch (Exception e) {
                    handleProblem("Couldn't handle message", e);
                }
            }
        }

        if (requestCode == RESULT_VIDEO) {

            String file = data.getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE);
            if (file != null) {

                Bitmap thumb = ThumbnailUtils.createVideoThumbnail(file, MediaStore.Images.Thumbnails.MINI_KIND);

                try {
                    String url = "file:" + file;

                    // TODO
                    //m_runner.resume(m_runState, Input.of("video/mp4", url));
                    addMedia(thumb, url, R.string.media_video);
                    //addMessages(m_runState);
                    //saveSteps();
                } catch (Exception e) {
                    handleProblem("Couldn't handle message", e);
                }
            }
        }

        if (requestCode == RESULT_AUDIO) {
            String file = data.getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE);
            if (file != null) {
                try {
                    String url = "file:" + file;
                    // TODO
                    //m_runner.resume(m_runState, Input.of("audio/mp4", url));
                    addMediaLink(getString(R.string.made_recording), url, R.string.media_audio);
                    //addMessages(m_runState);
                    //saveSteps();

                } catch (Exception e) {
                    handleProblem("Couldn't handle message", e);
                }
            }
        }
    }

    /**
     * Something has gone wrong... show the user the big report dialog
     */
    private void handleProblem(String toastMsg, Throwable e) {
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();

        if (e != null) {
            SurveyorApplication.LOG.e("Error running flow", e);
            showBugReportDialog();
        }

        finish();
    }

    private void resumeSession(MsgIn msg) {
        try {
            Resume resume = Engine.createMsgResume(null, null, msg);
            List<Event> events = session.resume(resume);

            handleEngineOutput(events);

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
     * @param events the new events
     */
    private void handleEngineOutput(List<Event> events) throws IOException, EngineException {
        for (Event event : events) {
            SurveyorApplication.LOG.d("Event: " + event.getPayload());
            JsonObject asObj = new JsonParser().parse(event.getPayload()).getAsJsonObject();

            if (event.getType().equals("msg_created")) {
                JsonObject msg = asObj.get("msg").getAsJsonObject();
                addMessage(msg.get("text").getAsString(), false);
            }
        }

        if (!session.isWaiting()) {
            addLogMessage(R.string.log_flow_complete);

            ViewCache cache = getViewCache();
            cache.hide(R.id.chat_box, true);
            cache.show(R.id.completion_buttons);
        } else {
            waitForInput(session.getWait().getMediaHint());
        }

        submission.saveSession(session);
        submission.saveNewEvents(events);
    }

    private void waitForInput(String mediaType) {
        ViewCache vc = getViewCache();
        TextView mediaButton = vc.getTextView(R.id.media_icon);
        TextView mediaText = vc.getTextView(R.id.media_text);

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
            case "gps":
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
     * User pressed the save button - session is already saved so all we have to do is finish the activity
     *
     * @param view the save button
     */
    public void onActionSave(View view) {
        finish();
    }

    public void onActionDiscard(View view) {
        confirmDiscardRun();
    }

    private void confirmDiscardRun() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_run_removal))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        submission.delete();
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
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

    @Override
    public void onConnected(Bundle bundle) {
        SurveyorApplication.LOG.d("GoogleAPI client connected");
        m_connected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        SurveyorApplication.LOG.d("GoogleAPI client suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        SurveyorApplication.LOG.d("GoogleAPI client failed");
    }
}
