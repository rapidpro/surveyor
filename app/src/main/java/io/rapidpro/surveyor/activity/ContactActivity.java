package io.rapidpro.surveyor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.rapidpro.flows.definition.Flow;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.RunnerUtil;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.Language;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.ui.ViewCache;
import io.realm.Realm;

/**
 * ContactActivity collects information about a contact for
 * whom to start a flow run for.
 */
public class ContactActivity extends BaseActivity {

    // the flow we are trying to start
    private Flow m_flow;

    private ViewCache m_cache;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_new_run);
        m_cache = getViewCache();

        // get all of our language choices
        try {
            DBFlow dbFlow = getDBFlow();
            m_cache.setText(R.id.text_flow_name, dbFlow.getName());

            m_flow = RunnerUtil.createFlow(dbFlow);
            List<Language> languageOptions = new ArrayList<>();

            // add our base language
            languageOptions.add(new Language(m_flow.getBaseLanguage()));

            // add all of our other languages
            for (String lang : m_flow.getLanguages()) {
                // don't add our base language again
                if (!lang.equals(m_flow.getBaseLanguage())) {
                    Language language = new Language(lang);
                    if (language.isRecognized()) {
                        languageOptions.add(new Language(lang));
                    }
                }
            }

            // populate our language dropdown
            Spinner dropdown = (Spinner) findViewById(R.id.language_spinner);

            if (languageOptions.size() > 1) {
                ArrayAdapter<Language> adapter = new ArrayAdapter<>(this, R.layout.item_text, languageOptions);
                adapter.setDropDownViewResource(R.layout.dropdown_text);
                dropdown.setAdapter(adapter);
            } else {
                m_cache.hide(R.id.language_section);
            }

        } catch (Throwable t) {

            // some flow types aren't supported, make sure we don't crash on them
            Surveyor.LOG.d("Sorry, this flow is not supported.");
            finish();
        }
    }

    public void startFlow(View view) {

        Language lang = (Language) m_cache.getSelectedItem(R.id.language_spinner);
        String name = m_cache.getRequiredText(R.id.text_contact_name);
        String phone = m_cache.getRequiredText(R.id.text_phone_number);

        // don't proceed without a name or phone number
        if (name == null || phone == null) {
            return;
        }

        // if we don't have a language, use the base language
        if (lang == null){
            lang = new Language(m_flow.getBaseLanguage());
        }

        // create a new submission
        Submission submission = new Submission(getDBFlow(), name, lang.getCode(), phone);
        submission.save();

        // start our flow run activity for our new contact
        Intent intent = getIntent(this, FlowRunActivity.class);
        intent.putExtra(SurveyorIntent.EXTRA_SUBMISSION_FILE, submission.getFilename());
        startActivity(intent);

        // remove us from the back stack
        finish();
    }
}
