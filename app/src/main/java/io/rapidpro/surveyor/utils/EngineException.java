package io.rapidpro.surveyor.utils;

import io.rapidpro.surveyor.SurveyorException;

public class EngineException extends SurveyorException {
    public EngineException(Exception e) {
        super(e.getMessage(), e);
    }
}
