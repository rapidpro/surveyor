package io.rapidpro.surveyor.data;

import java.io.File;
import java.util.UUID;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.utils.SurveyUtils;

public class SubmissionService {

    private File rootDir;
    private Logger log;

    public SubmissionService(File rootDir, Logger log) {
        this.rootDir = rootDir;
        this.log = log;
    }

    public Submission newSubmission(Org org, Flow flow) {
        File directory = SurveyUtils.mkdir(rootDir, org.getUuid(), flow.getUuid(), UUID.randomUUID().toString());
        return new Submission(directory);
    }
}
