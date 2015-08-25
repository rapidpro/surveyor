package io.rapidpro.surveyor.activity;

import android.os.Bundle;
import android.widget.LinearLayout;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.widget.ChatBubbleView;

/**
 * Starts and runs a given flow
 */
public class FlowRunActivity extends BaseActivity {

    LinearLayout m_chats;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flowrun);

        m_chats = (LinearLayout) findViewById(R.id.chats);
        addMessage("Hi there, what is your name", true);
        addMessage("Eric", false);
        addMessage("What is your favorite color?", true);
        addMessage("banana", false);
        addMessage("I'm sorry, banana is not a color I know. What is your favorite color?", true);
    }

    public void addMessage(String message, boolean inbound) {
        getLayoutInflater().inflate(R.layout.item_chat_bubble, m_chats);
        ChatBubbleView bubble = new ChatBubbleView(this);
        bubble.setMessage(message, inbound);
        m_chats.addView(bubble);
    }

}
