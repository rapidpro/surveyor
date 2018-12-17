package io.rapidpro.surveyor.net.requests;

import java.util.List;

import io.rapidpro.surveyor.utils.RawJson;

public class SubmissionPayload {
    private RawJson session;
    private List<RawJson> modifiers;
    private List<RawJson> events;

    public SubmissionPayload(RawJson session, List<RawJson> modifiers, List<RawJson> events) {
        this.session = session;
        this.modifiers = modifiers;
        this.events = events;
    }

    public RawJson getSession() {
        return session;
    }

    public List<RawJson> getModifiers() {
        return modifiers;
    }

    public List<RawJson> getEvents() {
        return events;
    }
}
