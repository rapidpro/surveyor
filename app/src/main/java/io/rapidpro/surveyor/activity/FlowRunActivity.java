package io.rapidpro.surveyor.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.JsonObject;
import com.greysonparrelli.permiso.Permiso;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.rapidpro.flows.RunnerBuilder;
import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.definition.actions.Action;
import io.rapidpro.flows.definition.actions.message.MessageAction;
import io.rapidpro.flows.runner.ContactUrn;
import io.rapidpro.flows.runner.FlowRunException;
import io.rapidpro.flows.runner.Input;
import io.rapidpro.flows.runner.Location;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Runner;
import io.rapidpro.flows.runner.Step;
import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.RunnerUtil;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.TembaException;
import io.rapidpro.surveyor.data.DBAlias;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBLocation;
import io.rapidpro.surveyor.data.OrgDetails;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.net.Definitions;
import io.rapidpro.surveyor.net.FlowDefinition;
import io.rapidpro.surveyor.ui.IconTextView;
import io.rapidpro.surveyor.ui.ViewCache;
import io.rapidpro.surveyor.widget.ChatBubbleView;
import io.rapidpro.surveyor.widget.IconLinkView;
import io.realm.Realm;

/**
 * Starts and runs a given flow
 */
public class FlowRunActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String MSG_TEXT = "text";
    public static final String MSG_PHOTO = "photo";
    public static final String MSG_AUDIO = "audio";
    public static final String MSG_VIDEO = "video";
    public static final String MSG_GPS = "geo";
    public static final List<String> MSG_RESOLVED = Arrays.asList(MSG_TEXT, MSG_GPS);

    private static final int RESULT_IMAGE = 1;
    private static final int RESULT_VIDEO = 2;
    private static final int RESULT_AUDIO = 3;

    private LinearLayout m_chats;
    private IconTextView m_sendButton;
    private EditText m_chatbox;
    private ScrollView m_scrollView;

    private Runner m_runner;
    private RunState m_runState;
    private Submission m_submission;
    private File m_lastMediaFile;
    private GoogleApiClient m_googleApi;

    private android.location.Location m_lastLocation;
    private boolean m_connected;
    private LocationRequest m_locationRequest;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (m_googleApi == null) {
            m_googleApi = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            m_googleApi.connect();
        }


        setContentView(R.layout.activity_flowrun);

        DBFlow flow = getDBFlow();

        final Realm realm = getRealm();

        try {

            m_chats = (LinearLayout) findViewById(R.id.chats);
            m_chatbox = (EditText) findViewById(R.id.text_chat);
            m_sendButton = (IconTextView) findViewById(R.id.button_send);
            m_scrollView = (ScrollView) findViewById(R.id.scroll);

            setTitle(flow.getName());

            // allow messages to be sent with the enter key
            m_chatbox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (event != null && actionId == EditorInfo.IME_ACTION_SEND && event.getAction() == KeyEvent.ACTION_DOWN) {
                        sendMessage(null);
                        return true;
                    }
                    return false;
                }
            });

            m_chatbox.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() > 0) {
                        m_sendButton.setIconColor(R.color.tertiary_light);
                    } else {
                        m_sendButton.setIconColor(R.color.light_gray);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            m_chatbox.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                        sendMessage(null);
                        return true;
                    }
                    return false;
                }
            });


            // read our individual flow definitions
            Definitions definitions = JsonUtils.getGson().fromJson(getDBFlow().getDefinition(), Definitions.class);

            StringBuilder sb = new StringBuilder();
            List<Flow> flows = new ArrayList<>();
            for (FlowDefinition def : definitions.flows) {
                sb.append(def.metadata.revision).append('-');
                flows.add(Flow.fromJson(def.toString()));
            }

            String revision = "0";
            if (sb.length() > 0) {
                revision = sb.substring(0, sb.length() - 1);
            }

            m_runner = new RunnerBuilder(flows).withLocationResolver(new Location.Resolver() {

                @Override
                public Location resolve(String input, String country, Location.Level levelEnum, Location parent) {

                    int level = 1;
                    if (levelEnum == Location.Level.DISTRICT) {
                        level = 2;
                    }

                    DBLocation location = realm.where(DBLocation.class).equalTo("org.id", getDBOrg().getId()).equalTo("name", input, false).equalTo("level", level).findFirst();

                    if (location == null) {
                        DBAlias alias = realm.where(DBAlias.class).equalTo("name", input, false).equalTo("location.level", level).findFirst();
                        if (alias != null) {
                            location = alias.getLocation();
                        }
                    }

                    if (location != null) {
                        return new Location(location.getBoundary(), location.getName(), levelEnum);
                    }

                    return null;

                }
            }).build();


            m_submission = new Submission(getUsername(), getDBFlow(), revision);

            // create a run state based on our contact
            OrgDetails details = OrgDetails.load(flow.getOrg());
            m_runState = RunnerUtil.getRunState(m_runner, getDBFlow(), details.getFields());

            // if our contact creation is per login, add their urn
            JsonObject metadata = m_runState.getActiveFlow().getMetadata();
            if (metadata.has("contact_creation")) {
                String contactCreation = metadata.get("contact_creation").toString();
                if (contactCreation != null) {
                    if ("login".equals(contactCreation)) {
                        m_runState.getContact().getUrns().add(ContactUrn.fromString("mailto:" + getUsername()));
                    }
                }
            }

            // show any initial messages
            addMessages(m_runState);

            saveSteps();

        } catch (Throwable t) {
            Surveyor.LOG.e("Error running flow", t);
            Toast.makeText(this, "Sorry, this flow is not supported.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        confirmDiscardRun();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_run, menu);
        menu.findItem(R.id.action_cancel).setVisible(!m_submission.isCompleted());
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

    public void onRequestMedia(View view) {
        View media = getViewCache().getView(R.id.media_icon);
        if (m_runState.getState() != RunState.State.COMPLETED) {
            if (media != null && MSG_PHOTO.equals(media.getTag())) {
                requestPhoto();
            } else if (media != null && MSG_VIDEO.equals(media.getTag())) {
                requestVideo();
            } else if (media != null && MSG_AUDIO.equals(media.getTag())) {
                requestAudio();
            } else if (media != null && MSG_GPS.equals(media.getTag())) {
                requestLocation();
            }
        }
    }

    public void sendMessage(View sendButton) {

        if (m_runState.getState() == RunState.State.COMPLETED) {
            return;
        }

        EditText chatBox = (EditText) findViewById(R.id.text_chat);
        String message = chatBox.getText().toString();

        if (message.trim().length() > 0) {
            chatBox.setText("");

            try {
                m_runner.resume(m_runState, Input.of(message));
                saveSteps();

                addMessage(message, false);

                addMessages(m_runState);

            } catch (Throwable t) {
                Toast.makeText(this, "Couldn't handle message", Toast.LENGTH_SHORT).show();
                Surveyor.LOG.e("Error running flow", t);
                showSendBugReport();
            }

            // scroll us to the bottom
            m_scrollView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    m_scrollView.setSmoothScrollingEnabled(true);
                    m_scrollView.fullScroll(ScrollView.FOCUS_DOWN);

                    // put the focus back on the chat box
                    m_chatbox.requestFocus();
                }
            }, 100);
        }

        // refresh our menu
        invalidateOptionsMenu();
    }

    private void saveSteps() throws FlowRunException {
        // update our persisted state with the newly completed steps
        m_submission.addSteps(m_runState);
        m_submission.save();
    }

    private void addMessages(RunState run) {
        if (run != null) {
            for (Step step : run.getCompletedSteps()) {
                for (Action action : step.getActions()) {
                    if (action instanceof MessageAction) {
                        getSurveyor().LOG.d("Message: " + ((MessageAction) action).getMsg().getLocalized(run));
                        addMessage(((MessageAction) action).getMsg().getLocalized(run), true);
                    } else {
                        getSurveyor().LOG.d("Action: " + action.toString());
                    }
                }
            }

            ViewCache vc = getViewCache();
            TextView mediaButton = vc.getTextView(R.id.media_icon);
            TextView mediaText = vc.getTextView(R.id.media_text);

            if (run.getState() == RunState.State.WAIT_PHOTO) {
                mediaButton.setText(getString(R.string.icon_photo_camera));
                mediaButton.setTag(MSG_PHOTO);
                mediaText.setText(getString(R.string.request_photo));
                vc.hide(R.id.chat_box, true);
                vc.show(R.id.container_request_media);
            } else if (run.getState() == RunState.State.WAIT_VIDEO) {
                mediaButton.setText(getString(R.string.icon_videocam));
                mediaButton.setTag(MSG_VIDEO);
                mediaText.setText(getString(R.string.request_video));
                vc.hide(R.id.chat_box, true);
                vc.show(R.id.container_request_media);
            } else if (run.getState() == RunState.State.WAIT_AUDIO) {
                mediaButton.setText(getString(R.string.icon_mic));
                mediaButton.setTag(MSG_AUDIO);
                mediaText.setText(getString(R.string.request_audio));
                vc.hide(R.id.chat_box, true);
                vc.show(R.id.container_request_media);
            } else if (run.getState() == RunState.State.WAIT_GPS) {
                mediaButton.setText(getString(R.string.icon_place));
                mediaButton.setTag(MSG_GPS);
                mediaText.setText(getString(R.string.request_location));
                vc.hide(R.id.chat_box, true);
                vc.show(R.id.container_request_media);
            }
            else {
                vc.show(R.id.chat_box);
                vc.hide(R.id.container_request_media);
            }

            if (run.getState() == RunState.State.COMPLETED) {
                markFlowComplete();
            }
        }
    }


    private void requestMedia(Intent intent, int resultType) {
        m_lastMediaFile = m_submission.createMediaFile("jpg");

        // Continue only if the File was successfully created
        if (m_lastMediaFile != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(m_lastMediaFile));
            startActivityForResult(intent, resultType);
        }

    }

    private void requestPhoto() {

        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            @SuppressWarnings("ResourceType")
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        requestMedia(takePictureIntent, RESULT_IMAGE);
                    }
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                FlowRunActivity.this.showRationaleDialog(R.string.permission_camera, callback);
            }

        }, Manifest.permission.CAMERA);
    }

    private void requestVideo() {
        Intent intent = new Intent(this, VideoCaptureActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, m_submission.createMediaFile("mp4").getAbsolutePath());
        startActivityForResult(intent, RESULT_VIDEO);
    }

    private void requestAudio() {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            @SuppressWarnings("ResourceType")
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    Intent intent = new Intent(FlowRunActivity.this, AudioCaptureActivity.class);
                    intent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, m_submission.createMediaFile("m4a").getAbsolutePath());
                    startActivityForResult(intent, RESULT_AUDIO);
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                FlowRunActivity.this.showRationaleDialog(R.string.permission_camera, callback);
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
                                m_runner.resume(m_runState, Input.of("geo", location));

                                String url = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(Location)";
                                addMediaLink(latitude + "," + longitude, url, R.string.media_location);
                                addMessages(m_runState);
                                saveSteps();

                            } catch (FlowRunException e) {
                                throw new TembaException(e);
                            }
                        } else {
                            Toast.makeText(FlowRunActivity.this,
                                    R.string.location_unavailable,
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(FlowRunActivity.this,
                                R.string.location_unavailable,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                FlowRunActivity.this.showRationaleDialog(R.string.permission_location, callback);
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

    protected Bitmap scaleToWidth(Bitmap bitmap, int width) {
        double ratio = (double) width / (double) bitmap.getWidth();
        return Bitmap.createScaledBitmap(bitmap, width, (int) ((double) bitmap.getHeight() * ratio), false);
    }

    /**
     * Scales a bitmap so that it's longest dimension is provided value
     */
    protected Bitmap scaleToMax(Bitmap bitmap, int max) {

        // landscape photos
        if (bitmap.getWidth() > bitmap.getHeight()) {
            double ratio = (double) max / (double) bitmap.getWidth();
            return Bitmap.createScaledBitmap(bitmap, max, (int) ((double) bitmap.getHeight() * ratio), false);

        } else {
            double ratio = (double) max / (double) bitmap.getHeight();
            return Bitmap.createScaledBitmap(bitmap, (int) ((double) bitmap.getWidth() * ratio), max, false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_IMAGE && resultCode == RESULT_OK) {

            if (m_lastMediaFile != null && m_lastMediaFile.exists()) {

                Bitmap full = BitmapFactory.decodeFile(m_lastMediaFile.getAbsolutePath());
                Bitmap scaled = scaleToMax(full, 1024);
                Bitmap thumb = scaleToMax(scaled, 600);

                byte[] bytes = convertToJPEG(scaled);

                try {
                    FileUtils.writeByteArrayToFile(m_lastMediaFile, bytes);
                    String url = "file:" + m_lastMediaFile.getAbsolutePath();
                    m_runner.resume(m_runState, Input.of("image/jpeg", url));
                    addMedia(thumb, url, R.string.media_image);
                    addMessages(m_runState);
                    saveSteps();
                } catch (Exception e) {
                    Toast.makeText(this, "Couldn't handle message", Toast.LENGTH_SHORT).show();
                    Surveyor.LOG.e("Error running flow", e);
                    showSendBugReport();
                }
            }
        }

        if (requestCode == RESULT_VIDEO && resultCode == RESULT_OK) {

            String file = data.getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE);
            if (file != null) {

                Bitmap thumb = ThumbnailUtils.createVideoThumbnail(file, MediaStore.Images.Thumbnails.MINI_KIND);

                try {
                    String url = "file:" + file;
                    m_runner.resume(m_runState, Input.of("video/mp4", url));
                    addMedia(thumb, url, R.string.media_video);
                    addMessages(m_runState);
                    saveSteps();
                } catch (Exception e) {
                    Toast.makeText(this, "Couldn't handle message", Toast.LENGTH_SHORT).show();
                    Surveyor.LOG.e("Error running flow", e);
                    showSendBugReport();
                }
            }
        }

        if (requestCode == RESULT_AUDIO && resultCode == RESULT_OK) {
            String file = data.getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE);
            if (file != null) {
                try {
                    String url = "file:" + file;
                    m_runner.resume(m_runState, Input.of("audio/mp4", url));
                    addMediaLink(getString(R.string.made_recording), url, R.string.media_audio);
                    addMessages(m_runState);
                    saveSteps();

                } catch (Exception e) {
                    Toast.makeText(this, "Couldn't handle message", Toast.LENGTH_SHORT).show();
                    Surveyor.LOG.e("Error running flow", e);
                    showSendBugReport();
                }
            }
        }
    }

    private void createMediaFile() throws IOException {

        m_lastMediaFile = null;

        // Create a unique file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "MEDIA_" + timeStamp + "_";

        // create a hidden temporary directory if we don't have one
        File temp = Environment.getExternalStorageDirectory();
        temp = new File(temp.getAbsolutePath() + "/.temp/");
        if (!temp.exists()) {
            temp.mkdir();
        }

        m_lastMediaFile = File.createTempFile(imageFileName, ".media", temp);
    }

    public static byte[] convertToJPEG(Bitmap bm) {
        // int iBytes = bm.getWidth() * bm.getHeight() * 4;
        // ByteBuffer buffer = ByteBuffer.allocate(iBytes);
        // bm.copyPixelsToBuffer(buffer);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return baos.toByteArray();
    }


    private void markFlowComplete() {

        addLogMessage(R.string.log_flow_complete);
        ViewCache cache = getViewCache();
        cache.hide(R.id.chat_box, true);
        cache.show(R.id.completion_buttons);

    }

    private void addLogMessage(int message) {
        getLayoutInflater().inflate(R.layout.item_log_message, m_chats);
        TextView view = (TextView) m_chats.getChildAt(m_chats.getChildCount() - 1);
        view.setText(getString(message));
    }

    private void addMessage(String message, boolean inbound) {
        getLayoutInflater().inflate(R.layout.item_chat_bubble, m_chats);
        ChatBubbleView bubble = (ChatBubbleView) m_chats.getChildAt(m_chats.getChildCount() - 1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bubble.setTransitionName(getString(R.string.transition_chat));
        }

        bubble.setMessage(message, inbound);
        scrollToBottom();
    }


    private void addMedia(Bitmap image, String url, int type) {
        getLayoutInflater().inflate(R.layout.item_chat_bubble, m_chats);
        ChatBubbleView bubble = (ChatBubbleView) m_chats.getChildAt(m_chats.getChildCount() - 1);
        bubble.setThumbnail(image, url, type);
        scrollToBottom();
    }

    private void addMediaLink(String title, String url, int type) {
        getLayoutInflater().inflate(R.layout.item_icon_link, m_chats);
        IconLinkView icon = (IconLinkView) m_chats.getChildAt(m_chats.getChildCount() - 1);
        icon.initialize(title, type, url);
        scrollToBottom();
    }

    private void scrollToBottom() {
        m_scrollView.post(new Runnable() {
            @Override
            public void run() {
                m_scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void confirmDiscardRun() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_run_removal))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // delete our run file
                        m_submission.delete();
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

    public void saveRunButton(View view) {

        // update the time of our last run
        Realm realm = getRealm();
        realm.beginTransaction();
        DBFlow flow = getDBFlow();
        flow.setLastRunDate(new Date());
        realm.copyToRealmOrUpdate(flow);
        realm.commitTransaction();

        m_submission.complete();

        finish();
    }

    public void discardRunButton(View view) {
        confirmDiscardRun();
    }

    public void onClickMedia(View view) {

        String url = (String) view.getTag(R.string.tag_url);
        int mediaType = (int) view.getTag(R.string.tag_media_type);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);

        if (mediaType == R.string.media_image) {
            intent.setDataAndType(Uri.parse(url), "image/*");
        } else if (mediaType == R.string.media_video) {
            intent.setDataAndType(Uri.parse(url), "video/*");
        } else if (mediaType == R.string.media_audio) {
            intent.setDataAndType(Uri.parse(url), "audio/*");
        } else if (mediaType == R.string.media_location) {
            intent.setDataAndType(Uri.parse(url), null);
        }

        startActivity(intent);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Surveyor.LOG.d("GoogleAPI client connected");
        m_connected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Surveyor.LOG.d("GoogleAPI client suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Surveyor.LOG.d("GoogleAPI client failed");
    }

}
