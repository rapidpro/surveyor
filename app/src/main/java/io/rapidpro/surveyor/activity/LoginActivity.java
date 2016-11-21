package io.rapidpro.surveyor.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.greysonparrelli.permiso.Permiso;

import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.net.APIError;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends BaseActivity {

    // UI references.
    private AutoCompleteTextView m_emailView;
    private EditText m_passwordView;
    private View m_progressView;
    private View m_loginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (!resultSet.areAllPermissionsGranted()) {
                    finish();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                LoginActivity.this.showRationaleDialog(R.string.permission_storage, callback);
            }
        }, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);


        overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);

        setContentView(R.layout.activity_login);

        // Set up the login form.
        m_emailView = (AutoCompleteTextView) findViewById(R.id.email);

        // prepoulate with our previous username if we have one
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString(SurveyorIntent.PREF_USERNAME, "");
        m_emailView.setText(username);

        m_passwordView = (EditText) findViewById(R.id.password);
        m_passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button emailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        emailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        m_loginFormView = findViewById(R.id.login_form);
        m_progressView = findViewById(R.id.login_progress);

        // set our error message if we have one
        setErrorMessage(getIntent().getStringExtra(SurveyorIntent.EXTRA_ERROR));

    }

    public void onResume() {
        super.onResume();
    }

    public boolean validateLogin() {
        return false;
    }

    private void setErrorMessage(String message) {

        TextView errorBox  = (TextView) findViewById(R.id.text_error_message);
        if (message != null) {
            errorBox.setVisibility(View.VISIBLE);
            errorBox.setText(message);
        } else {
            errorBox.setVisibility(View.GONE);
        }
    }

    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {

        // Reset errors.
        m_emailView.setError(null);
        m_passwordView.setError(null);

        // Store values at the time of the login attempt.
        final String email = m_emailView.getText().toString();
        String password = m_passwordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            m_passwordView.setError(getString(R.string.error_invalid_password));
            focusView = m_passwordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            m_emailView.setError(getString(R.string.error_field_required));
            focusView = m_emailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            m_emailView.setError(getString(R.string.error_invalid_email));
            focusView = m_emailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            getRapidProService().getOrgs(email, password, new Callback<List<DBOrg>>() {
                @Override
                public void onResponse(Call<List<DBOrg>> call, Response<List<DBOrg>> response) {

                    if (response.isSuccessful()) {
                        List<DBOrg> orgs = response.body();
                        login(email, orgs);
                    } else {
                        APIError error = getRapidProService().parseError(response);
                        int status = error.getStatus();
                        if (status == 404) {
                            setErrorMessage(getString(R.string.error_server_not_found));
                        } else if (status == 500) {
                            setErrorMessage(getString(R.string.error_server_failure));
                        } else if (status == 403) {
                            setErrorMessage(getString(R.string.error_invalid_login));
                        } else {
                            setErrorMessage(getString(R.string.error_network));
                        }
                        showProgress(false);
                    }
                }

                @Override
                public void onFailure(Call<List<DBOrg>> call, Throwable t) {
                    Surveyor.LOG.e("Failure logging in", t);
                    setErrorMessage(getString(R.string.error_network));
                    showProgress(false);
                }
            });
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            m_loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            m_loginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    m_loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            m_progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            m_progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    m_progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            m_progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            m_loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * The user clicked on the link to create a new account,
     * launch our CreateAccountActivity
     **/
    public void onCreateAccount(View view) {
        startActivity(new Intent(this, CreateAccountActivity.class));
        finish();
    }
}