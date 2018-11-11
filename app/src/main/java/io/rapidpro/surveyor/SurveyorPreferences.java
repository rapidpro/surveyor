package io.rapidpro.surveyor;

public interface SurveyorPreferences {
    /**
     * Host we are connected to
     */
    String HOST = "host";

    /**
     * Username/email we are logged in as. If this is set, we are logged in
     */
    String AUTH_USERNAME = "auth_username";

    /**
     * Username/email we were previously logged in as - used to prepopulate login form
     */
    String PREV_USERNAME = "prev_username";

    /**
     * UUIDs of the orgs this user has access to
     */
    String AUTH_ORGS = "auth_orgs";
}
