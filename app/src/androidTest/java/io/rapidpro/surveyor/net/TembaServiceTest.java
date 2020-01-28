package io.rapidpro.surveyor.net;

import android.net.Uri;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.rapidpro.surveyor.net.responses.Boundary;
import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Flow;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.net.responses.Org;
import io.rapidpro.surveyor.net.responses.Token;
import io.rapidpro.surveyor.net.responses.TokenResults;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import io.rapidpro.surveyor.utils.RawJson;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TembaServiceTest extends BaseApplicationTest {

    /**
     * @see TembaService#authenticate(String, String, Callback)
     */
    @Test
    public void authenticate() throws IOException, InterruptedException {
        // needed to ensure test waits for async request
        final CountDownLatch latch = new CountDownLatch(1);

        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_authenticate_post, "application/json", 200);

        getSurveyor().getTembaService().authenticate("bob@nyaruka.com", "Qwerty123", new Callback<TokenResults>() {
            @Override
            public void onResponse(Call<TokenResults> call, Response<TokenResults> response) {
                List<Token> tokens = response.body().getTokens();
                assertThat(tokens, hasSize(2));
                assertThat(tokens.get(0).getToken(), is("23453fwf33fw35g3222f67778"));
                assertThat(tokens.get(0).getOrg().getUuid(), is("9f578940-215c-4e58-b399-e65d74041dc8"));
                assertThat(tokens.get(0).getOrg().getName(), is("UNICEF"));

                latch.countDown();
            }

            @Override
            public void onFailure(Call<TokenResults> call, Throwable t) {
                fail();

                latch.countDown();
            }
        });

        latch.await(30, TimeUnit.SECONDS);
    }

    /**
     * @see TembaService#getBoundaries(String)
     */
    @Test
    public void getBoundaries() throws Exception {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_boundaries_get, "application/json", 200);

        List<Boundary> boundaries = getSurveyor().getTembaService().getBoundaries("abc123");
        assertThat(boundaries, hasSize(4));
        assertThat(boundaries.get(0).getOsmID(), is("192787"));
        assertThat(boundaries.get(0).getName(), is("Nigeria"));
        assertThat(boundaries.get(0).getParent(), is(nullValue()));
        assertThat(boundaries.get(0).getLevel(), is(0));
        assertThat(boundaries.get(0).getAliases(), is(emptyArray()));
        assertThat(boundaries.get(1).getOsmID(), is("3698564"));
        assertThat(boundaries.get(1).getName(), is("Yobe"));
        assertThat(boundaries.get(1).getParent().getOsmID(), is("192787"));
        assertThat(boundaries.get(1).getParent().getName(), is("Nigeria"));
        assertThat(boundaries.get(1).getAliases(), is(arrayContaining("Iobe")));
    }

    /**
     * @see TembaService#getOrg(String)
     */
    @Test
    public void getOrg() throws Exception {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_org_get, "application/json", 200);

        Org org = getSurveyor().getTembaService().getOrg("abc123");
        assertThat(org.getUuid(), is("dc8123a1-168c-4962-ab9e-f784f3d804a2"));
        assertThat(org.getName(), is("Nyaruka"));
        assertThat(org.getPrimaryLanguage(), is("eng"));
        assertThat(org.getLanguages(), arrayContaining("eng", "fra"));
        assertThat(org.getTimezone(), is("Africa/Kigali"));
        assertThat(org.getDateStyle(), is("day_first"));
        assertThat(org.getCountry(), is("RW"));
        assertThat(org.isAnon(), is(false));
    }

    /**
     * @see TembaService#getFields(String)
     */
    @Test
    public void getFields() throws Exception {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_fields_get_page_1, "application/json", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_fields_get_page_2, "application/json", 200);

        List<Field> fields = getSurveyor().getTembaService().getFields("abc123");
        assertThat(fields, hasSize(3));
        assertThat(fields.get(0).getKey(), is("gender"));
        assertThat(fields.get(0).getLabel(), is("Gender"));
        assertThat(fields.get(0).getValueType(), is("text"));
        assertThat(fields.get(1).getKey(), is("age"));
        assertThat(fields.get(1).getLabel(), is("Age"));
        assertThat(fields.get(1).getValueType(), is("numeric"));
        assertThat(fields.get(2).getKey(), is("join_date"));
        assertThat(fields.get(2).getLabel(), is("Join Date"));
        assertThat(fields.get(2).getValueType(), is("datetime"));

        RecordedRequest request1 = mockServer.takeRequest();
        assertThat(request1.getPath(), is("/api/v2/fields.json"));
        assertThat(request1.getHeader("Authorization"), is("Token abc123"));

        RecordedRequest request2 = mockServer.takeRequest();
        assertThat(request2.getPath(), is("/api/v2/fields.json?cursor=123456789"));
    }

    /**
     * @see TembaService#getFlows(String)
     */
    @Test
    public void getFlows() throws Exception {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_flows_get, "application/json", 200);

        List<Flow> flows = getSurveyor().getTembaService().getFlows("abc123");
        assertThat(flows, hasSize(3));
        assertThat(flows.get(0).getUuid(), is("bdd61538-5f50-4836-a8fb-acaafd64ddb1"));
        assertThat(flows.get(0).getName(), is("Two Questions"));
        assertThat(flows.get(0).getType(), is("survey"));
        assertThat(flows.get(0).isArchived(), is(false));
        assertThat(flows.get(0).getExpires(), is(10080));
        assertThat(flows.get(1).getUuid(), is("ed8cf8d4-a42c-4ce1-a7e3-44a2918e3cec"));
        assertThat(flows.get(1).getName(), is("Contact Details"));
        assertThat(flows.get(2).getUuid(), is("e54809ba-2f28-439b-b90b-c623eafa05ae"));
        assertThat(flows.get(2).getName(), is("Multimedia"));
    }

    /**
     * @see TembaService#getGroups(String)
     */
    @Test
    public void getGroups() throws Exception {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_groups_get, "application/json", 200);

        List<Group> groups = getSurveyor().getTembaService().getGroups("abc123");
        assertThat(groups, hasSize(3));
        assertThat(groups.get(0).getUuid(), is("6696cabf-eb5e-42bf-bcc6-f0c8be9b1316"));
        assertThat(groups.get(0).getName(), is("Testers"));
        assertThat(groups.get(0).getQuery(), is(""));
        assertThat(groups.get(1).getUuid(), is("372aba66-16e2-44ee-8486-fb5cedfe51d9"));
        assertThat(groups.get(1).getName(), is("Customers"));
        assertThat(groups.get(1).getQuery(), is(""));
        assertThat(groups.get(2).getUuid(), is("63867d07-c033-4ef1-957c-85fa9708c19c"));
        assertThat(groups.get(2).getName(), is("Youth"));
        assertThat(groups.get(2).getQuery(), is("age <= 18"));
    }

    /**
     * @see TembaService#getDefinitions(String, List)
     */
    @Test
    public void getDefinitions_inLegacyFormat() throws Exception {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_flows_get, "application/json", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_definitions_get_v11, "application/json", 200);

        List<Flow> flows = getSurveyor().getTembaService().getFlows("abc123");
        List<RawJson> definitions = getSurveyor().getTembaService().getDefinitions("abc123", flows);

        // check flow definitions have been migrated
        assertThat(definitions, hasSize(3));
        assertThat(definitions.get(0).toString(), startsWith("{\"entry\":\"036901e0-abb8-4979-92cb-f0d43aeb5b68\""));
    }

    /**
     * @see TembaService#getDefinitions(String, List)
     */
    @Test
    public void getDefinitions() throws Exception {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_flows_get, "application/json", 200);
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_definitions_get_v13, "application/json", 200);

        List<Flow> flows = getSurveyor().getTembaService().getFlows("abc123");
        List<RawJson> definitions = getSurveyor().getTembaService().getDefinitions("abc123", flows);

        assertThat(definitions, hasSize(3));
        assertThat(definitions.get(0).toString(), startsWith("{\"uuid\":\"ed8cf8d4-a42c-4ce1-a7e3-44a2918e3cec\",\"name\":\"Contact Details\""));
    }

    /**
     * @see TembaService#uploadMedia(String, Uri)
     */
    @Test
    public void uploadMedia() throws Exception {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_media_post, "application/json", 200);

        File upload = new File(getSurveyor().getExternalCacheDir(), "test.jpg");
        FileUtils.write(upload, "I'm an image!");
        Uri uri = getSurveyor().getUriForFile(upload);

        String newUrl = getSurveyor().getTembaService().uploadMedia("abc123", uri);

        assertThat(newUrl, is("https://uploads.rapidpro.io/1224626264215.jpg"));

        RecordedRequest request1 = mockServer.takeRequest();
        assertThat(request1.getPath(), is("/api/v2/media.json"));
        assertThat(request1.getHeader("Authorization"), is("Token abc123"));
        assertThat(request1.getMethod(), is("POST"));
    }
}
