package io.rapidpro.surveyor.engine;

import io.rapidpro.surveyor.SurveyorException;

public class EngineException extends SurveyorException {
    public EngineException(Exception e) {
        super(e.getMessage(), e);
    }
}
