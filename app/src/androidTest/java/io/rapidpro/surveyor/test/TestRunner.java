package io.rapidpro.surveyor.test;


import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.test.runner.AndroidJUnitRunner;

import io.rapidpro.surveyor.Logger;

/**
 * Custom test runner class which will use a subclass of the regular Surveyor application
 */
public class TestRunner extends AndroidJUnitRunner {

    public static int PAUSE_MILLIS = 1000;

    public void onCreate(Bundle arguments) {
        Logger.d("Creating test runner with arguments: ");
        for (String arg : arguments.keySet()) {
            Logger.d(" - " + arg + ": " + arguments.get(arg));
        }

        String pauseArg = arguments.getString("pause");
        if (pauseArg != null) {
            PAUSE_MILLIS = Integer.parseInt(pauseArg);
        }

        super.onCreate(arguments);
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        return super.newApplication(cl, TestApplication.class.getName(), context);
    }
}
