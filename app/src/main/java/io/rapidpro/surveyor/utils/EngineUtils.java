package io.rapidpro.surveyor.utils;

import com.nyaruka.goflow.mobile.Mobile;

/**
 * Wraps functionality in the goflow mobile library module
 */
public class EngineUtils {
    /**
     * Migrates a legacy flow definition to the new engine format
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
}
