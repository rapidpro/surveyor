package io.rapidpro.surveyor.data;

import org.apache.commons.io.filefilter.DirectoryFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.util.UUID;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.utils.SurveyUtils;

public class SubmissionService {

    private File rootDir;
    private Logger log;

    /**
     * Creates a new submission service
     * @param rootDir the root directory
     * @param log the logger
     */
    public SubmissionService(File rootDir, Logger log) {
        this.rootDir = rootDir;
        this.log = log;
    }

    /**
     * Creates a new submission for the given flow in the given org
     * @param org the org
     * @param flow the flow
     * @return the new submission
     */
    public Submission newSubmission(Org org, Flow flow) {
        File directory = SurveyUtils.mkdir(rootDir, org.getUuid(), flow.getUuid(), UUID.randomUUID().toString());

        log.d("Creating new submission in " + directory.getPath());

        return new Submission(directory);
    }

    /**
     * Return the count of pending submissions across all flows for the given org
     *
     * @param org the org
     * @return the count of submissions
     */
    public int getPendingCount(Org org) {
        int count = 0;
        for (Flow flow : org.getFlows()) {
            count += getPendingCount(org, flow);
        }
        return count;
    }

    /**
     * Return the count of pending submissions for the given flow in the given org
     *
     * @param org the org
     * @param flow the flow
     * @return the count of submissions
     */
    public int getPendingCount(Org org, Flow flow) {
        File orgDir = new File(rootDir, org.getUuid());
        File flowDir = new File(orgDir, flow.getUuid());
        if (flowDir.exists()) {
            return flowDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY).length;
        }
        return 0;
    }
}