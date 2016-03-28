package io.rapidpro.surveyor.activity;

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

import com.google.gson.JsonObject;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.rapidpro.flows.RunnerBuilder;
import io.rapidpro.flows.definition.actions.Action;
import io.rapidpro.flows.definition.actions.message.MessageAction;
import io.rapidpro.flows.runner.ContactUrn;
import io.rapidpro.flows.runner.FlowRunException;
import io.rapidpro.flows.runner.Input;
import io.rapidpro.flows.runner.Location;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Runner;
import io.rapidpro.flows.runner.Step;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.RunnerUtil;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.DBAlias;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBLocation;
import io.rapidpro.surveyor.data.OrgDetails;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.ui.IconTextView;
import io.rapidpro.surveyor.ui.ViewCache;
import io.rapidpro.surveyor.widget.ChatBubbleView;
import io.realm.Realm;

/**
 * Starts and runs a given flow
 */
public class FlowRunActivity extends BaseActivity {

    private LinearLayout m_chats;
    private IconTextView m_sendButton;
    private EditText m_chatbox;
    private ScrollView m_scrollView;

    private Runner m_runner;
    private RunState m_runState;

    private Submission m_submission;

    private File m_lastMediaFile;

    private static final int ACTION_TEXT = 1;
    private static final int ACTION_PHOTO = 2;
    private static final int ACTION_AUDIO = 3;
    private static final int ACTION_VIDEO = 4;
    private static final int ACTION_GPS = 5;

    private static final int RESULT_IMAGE = 1;
    private static final int RESULT_VIDEO = 2;
    private static final int RESULT_AUDIO = 3;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flowrun);

        DBFlow flow = getDBFlow();

        final Realm realm = getRealm();

        try {

            m_chats = (LinearLayout) findViewById(R.id.chats);
            m_chatbox = (EditText) findViewById(R.id.text_chat);
            m_sendButton = (IconTextView) findViewById(R.id.button_send);
            m_scrollView = (ScrollView) findViewById(R.id.scroll);

            ((TextView)findViewById(R.id.text_flow_name)).setText(flow.getName());

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

            m_runner = new RunnerBuilder().withLocationResolver(new Location.Resolver() {

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


            m_submission = new Submission(getUsername(), getDBFlow());

            // create a run state based on our contact
            OrgDetails details = OrgDetails.load(flow.getOrg());
            m_runState = RunnerUtil.getRunState(m_runner, getDBFlow(), details.getFields());

            // if our contact creation is per login, add their urn
            JsonObject metadata = m_runState.getFlow().getMetadata();
            if (metadata.has("contact_creation")) {
                if ("login".equals(metadata.get("contact_creation").getAsString())) {
                    m_runState.getContact().getUrns().add(ContactUrn.fromString("mailto:" + getUsername()));
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

    public void refreshRun(MenuItem item) {

    }


    public void sendMessage(View sendButton) {

        if (m_runState.getState() == RunState.State.COMPLETED) {
            return;
        }

        if (sendButton != null && sendButton.getTag() == ACTION_PHOTO) {
            requestPhoto();
            return;
        }

        if (sendButton != null && sendButton.getTag() == ACTION_VIDEO) {
            requestVideo();
            return;
        }

        if (sendButton != null && sendButton.getTag() == ACTION_AUDIO) {
            requestAudio();
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
                // addMessage(t.getMessage().toString(), true);
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
            TextView sendButton = vc.getTextView(R.id.button_send);

            if (run.getState() == RunState.State.WAIT_PHOTO) {
                sendButton.setText(getString(R.string.icon_photo_camera));
                sendButton.setTag(ACTION_PHOTO);
                vc.hide(R.id.text_chat, true);
            } else if (run.getState() == RunState.State.WAIT_VIDEO) {
                sendButton.setText(getString(R.string.icon_videocam));
                sendButton.setTag(ACTION_VIDEO);
                vc.hide(R.id.text_chat, true);
            } else if (run.getState() == RunState.State.WAIT_AUDIO) {
                sendButton.setText(getString(R.string.icon_mic));
                sendButton.setTag(ACTION_AUDIO);
                vc.hide(R.id.text_chat, true);
            } else {
                sendButton.setText(getString(R.string.icon_send));
                sendButton.setTag(ACTION_TEXT);
                vc.show(R.id.text_chat);
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
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            requestMedia(takePictureIntent, RESULT_IMAGE);
        }
    }

    private void requestVideo() {
        Intent intent = new Intent(this, VideoCaptureActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, m_submission.createMediaFile("mp4").getAbsolutePath());
        startActivityForResult(intent, RESULT_VIDEO);
    }

    private void requestAudio() {
        Intent intent = new Intent(this, AudioCaptureActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_MEDIA_FILE, m_submission.createMediaFile("m4a").getAbsolutePath());
        startActivityForResult(intent, RESULT_AUDIO);
    }

    protected Bitmap scaleToWidth(Bitmap bitmap, int width) {
        double ratio = (double)width / (double)bitmap.getWidth();
        return Bitmap.createScaledBitmap(bitmap, width, (int)((double)bitmap.getHeight() * ratio), false);
    }

    /**
     * Scales a bitmap so that it's longest dimension is provided value
     */
    protected Bitmap scaleToMax(Bitmap bitmap, int max) {

        // landscape photos
        if (bitmap.getWidth() > bitmap.getHeight()) {
            double ratio = (double)max / (double)bitmap.getWidth();
            return Bitmap.createScaledBitmap(bitmap, max, (int)((double)bitmap.getHeight() * ratio), false);

        } else {
            double ratio = (double)max / (double)bitmap.getHeight();
            return Bitmap.createScaledBitmap(bitmap, (int)((double)bitmap.getWidth() * ratio), max, false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_IMAGE && resultCode == RESULT_OK) {

            if(m_lastMediaFile != null && m_lastMediaFile.exists()){

                Bitmap full = BitmapFactory.decodeFile(m_lastMediaFile.getAbsolutePath());
                Bitmap scaled = scaleToMax(full, 1024);
                Bitmap thumb = scaleToMax(scaled, 600);

                byte[] bytes = convertToJPEG(scaled);

                try {
                    FileUtils.writeByteArrayToFile(m_lastMediaFile, bytes);
                    String url = "file:" + m_lastMediaFile.getAbsolutePath();
                    m_runner.resume(m_runState, Input.of("image", url));
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
            if(file != null) {

                Bitmap thumb = ThumbnailUtils.createVideoThumbnail(file, MediaStore.Images.Thumbnails.MINI_KIND);

                try {
                    String url = "file:" + file;
                    m_runner.resume(m_runState, Input.of("video", url));
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
            Surveyor.LOG.d("AUDIO RESULT");
            String file = data.getStringExtra(SurveyorIntent.EXTRA_MEDIA_FILE);
            if(file != null) {
                try {
                    String url = "file:" + file;
                    m_runner.resume(m_runState, Input.of("audio", url));
                    addMedia(null, url, R.string.media_audio);
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

        // create a hidden temprorary directory if we don't have one
        File temp = Environment.getExternalStorageDirectory();
        temp = new File(temp.getAbsolutePath()+"/.temp/");
        if(!temp.exists()) {
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
    }


    private void addMedia(Bitmap image, String url, int type) {
        getLayoutInflater().inflate(R.layout.item_chat_bubble, m_chats);
        ChatBubbleView bubble = (ChatBubbleView) m_chats.getChildAt(m_chats.getChildCount() - 1);
        bubble.setThumbnail(image, url, type);
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
        }

        startActivity(intent);
    }
}
