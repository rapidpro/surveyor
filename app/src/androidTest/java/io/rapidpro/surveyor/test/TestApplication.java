package io.rapidpro.surveyor.test;

import android.os.Environment;

import java.io.File;

import io.rapidpro.surveyor.SurveyorApplication;

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
    public File getOrgsDirectory() {
        File dir = new File(getFilesDir(), "test_orgs");
        dir.mkdirs();
        return dir;
    }

    /**
     * @see SurveyorApplication#getSubmissionsDirectory()
     */
    public File getSubmissionsDirectory() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Surveyor.test");
        dir.mkdirs();
        return dir;
    }
}
