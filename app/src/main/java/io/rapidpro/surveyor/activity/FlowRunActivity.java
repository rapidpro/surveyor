package io.rapidpro.surveyor.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
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

import io.rapidpro.flows.RunnerBuilder;
import io.rapidpro.flows.definition.actions.Action;
import io.rapidpro.flows.definition.actions.message.MessageAction;
import io.rapidpro.flows.runner.FlowRunException;
import io.rapidpro.flows.runner.Input;
import io.rapidpro.flows.runner.Location;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.flows.runner.Runner;
import io.rapidpro.flows.runner.Step;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.RunnerUtil;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.DBAlias;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBLocation;
import io.rapidpro.surveyor.data.OrgDetails;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.ui.ViewCache;
import io.rapidpro.surveyor.widget.ChatBubbleView;
import io.realm.Realm;

/**
 * Starts and runs a given flow
 */
public class FlowRunActivity extends BaseActivity {

    private LinearLayout m_chats;
    private EditText m_chatbox;
    private ScrollView m_scrollView;

    private Runner m_runner;
    private RunState m_runState;

    private Submission m_submission;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flowrun);

        DBFlow flow = getDBFlow();

        final Realm realm = getRealm();

        try {

            m_chats = (LinearLayout) findViewById(R.id.chats);
            m_chatbox = (EditText) findViewById(R.id.text_chat);
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

            m_submission = new Submission(getDBFlow());

            // create a run state based on our contact
            OrgDetails details = OrgDetails.load(flow.getOrg());
            m_runState = RunnerUtil.getRunState(m_runner, getDBFlow(), details.getFields());

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

            if (run.getState() == RunState.State.COMPLETED) {
                markFlowComplete();
            }
        }
    }

    private void markFlowComplete() {

        addLogMessage(R.string.log_flow_complete);
        ViewCache cache = getViewCache();
        cache.hide(R.id.chat_box);
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
        bubble.setMessage(message, inbound);
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
        finish();
    }

    public void discardRunButton(View view) {
        confirmDiscardRun();
    }
}
