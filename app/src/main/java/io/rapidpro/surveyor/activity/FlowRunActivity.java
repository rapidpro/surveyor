package io.rapidpro.surveyor.activity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import io.rapidpro.expressions.dates.DateStyle;
import io.rapidpro.flows.RunnerBuilder;
import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.definition.actions.Action;
import io.rapidpro.flows.definition.actions.message.MessageAction;
import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.runner.ContactUrn;
import io.rapidpro.flows.runner.FlowRunException;
import io.rapidpro.flows.runner.Input;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Runner;
import io.rapidpro.flows.runner.Step;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.widget.ChatBubbleView;
import io.rapidpro.flows.runner.Org;

import org.threeten.bp.ZoneId;

import java.util.List;

/**
 * Starts and runs a given flow
 */
public class FlowRunActivity extends BaseActivity {

    private LinearLayout m_chats;
    private Runner m_runner;
    private RunState m_run;
    private EditText m_chatbox;
    private ScrollView m_scrollView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flowrun);

        DBFlow dbFlow = getFlow();

        m_chats = (LinearLayout) findViewById(R.id.chats);
        m_chatbox = (EditText) findViewById(R.id.text_chat);
        m_scrollView = (ScrollView) findViewById(R.id.scroll);

        ((TextView)findViewById(R.id.text_flow_name)).setText(dbFlow.getName());

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


        // initialize our runner
        Org org = new Org("RW", "eng", ZoneId.of("Africa/Kigali"), DateStyle.DAY_FIRST, false);
        Contact contact = new Contact("uuid", "Eric Newcomer", ContactUrn.fromString("tel:+250788382382"), "eng");
        Flow flow = Flow.fromJson(dbFlow.getDefinition());

        // initialize our runner and start the flow
        m_runner = new RunnerBuilder().build();


        try {
            m_run = m_runner.start(org, contact, flow);
        } catch (Throwable t) {
            addMessage(t.getMessage().toString(), true);
            getSurveyor().LOG.e("Error running flow", t);
        }

        // show any initial messages
        addMessages(m_run);

    }

    public void sendMessage(View sendButton) {

        EditText chatBox = (EditText) findViewById(R.id.text_chat);
        String message = chatBox.getText().toString();

        if (message.trim().length() > 0) {
            chatBox.setText("");

            addMessage(message, false);

            try {
                m_runner.resume(m_run, Input.of(message));
                addMessages(m_run);
            } catch (Throwable t) {
                addMessage(t.getMessage().toString(), true);
                getSurveyor().LOG.e("Error running flow", t);
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
    }

    private void addMessages(RunState run) {

        for (Step step : run.getSteps()) {
            for (Action action : step.getActions()) {

                if (action instanceof MessageAction) {
                    getSurveyor().LOG.d("Message: " + ((MessageAction)action).getMsg().getLocalized(run));
                    addMessage(((MessageAction) action).getMsg().getLocalized(run), true);
                } else {
                    getSurveyor().LOG.d("Action: " + action.toString());
                }
            }
        }
    }

    public void addMessage(String message, boolean inbound) {
        getLayoutInflater().inflate(R.layout.item_chat_bubble, m_chats);
        ChatBubbleView bubble = (ChatBubbleView) m_chats.getChildAt(m_chats.getChildCount() - 1);

        //ChatBubbleView bubble = new ChatBubbleView(this);
        bubble.setMessage(message, inbound);
        // m_chats.addView(bubble);
    }

}
