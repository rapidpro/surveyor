package io.rapidpro.surveyor.test;

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
}
