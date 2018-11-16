package io.rapidpro.surveyor.utils;

import com.nyaruka.goflow.mobile.AssetsSource;
import com.nyaruka.goflow.mobile.Environment;
import com.nyaruka.goflow.mobile.Mobile;
import com.nyaruka.goflow.mobile.Session;
import com.nyaruka.goflow.mobile.SessionAssets;

import io.rapidpro.surveyor.data.Org;

/**
 * Wraps functionality in the goflow mobile library module
 */
public class EngineUtils {
    /**
     * Migrates a legacy flow definition to the new engine format
     *
     * @param definition the legacy definition
     * @return the new definition
     */
    public static String migrateFlow(String definition) {
        try {
            return Mobile.migrateLegacyFlow(definition);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        return new Environment(dateFormat, timeformat, org.getTimezone(), org.getPrimaryLanguage());
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
     * Creates a new session
     *
     * @param assets the session assets
     * @return the session
     */
    public static Session createSession(SessionAssets assets) throws EngineException {
        try {
            return new Session(assets, null);
        } catch (Exception e) {
            throw new EngineException(e);
        }
    }
}
