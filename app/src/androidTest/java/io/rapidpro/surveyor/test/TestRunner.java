package io.rapidpro.surveyor.test;


import android.app.Application;
import android.content.Context;

import androidx.test.runner.AndroidJUnitRunner;

/**
 * Custom test runner class which will use a subclass of the regular Surveyor application
 */
public class TestRunner extends AndroidJUnitRunner {
    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        return super.newApplication(cl, TestApplication.class.getName(), context);
    }
}
