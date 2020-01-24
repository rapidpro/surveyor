package io.rapidpro.surveyor.data;

import org.apache.commons.io.FileUtils;
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

    private static FileFilter DIR_FILTER = DirectoryFileFilter.INSTANCE;

    /**
     * Creates a new submission service
     *
     * @param rootDir the root directory
     */
    public SubmissionService(File rootDir) {
        this.rootDir = rootDir;

        Logger.d("SubmissionService created for directory " + this.rootDir.getAbsolutePath());
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

        // blow away any existing incomplete submissions for this flow
        discardIncomplete(org, flow);

        File directory = new File(flowDir, UUID.randomUUID().toString());
        directory.mkdirs();

        Logger.d("Creating new submission in " + directory.getPath());

        return new Submission(org, directory);
    }

    private void discardIncomplete(Org org, Flow flow) throws IOException {
        for (Submission sub : getAll(org, flow)) {
            if (!sub.isCompleted()) {
                FileUtils.deleteDirectory(sub.getDirectory());
            }
        }
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

    private List<Submission> getAll(Org org, Flow flow) {
        List<Submission> all = new ArrayList<>();
        File orgDir = new File(rootDir, org.getUuid());
        File flowDir = new File(orgDir, flow.getUuid());
        if (flowDir.exists()) {
            for (File file : flowDir.listFiles(DIR_FILTER)) {
                all.add(new Submission(org, file));
            }
        }
        return all;
    }

    public boolean hasSubmissions() {
        for (File orgDir : rootDir.listFiles(DIR_FILTER)) {
            for (File flowDir : orgDir.listFiles(DIR_FILTER)) {
                File[] subDirs = flowDir.listFiles(DIR_FILTER);
                if (subDirs != null && subDirs.length > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the pending submissions for the given flow in the given org
     *
     * @param org  the org
     * @param flow the flow
     * @return the submissions
     */
    public List<Submission> getCompleted(Org org, Flow flow) {
        List<Submission> completed = new ArrayList<>();
        for (Submission sub : getAll(org, flow)) {
            if (sub.isCompleted()) {
                completed.add(sub);
            }
        }
        return completed;
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

    /**
     * Clear all submissions
     */
    public void clearAll() throws IOException {
        FileUtils.deleteDirectory(rootDir);
        rootDir.mkdir();
    }
}
