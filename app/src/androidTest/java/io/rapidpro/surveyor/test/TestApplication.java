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
        // TODO try again to get travis to let us write to external storage?
        return SurveyUtils.mkdir(getFilesDir(), "shared");
    }

    /**
     * @see SurveyorApplication#getSubmissionsDirectory()
     */
    @Override
    protected File getSubmissionsDirectory() throws IOException {
        return SurveyUtils.mkdir(getStorageDirectory(), "test_submissions");
    }
}
