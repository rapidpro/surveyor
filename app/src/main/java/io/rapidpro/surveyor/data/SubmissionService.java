package io.rapidpro.surveyor.data;

import org.apache.commons.io.filefilter.DirectoryFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.utils.SurveyUtils;

/**
 * Directory based service for flow session submissions
 */
public class SubmissionService {

    private File rootDir;
    private Logger log;

    /**
     * Creates a new submission service
     *
     * @param rootDir the root directory
     * @param log     the logger
     */
    public SubmissionService(File rootDir, Logger log) {
        this.rootDir = rootDir;
        this.log = log;

        log.d("SubmissionService created for directory " + this.rootDir.getAbsolutePath());
    }

    /**
     * Creates a new submission for the given flow in the given org
     *
     * @param org  the org
     * @param flow the flow
     * @return the new submission
     */
    public Submission newSubmission(Org org, Flow flow) throws IOException {
        File directory = SurveyUtils.mkdir(rootDir, org.getUuid(), flow.getUuid(), UUID.randomUUID().toString());

        log.d("Creating new submission in " + directory.getPath());

        return new Submission(directory);
    }

    /**
     * Return the pending submissions across all flows for the given org
     *
     * @param org the org
     * @return the submissions
     */
    public List<Submission> getPending(Org org) {
        List<Submission> pending = new ArrayList<>();
        for (Flow flow : org.getFlows()) {
            pending.addAll(getPending(org, flow));
        }
        return pending;
    }

    /**
     * Return the pending submissions for the given flow in the given org
     *
     * @param org  the org
     * @param flow the flow
     * @return the submissions
     */
    public List<Submission> getPending(Org org, Flow flow) {
        List<Submission> pending = new ArrayList<>();
        File orgDir = new File(rootDir, org.getUuid());
        File flowDir = new File(orgDir, flow.getUuid());
        if (flowDir.exists()) {
            for (File file : flowDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)) {
                pending.add(new Submission(flowDir));
            }
        }
        return pending;
    }

    /**
     * Return the count of pending submissions across all flows for the given org
     *
     * @param org the org
     * @return the count of submissions
     */
    public int getPendingCount(Org org) {
        return getPending(org).size();
    }

    /**
     * Return the count of pending submissions for the given flow in the given org
     *
     * @param org  the org
     * @param flow the flow
     * @return the count of submissions
     */
    public int getPendingCount(Org org, Flow flow) {
        return getPending(org, flow).size();
    }
}
