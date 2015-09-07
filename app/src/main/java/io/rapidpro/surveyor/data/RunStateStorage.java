package io.rapidpro.surveyor.data;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.runner.RunState;
import io.rapidpro.surveyor.RunnerUtil;
import io.rapidpro.surveyor.Surveyor;

public class RunStateStorage {

    private static final String RUNS_DIR = "runs";

    /**
     * Get the number of pending submissions for this flow
     */
    public static int getPendingSubmissions(DBFlow flow) {
        return getFlowDir(flow).list().length;
    }

    /**
     * Save the run state to disk
     */
    public static void saveRunState(File file, RunState runState, DBFlow flow) {

        RunState previousState = getRunState(file, RunnerUtil.createFlow(flow));
        if (previousState != null) {
            previousState.getSteps().addAll(runState.getCompletedSteps());
            writeFile(file, previousState.toJson());
        } else {
            writeFile(file, runState.toJson());
        }
    }

    /**
     * Create a file for a given run. This ensures that the file is unique
     */
    public static File createRunFile(DBFlow flow, DBContact contact) {

        String uuid = contact.getUuid();

        // get a unique filename
        File file =  new File(getFlowDir(flow), uuid + "_1.json");
        int count = 2;
        while (file.exists()) {
            file =  new File(getFlowDir(flow), uuid + "_" + count + ".json");
            count++;
        }

        return file;
    }

    private static void writeFile(File file, String contents) {
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(contents.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Surveyor.LOG.e("Failed writing file " + file.getName(), e);
        }
    }

    private static File getFlowDir(DBFlow flow) {
        File flowDir = new File(getRunsDir(), flow.getUuid());
        flowDir.mkdirs();
        return flowDir;
    }

    private static File getRunsDir() {
        File runsDir = new File(Surveyor.get().getFilesDir(), RUNS_DIR);
        runsDir.mkdirs();
        return runsDir;
    }

    public static RunState getRunState(File runFile, Flow flow) {
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(runFile)));
            String inputString;
            StringBuffer sb = new StringBuffer();
            while ((inputString = inputReader.readLine()) != null) {
                sb.append(inputString + "\n");
            }
            inputReader.close();
            return RunState.fromJson(sb.toString(), flow);
        } catch (IOException e) {
            Surveyor.LOG.d(e.getMessage());
        }
        return null;
    }

    public static void clear() {
        FileUtils.deleteQuietly(getRunsDir());
    }
}
