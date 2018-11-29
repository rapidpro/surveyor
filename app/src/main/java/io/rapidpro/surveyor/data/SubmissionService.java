package io.rapidpro.surveyor.data;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        File flowDir = SurveyUtils.mkdir(rootDir, org.getUuid(), flow.getUuid());

        // blow away an existing current submission for this flow
        File directory = new File(flowDir, Submission.INCOMPLETE_DIRECTORY_NAME);
        if (directory.exists()) {
            FileUtils.deleteQuietly(directory);
        }

        directory = new File(flowDir, Submission.INCOMPLETE_DIRECTORY_NAME);
        directory.mkdirs();

        log.d("Creating new submission in " + directory.getPath());

        return new Submission(org, directory);
    }

    /**
     * Return the completed submissions across all flows for the given org
     *
     * @param org the org
     * @return the submissions
     */
    public List<Submission> getCompleted(Org org) {
        List<Submission> pending = new ArrayList<>();
        for (Flow flow : org.getFlows()) {
            pending.addAll(getCompleted(org, flow));
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
    public List<Submission> getCompleted(Org org, Flow flow) {
        List<Submission> pending = new ArrayList<>();
        File orgDir = new File(rootDir, org.getUuid());
        File flowDir = new File(orgDir, flow.getUuid());
        if (flowDir.exists()) {
            for (File file : flowDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() && !f.getName().equals(Submission.INCOMPLETE_DIRECTORY_NAME);
                }
            })) {
                pending.add(new Submission(org, file));
            }
        }
        return pending;
    }

    /**
     * Return the count of completed submissions across all flows for the given org
     *
     * @param org the org
     * @return the count of submissions
     */
    public int getCompletedCount(Org org) {
        return getCompleted(org).size();
    }

    /**
     * Return the count of completed submissions for the given flow in the given org
     *
     * @param org  the org
     * @param flow the flow
     * @return the count of submissions
     */
    public int getCompletedCount(Org org, Flow flow) {
        return getCompleted(org, flow).size();
    }
}
