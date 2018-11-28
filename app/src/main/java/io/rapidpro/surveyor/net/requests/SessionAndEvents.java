package io.rapidpro.surveyor.net.requests;

import java.util.List;

import io.rapidpro.surveyor.utils.RawJson;

public class SessionAndEvents {
    private RawJson session;
    private List<RawJson> events;

    public SessionAndEvents(RawJson session, List<RawJson> events) {
        this.session = session;
        this.events = events;
    }

    public RawJson getSession() {
        return session;
    }

    public List<RawJson> getEvents() {
        return events;
    }
}
