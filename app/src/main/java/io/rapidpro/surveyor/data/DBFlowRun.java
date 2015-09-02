package io.rapidpro.surveyor.data;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Holds a run for a contact against a specific flow
 */
public class DBFlowRun extends RealmObject {

    @PrimaryKey
    private String uuid;

    private DBContact contact;
    private DBFlow flow;

    private Date started;
    private Date completed;

    // JSON for our run state
    private String runState;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public DBContact getContact() {
        return contact;
    }

    public void setContact(DBContact contact) {
        this.contact = contact;
    }

    public DBFlow getFlow() {
        return flow;
    }

    public void setFlow(DBFlow flow) {
        this.flow = flow;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getCompleted() {
        return completed;
    }

    public void setCompleted(Date completed) {
        this.completed = completed;
    }

    public String getRunState() {
        return runState;
    }

    public void setRunState(String runState) {
        this.runState = runState;
    }
}
