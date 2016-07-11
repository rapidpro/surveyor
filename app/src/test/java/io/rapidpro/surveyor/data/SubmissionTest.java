package io.rapidpro.surveyor.data;

import junit.framework.TestCase;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class SubmissionTest extends TestCase {

    private File m_baseDirectory;

    public void setUp() throws Exception {
        super.setUp();
        m_baseDirectory = new File("test_data", "submissions");
    }

    public void tearDown() throws Exception {

    }

    @Test
    public void testSubmitVersion8() throws IOException {

        // try reading in a version 8 format submission
        File flowFile = new File(m_baseDirectory, "6/adbd4e14-f294-4c80-ba75-edeaea3f420d/40_cf9b5e5c-814d-458c-bace-11127eeae30c.json");
        Submission submission = Submission.load("chancellor", flowFile);
        assertNotNull(submission);

        // our messages should parse as expected
        String msg = submission.m_steps.get(0).getActions().get(0).toJson().getAsJsonObject().get("msg").getAsString();
        assertEquals("where are you", msg);
        assertEquals("-2.8904099,-79.0045795", submission.m_steps.get(1).getRuleResult().getValue());

    }
}