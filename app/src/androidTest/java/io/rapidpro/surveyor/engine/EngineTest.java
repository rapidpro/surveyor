package io.rapidpro.surveyor.engine;

import com.nyaruka.goflow.mobile.AssetsSource;
import com.nyaruka.goflow.mobile.Contact;
import com.nyaruka.goflow.mobile.Environment;
import com.nyaruka.goflow.mobile.Event;
import com.nyaruka.goflow.mobile.FlowReference;
import com.nyaruka.goflow.mobile.MsgIn;
import com.nyaruka.goflow.mobile.Resume;
import com.nyaruka.goflow.mobile.SessionAssets;
import com.nyaruka.goflow.mobile.StringSlice;
import com.nyaruka.goflow.mobile.Trigger;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.rapidpro.surveyor.data.Flow;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.OrgService;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import io.rapidpro.surveyor.test.R;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class EngineTest extends BaseApplicationTest {

    @Test
    public void isSpecVersionSupported() {
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
        final String FLOW_UUID = "bdd61538-5f50-4836-a8fb-acaafd64ddb1";

        installOrg(ORG_UUID, R.raw.org1_details, R.raw.org1_flows, R.raw.org1_assets);
        OrgService svc = getSurveyor().getOrgService();
        Org org = svc.get(ORG_UUID);

        String assetsJson = readResourceAsString(R.raw.org1_assets);

        AssetsSource source = Engine.loadAssets(assetsJson);
        SessionAssets assets = Engine.createSessionAssets(source);
        Session session = new Session(assets);

        Environment env = Engine.createEnvironment(org);
        Contact contact = Engine.createEmptyContact();
        FlowReference flow = Engine.createFlowReference(FLOW_UUID, "Two Questions");
        Trigger trigger = Engine.createManualTrigger(env, contact, flow);

        Sprint sprint = session.start(trigger);

        assertThat(session.getStatus(), is("waiting"));
        assertThat(session.isWaiting(), is(true));
        assertThat(sprint.getEvents(), hasSize(2));
        assertThat(sprint.getEvents().get(0).type(), is("msg_created"));
        assertThat(sprint.getEvents().get(1).type(), is("msg_wait"));

        MsgIn msg1 = Engine.createMsgIn("I like club");
        Resume resume1 = Engine.createMsgResume(null, null, msg1);

        sprint = session.resume(resume1);

        assertThat(session.getStatus(), is("waiting"));
        assertThat(session.isWaiting(), is(true));
        assertThat(sprint.getEvents(), hasSize(4));
        assertThat(sprint.getEvents().get(0).type(), is("msg_received"));
        assertThat(sprint.getEvents().get(0).payload(), containsString("I like club"));
        assertThat(sprint.getEvents().get(1).type(), is("run_result_changed"));
        assertThat(sprint.getEvents().get(2).type(), is("msg_created"));
        assertThat(sprint.getEvents().get(3).type(), is("msg_wait"));

        MsgIn msg2 = Engine.createMsgIn("RED");
        Resume resume2 = Engine.createMsgResume(null, null, msg2);

        sprint = session.resume(resume2);

        assertThat(session.getStatus(), is("completed"));
        assertThat(session.isWaiting(), is(false));
        assertThat(sprint.getEvents(), hasSize(3));
        assertThat(sprint.getEvents().get(0).type(), is("msg_received"));
        assertThat(sprint.getEvents().get(1).type(), is("run_result_changed"));
        assertThat(sprint.getEvents().get(2).type(), is("msg_created"));

        // try to marshal to JSON
        String marshaled = session.toJSON();
        assertThat(marshaled.substring(0, 50), is("{\"environment\":{\"date_format\":\"DD-MM-YYYY\",\"time_f"));

        // and unmarshal back
        Session session2 = Session.fromJson(assets, marshaled);
        assertThat(session2.getStatus(), is("completed"));
    }

    @Test
    public void multimedia() throws IOException, EngineException {
        final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";
        final String FLOW_UUID = "e54809ba-2f28-439b-b90b-c623eafa05ae";

        installOrg(ORG_UUID, R.raw.org1_details, R.raw.org1_flows, R.raw.org1_assets);
        OrgService svc = getSurveyor().getOrgService();
        Org org = svc.get(ORG_UUID);
        Flow flow = org.getFlow(FLOW_UUID);

        String assetsJson = readResourceAsString(R.raw.org1_assets);

        AssetsSource source = Engine.loadAssets(assetsJson);
        SessionAssets assets = Engine.createSessionAssets(source);
        Session session = new Session(assets);

        Environment env = Engine.createEnvironment(org);
        Contact contact = Engine.createEmptyContact();
        Trigger trigger = Engine.createManualTrigger(env, contact, flow.toReference());

        Sprint sprint = session.start(trigger);

        assertThat(session.getStatus(), is("waiting"));
        assertThat(session.isWaiting(), is(true));
        assertThat(session.getWait().hint(), is(notNullValue()));
        assertThat(session.getWait().hint().type(), is("image"));
        assertThat(sprint.getEvents(), hasSize(2));
        assertThat(sprint.getEvents().get(0).type(), is("msg_created"));
        assertThat(sprint.getEvents().get(1).type(), is("msg_wait"));

        MsgIn msg1 = Engine.createMsgIn("", "content://io.rapidpro.surveyor/files/selfie.jpg");
        Resume resume1 = Engine.createMsgResume(null, null, msg1);

        sprint = session.resume(resume1);

        assertThat(session.getStatus(), is("waiting"));
        assertThat(session.isWaiting(), is(true));
        assertThat(sprint.getEvents(), hasSize(4));
        assertThat(sprint.getEvents().get(0).type(), is("msg_received"));
        assertThat(sprint.getEvents().get(0).payload(), containsString("content://io.rapidpro.surveyor/files/selfie.jpg"));
        assertThat(sprint.getEvents().get(1).type(), is("run_result_changed"));
        assertThat(sprint.getEvents().get(2).type(), is("msg_created"));
        assertThat(sprint.getEvents().get(3).type(), is("msg_wait"));
    }

    @Test
    public void listToSlice() {
        List<String> vals = Arrays.asList("Foo", "bar");
        StringSlice slice = Engine.listToSlice(vals);

        assertThat(slice.length(), is(2L));
        assertThat(slice.get(0L), is("Foo"));
        assertThat(slice.get(1L), is("bar"));
    }
}