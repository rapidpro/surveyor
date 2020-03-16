package io.rapidpro.surveyor.data;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import io.rapidpro.surveyor.utils.RawJson;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FlowTest {

    @Test
    public void extractV13() throws IOException {
        File flow = new File(FlowTest.class.getClassLoader().getResource("flows/two_questions_v13.json").getPath());
        String definition = FileUtils.readFileToString(flow);

        Flow summary = Flow.extract(new RawJson(definition));

        assertThat(summary.getUuid(), is("bdd61538-5f50-4836-a8fb-acaafd64ddb1"));
        assertThat(summary.getName(), is("Two Questions"));
        assertThat(summary.getSpecVersion(), is("13.0.0"));
        assertThat(summary.getRevision(), is(24));
        assertThat(summary.getQuestionCount(), is(2));
    }

    @Test
    public void extractV11() throws IOException {
        File flow = new File(FlowTest.class.getClassLoader().getResource("flows/two_questions_v11.json").getPath());
        String definition = FileUtils.readFileToString(flow);

        Flow summary = Flow.extract(new RawJson(definition));

        assertThat(summary.getUuid(), is("bdd61538-5f50-4836-a8fb-acaafd64ddb1"));
        assertThat(summary.getName(), is("Two Questions"));
        assertThat(summary.getSpecVersion(), is("11.6"));
        assertThat(summary.getRevision(), is(24));
        assertThat(summary.getQuestionCount(), is(2));
    }
}
