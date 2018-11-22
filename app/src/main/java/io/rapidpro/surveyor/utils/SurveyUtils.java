package io.rapidpro.surveyor.utils;

import java.io.File;

public class SurveyUtils {
    public static File mkdir(File root, String... folders) {
        File current = root;
        for (String folder : folders) {
            current = new File(current, folder);
            current.mkdirs();
        }
        return current;
    }
}
