package io.rapidpro.surveyor.data;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.rapidpro.surveyor.net.TembaException;
import io.rapidpro.surveyor.test.BaseApplicationTest;
import io.rapidpro.surveyor.test.R;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class SubmissionTest extends BaseApplicationTest {
    private static final String ORG_UUID = "b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06";

    @Test
    public void multimedia() throws IOException, TembaException, InterruptedException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        Flow flow = org.getFlow("bdd61538-5f50-4836-a8fb-acaafd64ddb1");

        SubmissionService svc = getSurveyor().getSubmissionService();

        // make a submission that looks like a complete submission of the Multimedia flow
        Submission sub = svc.newSubmission(org, flow);
        File directory = sub.getDirectory();

        copyResource(R.raw.submission2_events, new File(directory, "events.jsonl"));
        copyResource(R.raw.submission2_modifiers, new File(directory, "modifiers.jsonl"));
        copyResource(R.raw.submission2_session, new File(directory, "session.json"));
        copyResource(R.raw.capture_image, new File(sub.getMediaDirectory(), "0cce52d1-ff59-4074-bf54-39643900e2bf.jpg"));
        copyResource(R.raw.capture_video, new File(sub.getMediaDirectory(), "6c519989-179c-4bab-bc6b-469ea6c909d6.mp4"));
        copyResource(R.raw.capture_audio, new File(sub.getMediaDirectory(), "fce55c47-8a2e-4502-999e-b0b79117b679.m4a"));
        sub.complete();

        mockServerResponse("{\"location\":\"http://uploads.rapidpro.io/0cce52d1.jpg\"}", "application/json", 200);
        mockServerResponse("{\"location\":\"http://uploads.rapidpro.io/6c519989.mp4\"}", "application/json", 200);
        mockServerResponse("{\"location\":\"http://uploads.rapidpro.io/fce55c47.m4a\"}", "application/json", 200);
        mockServerResponse("{\"msg\":\"thanks\"}", "application/json", 200);

        sub.submit();

        assertThat(sub.getDirectory(), is(nullValue()));
        assertThat(directory.exists(), is(false));

        // we have 3 posts to upload media
        RecordedRequest request1 = mockServer.takeRequest();
        assertThat(request1.getRequestLine(), is("POST /api/v2/media.json HTTP/1.1"));
        RecordedRequest request2 = mockServer.takeRequest();
        assertThat(request2.getRequestLine(), is("POST /api/v2/media.json HTTP/1.1"));
        RecordedRequest request3 = mockServer.takeRequest();
        assertThat(request3.getRequestLine(), is("POST /api/v2/media.json HTTP/1.1"));

        RecordedRequest request4 = mockServer.takeRequest();
        assertThat(request4.getRequestLine(), is("POST /mr/session/submit.json HTTP/1.1"));

        // TODO fix resolving to wrong path
        //String body = request4.getBody().readString(StandardCharsets.UTF_8);
        //assertThat(body, containsString("http://uploads.rapidpro.io/0cce52d1.jpg"));
        //assertThat(body, containsString("http://uploads.rapidpro.io/6c519989.mp4"));
        //assertThat(body, containsString("http://uploads.rapidpro.io/fce55c47.m4a"));
    }

    @Test
    public void contactDetails() throws IOException, TembaException, InterruptedException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        Flow flow1 = org.getFlow("ed8cf8d4-a42c-4ce1-a7e3-44a2918e3cec");

        SubmissionService svc = getSurveyor().getSubmissionService();

        Submission sub = svc.newSubmission(org, flow1);
        File directory = sub.getDirectory();

        copyResource(R.raw.submission3_events, new File(directory, "events.jsonl"));
        copyResource(R.raw.submission3_modifiers, new File(directory, "modifiers.jsonl"));
        copyResource(R.raw.submission3_session, new File(directory, "session.json"));
        sub.complete();

        mockServerResponse("{\"msg\":\"thanks\"}", "application/json", 200);

        sub.submit();

        assertThat(sub.getDirectory(), is(nullValue()));
        assertThat(directory.exists(), is(false));

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getRequestLine(), is("POST /mr/session/submit.json HTTP/1.1"));
    }
}
