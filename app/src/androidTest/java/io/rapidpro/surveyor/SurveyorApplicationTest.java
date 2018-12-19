package io.rapidpro.surveyor;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import io.rapidpro.surveyor.legacy.Legacy;
import io.rapidpro.surveyor.test.BaseApplicationTest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SurveyorApplicationTest extends BaseApplicationTest {

    @Test
    public void getUriFromFile() throws IOException {
        SurveyorApplication app = getSurveyor();

        assertThat(getSurveyor().getUriForFile(new File(app.getUserDirectory(), "test.x")).toString(), is("content://io.rapidpro.surveyor.provider/shared/test.x"));
        assertThat(getSurveyor().getUriForFile(new File(app.getSubmissionsDirectory(), "test.x")).toString(), is("content://io.rapidpro.surveyor.provider/shared/test_submissions/test.x"));
        assertThat(getSurveyor().getUriForFile(new File(app.getExternalCacheDir(), "test.x")).toString(), is("content://io.rapidpro.surveyor.provider/cache/test.x"));
        assertThat(getSurveyor().getUriForFile(new File(Legacy.getSubmissionsDirectory(), "test.x")).toString(), is("content://io.rapidpro.surveyor.provider/external_files/Surveyor/submissions/test.x"));
    }
}
