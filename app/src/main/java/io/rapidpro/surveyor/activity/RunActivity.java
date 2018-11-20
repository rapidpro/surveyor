package io.rapidpro.surveyor.activity;

import android.os.Build;
import android.os.Bundle;
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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nyaruka.goflow.mobile.Environment;
import com.nyaruka.goflow.mobile.Event;
import com.nyaruka.goflow.mobile.MsgIn;
import com.nyaruka.goflow.mobile.Resume;
import com.nyaruka.goflow.mobile.SessionAssets;
import com.nyaruka.goflow.mobile.Trigger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.engine.Engine;
import io.rapidpro.surveyor.engine.EngineException;
import io.rapidpro.surveyor.engine.Session;
import io.rapidpro.surveyor.ui.IconTextView;
import io.rapidpro.surveyor.ui.ViewCache;
import io.rapidpro.surveyor.widget.ChatBubbleView;

public class RunActivity extends BaseActivity /*implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener*/ {

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

    private Org org;
    private Session session;

    //private Submission m_submission;
    private File m_lastMediaFile;
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

        try {
            org = getSurveyor().getOrgService().get(orgUUID);
            SessionAssets assets = Engine.createSessionAssets(Engine.loadAssets(org.getAssets()));
            Environment environment = Engine.createEnvironment(org);

            Flow flow = org.getFlow(flowUUID);
            setTitle(flow.getName());

            Trigger trigger = Engine.createManualTrigger(environment, Engine.createEmptyContact(), flow.toReference());

            session = new Session(assets);

            List<Event> events = session.start(trigger);

            // show any initial messages
            renderEvents(events);

            if (!session.isWaiting()) {
                sessionEnded();
            }

        } catch (EngineException|IOException e) {
            e.printStackTrace();
            showBugReportDialog();
            finish();
        }

        /*if (m_googleApi == null) {
            m_googleApi = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            m_googleApi.connect();
        }*/
    }

    private void initUI() {
        m_chats = findViewById(R.id.chats);
        m_chatbox = findViewById(R.id.text_chat);
        m_sendButton = findViewById(R.id.button_send);
        m_scrollView = findViewById(R.id.scroll);

        // allow messages to be sent with the enter key
        m_chatbox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && actionId == EditorInfo.IME_ACTION_SEND && event.getAction() == KeyEvent.ACTION_DOWN) {
                    onActionSend(m_sendButton);
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
            public void afterTextChanged(Editable s) {}
        });

        m_chatbox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    onActionSend(m_sendButton);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        // TODO confirmDiscardRun();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_run, menu);
        //menu.findItem(R.id.action_cancel).setVisible(m_submission != null && !m_submission.isCompleted());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_cancel) {
            // TODO confirmDiscardRun();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        // TODO stopLocationUpdates();
    }

    public void onActionSend(View sendButton) {
        if (!session.getStatus().equals("waiting")) {
            return;
        }

        EditText chatBox = findViewById(R.id.text_chat);
        String message = chatBox.getText().toString();

        if (message.trim().length() > 0) {
            chatBox.setText("");

            try {
                MsgIn msg = Engine.createMsgIn(UUID.randomUUID().toString(), message, null);
                Resume resume = Engine.createMsgResume(null, null, msg);
                List<Event> events = session.resume(resume);

                renderEvents(events);

                if (!session.isWaiting()) {
                    sessionEnded();
                }

            } catch (Throwable t) {
                Toast.makeText(this, "Couldn't handle message", Toast.LENGTH_SHORT).show();
                SurveyorApplication.LOG.e("Error running flow", t);
                showBugReportDialog();
                finish();
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

    private void renderEvents(List<Event> events) {
        for (Event event : events) {
            SurveyorApplication.LOG.d("Event: " + event.payload());
            JsonObject asObj = new JsonParser().parse(event.payload()).getAsJsonObject();

            if (event.type().equals("msg_created")) {
                JsonObject msg = asObj.get("msg").getAsJsonObject();
                addMessage(msg.get("text").getAsString(), false);
            }
            else if (event.type().equals("msg_received")) {
                JsonObject msg = asObj.get("msg").getAsJsonObject();
                addMessage(msg.get("text").getAsString(), true);
            }
        }

        /*
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
            } else {
                vc.show(R.id.chat_box);
                vc.hide(R.id.container_request_media);
            }

            if (run.getState() == RunState.State.COMPLETED) {
                markFlowComplete();
            }
        }*/
    }

    private void sessionEnded() {
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

    private void scrollToBottom() {
        m_scrollView.post(new Runnable() {
            @Override
            public void run() {
                m_scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}
