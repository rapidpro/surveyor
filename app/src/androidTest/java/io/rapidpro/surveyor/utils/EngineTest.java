package io.rapidpro.surveyor.utils;

import com.nyaruka.goflow.mobile.AssetsSource;
import com.nyaruka.goflow.mobile.Contact;
import com.nyaruka.goflow.mobile.Environment;
import com.nyaruka.goflow.mobile.Event;
import com.nyaruka.goflow.mobile.FlowReference;
import com.nyaruka.goflow.mobile.MsgIn;
import com.nyaruka.goflow.mobile.Resume;
import com.nyaruka.goflow.mobile.Session;
import com.nyaruka.goflow.mobile.SessionAssets;
import com.nyaruka.goflow.mobile.Trigger;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.OrgService;
import io.rapidpro.surveyor.test.R;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class EngineTest extends BaseApplicationTest {

    @Test
    public void isSpecVersionSupported()  {
        assertThat(Engine.isSpecVersionSupported("11.5"), is(false));
        assertThat(Engine.isSpecVersionSupported("12.0"), is(true));
    }

    @Test
    public void migrateFlow() throws Exception {
        String legacyFlow = "{\"action_sets\":[],\"rule_sets\":[],\"base_language\":\"eng\",\"metadata\":{\"uuid\":\"061be894-4507-470c-a20b-34273bf915be\",\"name\":\"Survey\"}}";
        String migrated = Engine.migrateFlow(legacyFlow);

        assertThat(migrated, is("{\"uuid\":\"061be894-4507-470c-a20b-34273bf915be\",\"name\":\"Survey\",\"spec_version\":\"12.0\",\"language\":\"eng\",\"type\":\"\",\"revision\":0,\"expire_after_minutes\":0,\"localization\":{},\"nodes\":[]}"));
    }

    @Test(expected = EngineException.class)
    public void loadAssetsThrowsExceptionIfJsonInvalid() throws EngineException {
        Engine.loadAssets("{");
    }

    @Test
    public void twoQuestions() throws IOException, EngineException {
        final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";
        final String FLOW_UUID = "14ca824e-6607-4c11-82f5-18e298d0bd58";

        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);
        OrgService svc = new OrgService(getSurveyor().getOrgsDirectory(), SurveyorApplication.LOG);
        Org org = svc.get(ORG_UUID);

        String assetsJson = readRawResource(R.raw.org1_assets);

        AssetsSource source = Engine.loadAssets(assetsJson);
        SessionAssets assets = Engine.createSessionAssets(source);
        Session session = Engine.createSession(assets);

        Environment env = Engine.createEnvironment(org);
        Contact contact = Engine.createEmptyContact();
        FlowReference flow = Engine.createFlowReference(FLOW_UUID, "Two Questions");
        Trigger trigger = Engine.createManualTrigger(env, contact, flow);

        List<Event> events = Engine.startSession(session, trigger);

        assertThat(session.status(), is("waiting"));
        assertThat(events, hasSize(2));
        assertThat(events.get(0).type(), is("msg_created"));
        assertThat(events.get(1).type(), is("msg_wait"));

        MsgIn msg1 = Engine.createMsgIn("8e68adc6-9602-4969-b077-9b25c3b1e7b5", "I like club", null);
        Resume resume1 = Engine.createMsgResume(null, null, msg1);

        events = Engine.resumeSession(session, resume1);

        assertThat(session.status(), is("waiting"));
        assertThat(events, hasSize(4));
        assertThat(events.get(0).type(), is("msg_received"));
        assertThat(events.get(1).type(), is("run_result_changed"));
        assertThat(events.get(2).type(), is("msg_created"));
        assertThat(events.get(3).type(), is("msg_wait"));

        MsgIn msg2 = Engine.createMsgIn("8e68adc6-9602-4969-b077-9b25c3b1e7b5", "RED", null);
        Resume resume2 = Engine.createMsgResume(null, null, msg2);

        events = Engine.resumeSession(session, resume2);

        assertThat(session.status(), is("completed"));
        assertThat(events, hasSize(3));
        assertThat(events.get(0).type(), is("msg_received"));
        assertThat(events.get(1).type(), is("run_result_changed"));
        assertThat(events.get(2).type(), is("msg_created"));
    }
}