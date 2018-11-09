package io.rapidpro.surveyor.activity;

import android.app.FragmentTransaction;
import android.os.Bundle;

import io.rapidpro.surveyor.fragment.SettingsFragment;


/**
 * Activity for modifying app settings
 */
public class SettingsActivity extends BaseActivity {

    public boolean requireLogin() {
        return false;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
