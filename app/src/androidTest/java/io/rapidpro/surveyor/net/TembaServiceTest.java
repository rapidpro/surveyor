package io.rapidpro.surveyor.net;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.net.responses.Org;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class TembaServiceTest extends BaseApplicationTest {

    @Test
    public void getOrg() throws IOException {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_org_get, "application/json", 200);

        Org org = getSurveyor().getTembaService().getOrg("abc123");
        assertThat(org.getUuid(), is("b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06"));
        assertThat(org.getName(), is("Nyaruka"));
        assertThat(org.getPrimaryLanguage(), is("eng"));
        assertThat(org.getLanguages(), arrayContaining("eng", "fra"));
        assertThat(org.getTimezone(), is("Africa/Kigali"));
        assertThat(org.getCountry(), is("RW"));
    }

    @Test
    public void getFields() throws IOException, InterruptedException {
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

    @Test
    public void getGroups() throws IOException {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_groups_get, "application/json", 200);

        List<Group> groups = getSurveyor().getTembaService().getGroups("abc123");
        assertThat(groups, hasSize(3));
        assertThat(groups.get(0).getUuid(), is("b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06"));
        assertThat(groups.get(0).getName(), is("Testers"));
        assertThat(groups.get(0).getQuery(), is(""));
        assertThat(groups.get(1).getUuid(), is("372aba66-16e2-44ee-8486-fb5cedfe51d9"));
        assertThat(groups.get(1).getName(), is("Customers"));
        assertThat(groups.get(1).getQuery(), is(""));
        assertThat(groups.get(2).getUuid(), is("63867d07-c033-4ef1-957c-85fa9708c19c"));
        assertThat(groups.get(2).getName(), is("Youth"));
        assertThat(groups.get(2).getQuery(), is("age <= 18"));
    }
}
