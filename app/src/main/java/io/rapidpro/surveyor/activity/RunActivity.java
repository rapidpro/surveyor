package io.rapidpro.surveyor.activity;

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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.ui.IconTextView;

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
    private Flow flow;
    //private Runner m_runner;
    //private RunState m_runState;
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

        try {
            org = getSurveyor().getOrgService().get(orgUUID);
            flow = org.getFlow(flowUUID);
        } catch (IOException e) {
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

        setContentView(R.layout.activity_run);
        setTitle(flow.getName());

        m_chats = (LinearLayout) findViewById(R.id.chats);
        m_chatbox = (EditText) findViewById(R.id.text_chat);
        m_sendButton = (IconTextView) findViewById(R.id.button_send);
        m_scrollView = (ScrollView) findViewById(R.id.scroll);

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
            public void afterTextChanged(Editable s) {}
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

    public void sendMessage(View sendButton) {

        //if (m_runState.getState() == RunState.State.COMPLETED) {
        //    return;
        //}

        EditText chatBox = (EditText) findViewById(R.id.text_chat);
        String message = chatBox.getText().toString();

        if (message.trim().length() > 0) {
            chatBox.setText("");

            try {
                //m_runner.resume(m_runState, Input.of(message));
                //saveSteps();

                //addMessage(message, false);

                //addMessages(m_runState);

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
}
