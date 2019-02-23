package io.rapidpro.surveyor.data;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class SubmissionServiceTest extends BaseApplicationTest {
    private static final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

    @Test
    public void getCompleted() throws IOException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        Flow flow1 = org.getFlow("bdd61538-5f50-4836-a8fb-acaafd64ddb1");
        Flow flow2 = org.getFlow("ed8cf8d4-a42c-4ce1-a7e3-44a2918e3cec");
        Flow flow3 = org.getFlow("e54809ba-2f28-439b-b90b-c623eafa05ae");

        SubmissionService svc = getSurveyor().getSubmissionService();

        Submission sub1 = svc.newSubmission(org, flow1);
        sub1.complete();

        assertThat(sub1.isCompleted(), is(true));

        Submission sub2 = svc.newSubmission(org, flow1);
        sub2.complete();
        Submission sub3 = svc.newSubmission(org, flow2);
        sub3.complete();
        Submission sub4 = svc.newSubmission(org, flow2);

        assertThat(sub1.getOrg(), is(org));
        assertThat(sub2.getOrg(), is(org));
        assertThat(sub3.getOrg(), is(org));
        assertThat(sub4.getOrg(), is(org));

        File submissionsDir = new File(getSurveyor().getUserDirectory(), "test_submissions");
        assertThat(submissionsDir.exists(), is(true));

        File orgDir = new File(submissionsDir, ORG_UUID);
        assertThat(orgDir.exists(), is(true));

        File flow1Dir = new File(orgDir, "bdd61538-5f50-4836-a8fb-acaafd64ddb1");
        assertThat(flow1Dir.exists(), is(true));

        File sub1Dir = new File(flow1Dir, sub1.getUuid());

        Logger.d("Checking " + sub1Dir.getAbsolutePath() + "  ....");

        assertThat(sub1Dir.exists(), is(true));

        assertThat(sub1.getDirectory(), is(sub1Dir));

        assertThat(svc.getCompletedCount(org), is(3));
        assertThat(svc.getCompletedCount(org, flow1), is(2));
        assertThat(svc.getCompletedCount(org, flow2), is(1));
        assertThat(svc.getCompletedCount(org, flow3), is(0));

        List<Submission> pending = svc.getCompleted(org);
        assertThat(pending, is(hasSize(3)));

        assertThat(pending.get(0).getDirectory(), is(sub1Dir));
    }
}
