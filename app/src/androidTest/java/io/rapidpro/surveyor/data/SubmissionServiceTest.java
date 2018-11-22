package io.rapidpro.surveyor.data;

import org.junit.Test;

import java.io.IOException;

import io.rapidpro.surveyor.test.BaseApplicationTest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SubmissionServiceTest extends BaseApplicationTest {
    private static final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

    @Test
    public void counts() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        Flow flow1 = org.getFlow("14ca824e-6607-4c11-82f5-18e298d0bd58");
        Flow flow2 = org.getFlow("ed8cf8d4-a42c-4ce1-a7e3-44a2918e3cec");
        Flow flow3 = org.getFlow("585958f3-ee7a-4f81-b4c2-fda374155681");

        SubmissionService svc = getSurveyor().getSubmissionService();

        Submission sub = svc.newSubmission(org, flow1);
        svc.newSubmission(org, flow1);
        svc.newSubmission(org, flow2);

        assertThat(svc.getPendingCount(org), is(3));
        assertThat(svc.getPendingCount(org, flow1), is(2));
        assertThat(svc.getPendingCount(org, flow2), is(1));
        assertThat(svc.getPendingCount(org, flow3), is(0));
    }
}
