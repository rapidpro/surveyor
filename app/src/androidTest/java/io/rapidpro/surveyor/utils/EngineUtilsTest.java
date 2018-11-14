package io.rapidpro.surveyor.utils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EngineUtilsTest {

    @Test
    public void migrateFlow() throws Exception {
        String legacyFlow = "{\"action_sets\":[],\"rule_sets\":[],\"base_language\":\"eng\",\"metadata\":{\"uuid\":\"061be894-4507-470c-a20b-34273bf915be\",\"name\":\"Survey\"}}";
        String migrated = EngineUtils.migrateFlow(legacyFlow);

        assertThat(migrated, is("{\"uuid\":\"061be894-4507-470c-a20b-34273bf915be\",\"name\":\"Survey\",\"language\":\"eng\",\"type\":\"\",\"revision\":0,\"expire_after_minutes\":0,\"localization\":{},\"nodes\":[]}"));
    }
}