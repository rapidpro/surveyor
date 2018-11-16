package io.rapidpro.surveyor;

public class SurveyorException extends Exception {
    public SurveyorException(String message) {
        super(message);
    }

    public SurveyorException(String message, Exception e) {
        super(message, e);
    }
}
