package io.rapidpro.surveyor.data;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import io.rapidpro.surveyor.test.BaseApplicationTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.junit.Assert.assertThat;

public class OrgTest extends BaseApplicationTest {
    private static final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

    @Test
    public void load() throws IOException {
        // install an org without downloaded assets
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, 0, 0);

        // load without flows
        Org org = Org.load(ORG_UUID, false);
        assertThat(org.getUuid(), is("b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06"));
        assertThat(org.getName(), is("Nyaruka"));
        assertThat(org.getCountry(), is("RW"));
        assertThat(org.getLanguages(), is(arrayContaining("eng", "fra")));
        assertThat(org.getPrimaryLanguage(), is("eng"));
        assertThat(org.getTimezone(), is("Africa/Kigali"));
        assertThat(org.getDateStyle(), is("day_first"));
        assertThat(org.isAnon(), is(false));
        assertThat(org.getToken(), is("67537873784848322fghsaf3g"));

        assertThat(org.hasAssets(), is(false));
        assertThat(org.getFlows(), is(nullValue()));

        // load with flows
        org = Org.load(ORG_UUID, true);

        assertThat(org.hasAssets(), is(false));
        assertThat(org.getFlows(), is(Collections.<FlowSummary>emptyList()));

        // install same org with downloaded assets
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        org = Org.load(ORG_UUID, true);

        assertThat(org.hasAssets(), is(true));
        assertThat(org.getFlows(), hasSize(2));
        assertThat(org.getFlow("ed8cf8d4-a42c-4ce1-a7e3-44a2918e3cec").getName(), is("Ask Name"));
        assertThat(org.getFlow("???"), is(nullValue()));
    }
}
