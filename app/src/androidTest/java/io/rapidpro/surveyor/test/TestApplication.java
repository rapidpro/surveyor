package io.rapidpro.surveyor.test;

import io.rapidpro.surveyor.SurveyorApplication;

/**
 * Custom application class for tests which overrides some stuff
 */
public class TestApplication extends SurveyorApplication {
    @Override
    protected String getPreferencesName() {
        return "tests";
    }
}
