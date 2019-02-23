package io.rapidpro.surveyor.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.greysonparrelli.permiso.Permiso;

import java.util.List;
import java.util.Set;

import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.SurveyorIntent;
import io.rapidpro.surveyor.SurveyorPreferences;
import io.rapidpro.surveyor.net.TembaService;
import io.rapidpro.surveyor.net.responses.Token;
import io.rapidpro.surveyor.net.responses.TokenResults;
import io.rapidpro.surveyor.task.FetchOrgsTask;
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

        // usually new activities come in from the right, but make this one opposite as we're going
        // "back" to a clean activity stack
        overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);

        setContentView(R.layout.activity_login);

        m_emailView = findViewById(R.id.email);

        // prepopulate with our previous username if we have one
        m_emailView.setText(getPreferences().getString(SurveyorPreferences.PREV_USERNAME, ""));

        m_passwordView = findViewById(R.id.password);
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

        Button emailSignInButton = findViewById(R.id.email_sign_in_button);
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

    public boolean requireLogin() {
        return false;
    }

    private void setErrorMessage(String message) {
        TextView errorBox = findViewById(R.id.text_error_message);
        if (message != null) {
            errorBox.setVisibility(View.VISIBLE);
            errorBox.setText(message);
        } else {
            errorBox.setVisibility(View.GONE);
        }
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
        final String password = m_passwordView.getText().toString();

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

            TembaService svc = getSurveyor().getTembaService();
            svc.authenticate(email, password, new Callback<TokenResults>() {
                @Override
                public void onResponse(Call<TokenResults> call, Response<TokenResults> response) {

                    if (response.isSuccessful()) {
                        List<Token> tokens = response.body().getTokens();

                        Logger.d("Authentication returned " + tokens.size() + " tokens");

                        fetchOrgsAndLogin(email, tokens);

                    } else {
                        switch (response.code()) {
                            case 403:
                                setErrorMessage(getString(R.string.error_invalid_login));
                                break;
                            case 404:
                                setErrorMessage(getString(R.string.error_server_not_found));
                                break;
                            case 500:
                                setErrorMessage(getString(R.string.error_server_failure));
                                break;
                            default:
                                setErrorMessage(getString(R.string.error_network));
                        }
                        showProgress(false);
                    }
                }

                @Override
                public void onFailure(Call<TokenResults> call, Throwable t) {
                    Logger.e("Failure logging in", t);
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
        return password.length() >= 8;
    }

    protected void fetchOrgsAndLogin(final String email, final List<Token> tokens) {
        new FetchOrgsTask(new FetchOrgsTask.Listener() {
            @Override
            public void onComplete(Set<String> orgUUIDs) {
                login(email, orgUUIDs);
            }

            @Override
            public void onFailure() {
                setErrorMessage(getString(R.string.error_fetching_org));
                showProgress(false);
            }
        }).execute(tokens.toArray(new Token[0]));
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
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