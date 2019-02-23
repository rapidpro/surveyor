package io.rapidpro.surveyor.utils;

import java.io.File;
import java.io.IOException;

/**
 * Misc utils
 */
public class SurveyUtils {
    /**
     * Creates a nested directory
     *
     * @param root    the root directory
     * @param folders the nested directory names
     * @return the directory
     * @throws IOException if any directory couldn't be created
     */
    public static File mkdir(File root, String... folders) throws IOException {
        File current = root;
        for (String folder : folders) {
            current = new File(current, folder);

            if (!current.exists() && !current.mkdirs()) {
                throw new IOException("Unable to create directory: " + current.getAbsolutePath());
            }
        }
        return current;
    }
}
