package io.rapidpro.surveyor.net;

import io.rapidpro.surveyor.SurveyorException;

/**
 * Exceptions that come from Temba API requests
 */
public class TembaException extends SurveyorException {
    public TembaException(String message) {
        super(message);
    }

    public TembaException(String message, Exception e) {
        super(message, e);
    }
}
