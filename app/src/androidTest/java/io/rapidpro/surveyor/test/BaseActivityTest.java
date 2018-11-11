package io.rapidpro.surveyor.test;

import android.content.SharedPreferences;

import org.junit.After;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.rapidpro.surveyor.SurveyorApplication;

@RunWith(AndroidJUnit4.class)
public abstract class BaseActivityTest {

    @After
    public void clearPreferences() {
        SharedPreferences.Editor editor = SurveyorApplication.get().getPreferences().edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Sets a string shared preference value
     * @param key the preference key
     * @param value the preference value
     */
    protected void setPreference(final String key, final String value) {
        SharedPreferences.Editor editor = SurveyorApplication.get().getPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }
}
