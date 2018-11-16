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
    public void extract() throws IOException {
        File flow = new File(FlowTest.class.getClassLoader().getResource("flows/two_questions.json").getPath());
        String definition = FileUtils.readFileToString(flow);

        Flow summary = Flow.extract(new RawJson(definition));

        assertThat(summary.getUuid(), is("14ca824e-6607-4c11-82f5-18e298d0bd58"));
        assertThat(summary.getName(), is("Two Questions"));
        assertThat(summary.getRevision(), is(24));
        assertThat(summary.getQuestionCount(), is(2));
    }
}
