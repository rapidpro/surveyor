package io.rapidpro.surveyor.utils;

import com.nyaruka.goflow.mobile.Mobile;

public class EngineUtils {
    public static String migrateFlow(String definition) {
        try {
            return Mobile.migrateLegacyFlow(definition);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
