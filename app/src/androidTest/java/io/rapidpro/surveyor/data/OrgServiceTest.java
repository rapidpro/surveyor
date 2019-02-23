package io.rapidpro.surveyor.data;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import io.rapidpro.surveyor.net.TembaException;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.junit.Assert.assertThat;

public class OrgServiceTest extends BaseApplicationTest {
    private static final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

    @Test(expected = RuntimeException.class)
    public void get_throwsExceptionIfOrgNotExist() throws IOException {
        OrgService svc = getSurveyor().getOrgService();
        svc.get("b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06");
    }

    @Test
    public void get() throws IOException {
        // install an org without downloaded assets
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, 0, 0);

        OrgService svc = getSurveyor().getOrgService();

        // load single org by UUID
        Org org = svc.get(ORG_UUID);
        assertThat(org.getUuid(), is("b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06"));
        assertThat(org.getName(), is("Nyaruka"));
        assertThat(org.getCountry(), is("RW"));
        assertThat(org.getLanguages(), is(arrayContaining("eng", "fra")));
        assertThat(org.getPrimaryLanguage(), is("eng"));
        assertThat(org.getTimezone(), is("Africa/Kigali"));
        assertThat(org.getDateStyle(), is("day_first"));
        assertThat(org.isAnon(), is(false));
        assertThat(org.getToken(), is("797d44ef78f7845de0f4dbb42d5174505563dd77"));
        assertThat(org.getFlows(), is(Collections.<Flow>emptyList()));
        assertThat(org.hasAssets(), is(false));

        // install same org with downloaded assets
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        svc.clearCache();
        org = svc.get(ORG_UUID);

        assertThat(org.getFlows(), hasSize(3));
        assertThat(org.getFlow("ed8cf8d4-a42c-4ce1-a7e3-44a2918e3cec").getName(), is("Contact Details"));
        assertThat(org.getFlow("???"), is(nullValue()));

        assertThat(org.hasAssets(), is(true));
        assertThat(org.getAssets(), not(isEmptyString()));
    }

    @Test
    public void getOrFetch() throws IOException, TembaException {
        mockServerResponse(io.rapidpro.surveyor.test.R.raw.api_v2_org_get, "application/json", 200);

        OrgService svc = getSurveyor().getOrgService();

        Org org = svc.getOrFetch(ORG_UUID, "Nyaruka", "797d44ef78f7845de0f4dbb42d5174505563dd77");
        assertThat(org.getUuid(), is("b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06"));
        assertThat(org.getName(), is("Nyaruka"));
        assertThat(org.getCountry(), is("RW"));
        assertThat(org.getFlows(), is(Collections.<Flow>emptyList()));
        assertThat(org.hasAssets(), is(false));
    }
}
