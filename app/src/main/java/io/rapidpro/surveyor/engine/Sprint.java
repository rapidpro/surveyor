package io.rapidpro.surveyor.engine;

import com.nyaruka.goflow.mobile.Event;
import com.nyaruka.goflow.mobile.EventSlice;
import com.nyaruka.goflow.mobile.Modifier;
import com.nyaruka.goflow.mobile.ModifierSlice;
import com.nyaruka.goflow.mobile.MsgWaitEvent;

import java.util.ArrayList;
import java.util.List;

public class Sprint {
    private List<Modifier> modifiers;

    private List<Event> events;

    private MsgWaitEvent msgWaitEvent;

    private Sprint(List<Modifier> modifiers, List<Event> events, MsgWaitEvent msgWaitEvent) {
        this.modifiers = modifiers;
        this.events = events;
        this.msgWaitEvent = msgWaitEvent;
    }

    static Sprint fromNative(com.nyaruka.goflow.mobile.Sprint s) {
        return new Sprint(sliceToList(s.modifiers()), sliceToList(s.events()), s.msgWaitEvent());
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    public List<Event> getEvents() {
        return events;
    }

    public MsgWaitEvent getMsgWaitEvent() {
        return msgWaitEvent;
    }

    private static List<Event> sliceToList(EventSlice slice) {
        List<Event> list = new ArrayList<>((int) slice.length());
        for (int e = 0; e < slice.length(); e++) {
            list.add(slice.get(e));
        }
        return list;
    }

    private static List<Modifier> sliceToList(ModifierSlice slice) {
        List<Modifier> list = new ArrayList<>((int) slice.length());
        for (int e = 0; e < slice.length(); e++) {
            list.add(slice.get(e));
        }
        return list;
    }
}
