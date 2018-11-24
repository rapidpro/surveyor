package io.rapidpro.surveyor.test;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.utils.SurveyUtils;

/**
 * Custom application class for tests which overrides some stuff to avoid clashing will real
 * application use.
 */
public class TestApplication extends SurveyorApplication {

    /**
     * @see SurveyorApplication#getPreferencesName()
     */
    @Override
    public String getPreferencesName() {
        return "tests";
    }

    /**
     * @see SurveyorApplication#getOrgsDirectory()
     */
    @Override
    public File getOrgsDirectory() throws IOException {
        return SurveyUtils.mkdir(getFilesDir(), "test_orgs");
    }

    /**
     * @see SurveyorApplication#getStorageDirectory()
     */
    @Override
    public File getStorageDirectory() throws IOException {
        return SurveyUtils.mkdir(Environment.getExternalStorageDirectory(), "Surveyor_test");
    }
}
