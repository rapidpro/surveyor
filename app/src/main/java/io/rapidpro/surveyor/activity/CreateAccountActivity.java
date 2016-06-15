package io.rapidpro.surveyor.activity;

import android.app.Activity;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.DBOrg;

public class CreateAccountActivity extends BaseActivity {

    public static final String CREATE_ACCOUNT_URL = "/org/surveyor/";

    public boolean validateLogin() { return false; }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_account);

        // load the account creation on to our webview
        WebView web = (WebView) findViewById(R.id.webview);

        final Activity activity = this;
        web.setWebViewClient(new WebViewClient() {

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(activity, getString(R.string.web_load_failure), Toast.LENGTH_SHORT).show();
                activity.finish();
            }

            public void onPageFinished(WebView view, String url) {

                UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(url);
                String user = sanitizer.getValue("user");
                String token = sanitizer.getValue("token");
                String name = sanitizer.getValue("org");

                if (user != null && token != null && name != null) {
                    DBOrg org = new DBOrg();
                    org.setToken(token);
                    org.setName(name);

                    List<DBOrg> orgs = new ArrayList<>();
                    orgs.add(org);
                    login(user, orgs);
                }

                if (url.endsWith(CREATE_ACCOUNT_URL))  {
                    getViewCache().hide(R.id.web_progress);
                }

            }

            public void onLoadResource (WebView view, String url) {
                if (url.endsWith(CREATE_ACCOUNT_URL)) {
                    getViewCache().show(R.id.web_progress);
                }
            }
        });

        web.loadUrl(Surveyor.BASE_URL + CREATE_ACCOUNT_URL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }
    
}
