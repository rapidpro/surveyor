package io.rapidpro.surveyor.engine;

import com.nyaruka.goflow.mobile.AssetsSource;
import com.nyaruka.goflow.mobile.Environment;
import com.nyaruka.goflow.mobile.FlowReference;
import com.nyaruka.goflow.mobile.Mobile;
import com.nyaruka.goflow.mobile.MsgIn;
import com.nyaruka.goflow.mobile.Resume;
import com.nyaruka.goflow.mobile.SessionAndSprint;
import com.nyaruka.goflow.mobile.SessionAssets;
import com.nyaruka.goflow.mobile.StringSlice;
import com.nyaruka.goflow.mobile.Trigger;
import com.vdurmont.semver4j.Semver;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.rapidpro.surveyor.data.Org;

/**
 * Wraps functionality in the goflow mobile library module
 */
public class Engine {
    /**
     * The engine singleton
     */
    private static Engine engine = new Engine();

    private com.nyaruka.goflow.mobile.Engine target;

    public static Engine getInstance() {
        return engine;
    }

    /**
     * Creates a new engine
     */
    private Engine() {
        this.target = new com.nyaruka.goflow.mobile.Engine(null);
    }

    /**
     * Migrates a legacy flow definition to the new engine format
     *
     * @param definition the legacy definition
     * @return the new definition
     */
    public static String migrateLegacyDefinition(String definition) {
        try {
            return Mobile.migrateLegacyDefinition(definition);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the current spec version
     *
     * @return the spec version
     */
    public static Semver currentSpecVersion() {
        return new Semver(Mobile.currentSpecVersion(), Semver.SemverType.LOOSE);
    }

    /**
     * Gets whether the given flow spec version is supported by the flow engine
     *
     * @param ver the spec version
     * @return true if supported
     */
    public static boolean isSpecVersionSupported(String ver) {
        return Mobile.isSpecVersionSupported(ver);
    }

    /**
     * Creates an engine environment from the given org
     *
     * @param org the org
     * @return the environment
     */
    public static Environment createEnvironment(Org org) {
        String dateFormat = org.getDateStyle().equals("day_first") ? "DD-MM-YYYY" : "MM-DD-YYYY";
        String timeformat = "tt:mm";
        StringSlice languages = listToSlice(Arrays.asList(org.getLanguages()));
        String redactionPolicy = org.isAnon() ? "urns" : "none";
        return new Environment(dateFormat, timeformat, org.getTimezone(), org.getPrimaryLanguage(), languages, org.getCountry(), redactionPolicy);
    }

    /**
     * Loads an assets source from the given JSON
     *
     * @param json the assets JSON
     * @return the source
     */
    public static AssetsSource loadAssets(String json) throws EngineException {
        try {
            return new AssetsSource(json);
        } catch (Exception e) {
            throw new EngineException(e);
        }
    }

    /**
     * Creates a new session assets instance from an assets source
     *
     * @param source the source
     * @return the session assets
     */
    public static SessionAssets createSessionAssets(AssetsSource source) throws EngineException {
        try {
            return new SessionAssets(source);
        } catch (Exception e) {
            throw new EngineException(e);
        }
    }

    /**
     * Creates a new incoming message
     *
     * @param text the message text
     * @return the message
     */
    public static MsgIn createMsgIn(String text) {
        return new MsgIn(UUID.randomUUID().toString(), text, null);
    }

    /**
     * Creates a new incoming message with an attachment
     *
     * @param text       the message text
     * @param attachment the message attachment
     * @return the message
     */
    public static MsgIn createMsgIn(String text, String attachment) {
        return new MsgIn(UUID.randomUUID().toString(), text, listToSlice(Collections.singletonList(attachment)));
    }

    /**
     * Creates a new manual trigger
     *
     * @param env     the environment
     * @param contact the contact
     * @param flow    the flow reference
     * @return the trigger
     */
    public static Trigger createManualTrigger(Environment env, Contact contact, FlowReference flow) {
        return Mobile.newManualTrigger(env, contact != null ? contact.target : null, flow);
    }

    /**
     * Creates a new message resume
     *
     * @param env     the environment
     * @param contact the contact
     * @param msg     the message
     * @return the resume
     */
    public static Resume createMsgResume(Environment env, Contact contact, MsgIn msg) {
        return Mobile.newMsgResume(env, contact != null ? contact.target : null, msg);
    }

    static StringSlice listToSlice(List<String> items) {
        if (items == null) {
            return null;
        }
        StringSlice slice = new StringSlice(items.size());
        for (String item : items) {
            slice.add(item);
        }
        return slice;
    }

    /**
     * Creates and starts a new session
     *
     * @param assets the session assets
     */
    public Pair<Session, Sprint> newSession(SessionAssets assets, Trigger trigger) throws EngineException {
        try {
            SessionAndSprint ss = this.target.newSession(assets, trigger);
            return new ImmutablePair<>(
                    new Session(ss.session()),
                    Sprint.fromNative(ss.sprint())
            );
        } catch (Exception e) {
            throw new EngineException(e);
        }
    }

    /**
     * Reads an existing session from JSON
     *
     * @param assets the session assets
     * @param json   the JSON
     */
    public Session readSession(SessionAssets assets, String json) throws EngineException {
        try {
            return new Session(this.target.readSession(assets, json));
        } catch (Exception e) {
            throw new EngineException(e);
        }
    }
}
