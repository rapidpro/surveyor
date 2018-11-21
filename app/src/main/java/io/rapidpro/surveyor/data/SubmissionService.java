package io.rapidpro.surveyor.data;

import java.io.File;

import io.rapidpro.surveyor.Logger;

public class SubmissionService {

    private static final String MEDIA_DIR = "Media";
    private static final String SUBMISSIONS_DIR = "Submissions";

    private File rootDir;
    private Logger log;

    public SubmissionService(File rootDir, Logger log) {
        this.rootDir = rootDir;
        this.log = log;
    }

    public File createMediaFile(String extension) {
        //File dir = new File(rootDir, MEDIA_DIR);
        //dir.mkdirs();
        //return dir;
        return null;
    }

    private File getMediaDir() {
        File dir = new File(rootDir, MEDIA_DIR);
        dir.mkdirs();
        return dir;
    }
}
