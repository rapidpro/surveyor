package io.rapidpro.surveyor.engine;

import com.nyaruka.goflow.mobile.AssetsSource;
import com.nyaruka.goflow.mobile.Environment;
import com.nyaruka.goflow.mobile.MsgIn;
import com.nyaruka.goflow.mobile.Resume;
import com.nyaruka.goflow.mobile.SessionAssets;
import com.nyaruka.goflow.mobile.StringSlice;
import com.nyaruka.goflow.mobile.Trigger;
import com.vdurmont.semver4j.Semver;

import org.apache.commons.lang3.tuple.Pair;
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
    public void currentSpecVersion() {
        assertThat(Engine.currentSpecVersion(), is(new Semver("13.0.0")));
    }

    @Test
    public void isSpecVersionSupported() {
        assertThat(Engine.isSpecVersionSupported("12.0"), is(false));
        assertThat(Engine.isSpecVersionSupported("13.0"), is(true));
        assertThat(Engine.isSpecVersionSupported("13.5"), is(true));
        assertThat(Engine.isSpecVersionSupported("14.0"), is(false));
    }

    @Test
    public void migrateLegacyDefinition() {
        String legacyFlow = "{\"action_sets\":[],\"rule_sets\":[],\"base_language\":\"eng\",\"metadata\":{\"uuid\":\"061be894-4507-470c-a20b-34273bf915be\",\"name\":\"Survey\"}}";
        String migrated = Engine.migrateLegacyDefinition(legacyFlow);

        assertThat(migrated, is("{\"uuid\":\"061be894-4507-470c-a20b-34273bf915be\",\"name\":\"Survey\",\"spec_version\":\"13.0.0\",\"language\":\"eng\",\"type\":\"\",\"revision\":0,\"expire_after_minutes\":0,\"localization\":{},\"nodes\":[],\"_ui\":{\"nodes\":{},\"stickies\":{}}}"));
    }

    @Test(expected = EngineException.class)
    public void loadAssetsThrowsExceptionIfJsonInvalid() throws EngineException {
        Engine.loadAssets("{");
    }

    @Test
    public void twoQuestions() throws IOException, EngineException {
        final String FLOW_UUID = "bdd61538-5f50-4836-a8fb-acaafd64ddb1";

        Pair<Session, Sprint> result = startSession(FLOW_UUID);
        Session session = result.getLeft();
        Sprint sprint = result.getRight();

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
        assertThat(marshaled.substring(0, 50), is("{\"type\":\"messaging_offline\",\"environment\":{\"date_f"));

        // and unmarshal back
        Session session2 = Engine.getInstance().readSession(session.getAssets(), marshaled);
        assertThat(session2.getStatus(), is("completed"));
    }

    @Test
    public void multimedia() throws IOException, EngineException {
        final String FLOW_UUID = "e54809ba-2f28-439b-b90b-c623eafa05ae";

        Pair<Session, Sprint> result = startSession(FLOW_UUID);
        Session session = result.getLeft();
        Sprint sprint = result.getRight();

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
    public void contactDetails() throws IOException, EngineException {
        final String FLOW_UUID = "ed8cf8d4-a42c-4ce1-a7e3-44a2918e3cec";

        Pair<Session, Sprint> result = startSession(FLOW_UUID);
        Session session = result.getLeft();
        Sprint sprint = result.getRight();

        assertThat(session.getStatus(), is("waiting"));
        assertThat(session.isWaiting(), is(true));
        assertThat(sprint.getEvents(), hasSize(2));
        assertThat(sprint.getEvents().get(0).type(), is("msg_created"));
        assertThat(sprint.getEvents().get(1).type(), is("msg_wait"));

        MsgIn msg1 = Engine.createMsgIn("Bob");
        Resume resume1 = Engine.createMsgResume(null, null, msg1);
        sprint = session.resume(resume1);

        assertThat(session.getStatus(), is("waiting"));
        assertThat(session.isWaiting(), is(true));
        assertThat(sprint.getEvents(), hasSize(5));
        assertThat(sprint.getEvents().get(0).type(), is("msg_received"));
        assertThat(sprint.getEvents().get(1).type(), is("run_result_changed"));
        assertThat(sprint.getEvents().get(2).type(), is("contact_name_changed"));
        assertThat(sprint.getEvents().get(3).type(), is("msg_created"));
        assertThat(sprint.getEvents().get(4).type(), is("msg_wait"));

        assertThat(sprint.getModifiers(), hasSize(1));
        assertThat(sprint.getModifiers().get(0).type(), is("name"));
        assertThat(sprint.getModifiers().get(0).payload(), is("{\"type\":\"name\",\"name\":\"Bob\"}"));

        MsgIn msg2 = Engine.createMsgIn("+593979123456");
        Resume resume2 = Engine.createMsgResume(null, null, msg2);
        sprint = session.resume(resume2);

        assertThat(session.getStatus(), is("waiting"));
        assertThat(session.isWaiting(), is(true));
        assertThat(sprint.getEvents(), hasSize(6));
        assertThat(sprint.getEvents().get(0).type(), is("msg_received"));
        assertThat(sprint.getEvents().get(1).type(), is("run_result_changed"));
        assertThat(sprint.getEvents().get(2).type(), is("contact_urns_changed"));
        assertThat(sprint.getEvents().get(3).type(), is("contact_groups_changed"));
        assertThat(sprint.getEvents().get(4).type(), is("msg_created"));
        assertThat(sprint.getEvents().get(5).type(), is("msg_wait"));

        assertThat(sprint.getModifiers(), hasSize(2));
        assertThat(sprint.getModifiers().get(0).type(), is("urn"));
        assertThat(sprint.getModifiers().get(0).payload(), is("{\"type\":\"urn\",\"urn\":\"tel:+593979123456\",\"modification\":\"append\"}"));
        assertThat(sprint.getModifiers().get(1).type(), is("groups"));
        assertThat(sprint.getModifiers().get(1).payload(), is("{\"type\":\"groups\",\"groups\":[{\"uuid\":\"6696cabf-eb5e-42bf-bcc6-f0c8be9b1316\",\"name\":\"Testers\"}],\"modification\":\"add\"}"));

        MsgIn msg3 = Engine.createMsgIn("37");
        Resume resume3 = Engine.createMsgResume(null, null, msg3);
        sprint = session.resume(resume3);

        assertThat(session.getStatus(), is("completed"));
        assertThat(session.isWaiting(), is(false));
        assertThat(sprint.getEvents(), hasSize(4));
        assertThat(sprint.getEvents().get(0).type(), is("msg_received"));
        assertThat(sprint.getEvents().get(1).type(), is("run_result_changed"));
        assertThat(sprint.getEvents().get(2).type(), is("contact_field_changed"));
        assertThat(sprint.getEvents().get(3).type(), is("msg_created"));

        assertThat(sprint.getModifiers(), hasSize(1));
        assertThat(sprint.getModifiers().get(0).type(), is("field"));
        assertThat(sprint.getModifiers().get(0).payload(), is("{\"type\":\"field\",\"field\":{\"key\":\"age\",\"name\":\"Age\"},\"value\":{\"text\":\"37\",\"number\":37}}"));
    }

    @Test
    public void listToSlice() {
        List<String> vals = Arrays.asList("Foo", "bar");
        StringSlice slice = Engine.listToSlice(vals);

        assertThat(slice.length(), is(2L));
        assertThat(slice.get(0L), is("Foo"));
        assertThat(slice.get(1L), is("bar"));
    }

    private Pair<Session, Sprint> startSession(String flowUUID) throws IOException, EngineException {
        final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

        installOrg(ORG_UUID, R.raw.org1_details, R.raw.org1_flows, R.raw.org1_assets);
        OrgService svc = getSurveyor().getOrgService();
        Org org = svc.get(ORG_UUID);
        Flow flow = org.getFlow(flowUUID);

        String assetsJson = readResourceAsString(R.raw.org1_assets);

        AssetsSource source = Engine.loadAssets(assetsJson);
        SessionAssets assets = Engine.createSessionAssets(source);

        Environment env = Engine.createEnvironment(org);
        Contact contact = Contact.createEmpty(assets);
        Trigger trigger = Engine.createManualTrigger(env, contact, flow.toReference());

        return Engine.getInstance().newSession(assets, trigger);
    }
}