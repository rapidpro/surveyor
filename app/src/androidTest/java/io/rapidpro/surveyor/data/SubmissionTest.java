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
    public void submit() throws IOException, TembaException, InterruptedException {
        installOrg(ORG_UUID, io.rapidpro.surveyor.test.R.raw.org1_details, io.rapidpro.surveyor.test.R.raw.org1_flows, io.rapidpro.surveyor.test.R.raw.org1_assets);

        Org org = getSurveyor().getOrgService().get(ORG_UUID);
        Flow flow1 = org.getFlow("14ca824e-6607-4c11-82f5-18e298d0bd58");

        SubmissionService svc = getSurveyor().getSubmissionService();

        // make a submission that looks like a complete submission of the Multimedia flow
        Submission sub = svc.newSubmission(org, flow1);
        File directory = sub.getDirectory();

        copyResource(R.raw.submissions_multimedia_events, new File(directory, "events.jsonl"));
        copyResource(R.raw.submissions_multimedia_session, new File(directory, "session.json"));
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

        // TODO

        //RecordedRequest request4 = mockServer.takeRequest();
        //assertThat(request4.getRequestLine(), is("POST /mr/upl HTTP/1.1"));

        //String body = request4.getBody().readString(StandardCharsets.UTF_8);
        //assertThat(body, containsString("http://uploads.rapidpro.io/0cce52d1.jpg"));
        //assertThat(body, containsString("http://uploads.rapidpro.io/6c519989.mp4"));
        //assertThat(body, containsString("http://uploads.rapidpro.io/fce55c47.m4a"));
    }
}
