package io.rapidpro.surveyor.utils;

import com.nyaruka.goflow.mobile.AssetsSource;
import com.nyaruka.goflow.mobile.Environment;
import com.nyaruka.goflow.mobile.Session;
import com.nyaruka.goflow.mobile.SessionAssets;

import org.junit.Test;

import java.io.IOException;

import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.OrgService;
import io.rapidpro.surveyor.test.R;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EngineUtilsTest extends BaseApplicationTest {

    @Test
    public void isSpecVersionSupported()  {
        assertThat(EngineUtils.isSpecVersionSupported("11.5"), is(false));
        assertThat(EngineUtils.isSpecVersionSupported("12.0"), is(true));
    }

    @Test
    public void migrateFlow() throws Exception {
        String legacyFlow = "{\"action_sets\":[],\"rule_sets\":[],\"base_language\":\"eng\",\"metadata\":{\"uuid\":\"061be894-4507-470c-a20b-34273bf915be\",\"name\":\"Survey\"}}";
        String migrated = EngineUtils.migrateFlow(legacyFlow);

        assertThat(migrated, is("{\"uuid\":\"061be894-4507-470c-a20b-34273bf915be\",\"name\":\"Survey\",\"spec_version\":\"12.0\",\"language\":\"eng\",\"type\":\"\",\"revision\":0,\"expire_after_minutes\":0,\"localization\":{},\"nodes\":[]}"));
    }

    @Test(expected = EngineException.class)
    public void loadAssetsThrowsExceptionIfJsonInvalid() throws EngineException {
        EngineUtils.loadAssets("{");
    }

    @Test
    public void createEnvironment() throws IOException, EngineException {
        final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);
        OrgService svc = new OrgService(getSurveyor().getOrgsDirectory(), SurveyorApplication.LOG);
        Org org = svc.get(ORG_UUID);

        Environment env = EngineUtils.createEnvironment(org);
    }

    @Test
    public void createSession() throws IOException, EngineException {
        String assetsJson = readRawResource(R.raw.org1_assets);

        AssetsSource source = EngineUtils.loadAssets(assetsJson);
        SessionAssets assets = EngineUtils.createSessionAssets(source);
        Session session = EngineUtils.createSession(assets);
    }
}