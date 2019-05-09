package io.rapidpro.surveyor.engine;

import com.nyaruka.goflow.mobile.Resume;
import com.nyaruka.goflow.mobile.SessionAssets;
import com.nyaruka.goflow.mobile.Trigger;
import com.nyaruka.goflow.mobile.Wait;

public class Session {
    private com.nyaruka.goflow.mobile.Session target;

    /**
     * Creates a new session
     */
    Session(com.nyaruka.goflow.mobile.Session target) {
        this.target = target;
    }

    /**
     * Resumes this session with a resume
     *
     * @param resume the resume
     * @return the events
     */
    public Sprint resume(Resume resume) throws EngineException {
        try {
            return Sprint.fromNative(target.resume(resume));
        } catch (Exception e) {
            throw new EngineException(e);
        }
    }

    /**
     * Gets the assets used by this session
     *
     * @return the assets
     */
    public SessionAssets getAssets() {
        return target.assets();
    }

    /**
     * Gets the status of this session
     *
     * @return the status
     */
    public String getStatus() {
        return target.status();
    }

    /**
     * Gets whether this session is waiting for input
     *
     * @return true if session is waiting
     */
    public boolean isWaiting() {
        return getStatus().equals("waiting");
    }

    public Wait getWait() {
        return target.getWait();
    }

    /**
     * Marshals this session to JSON
     *
     * @return the JSON
     */
    public String toJSON() throws EngineException {
        try {
            return target.toJSON();
        } catch (Exception e) {
            throw new EngineException(e);
        }
    }
}
