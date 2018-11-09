package io.rapidpro.surveyor.test;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.rapidpro.surveyor.net.TembaAPI;
import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.net.responses.Org;
import io.rapidpro.surveyor.net.responses.PaginatedResults;
import io.rapidpro.surveyor.net.responses.Token;
import io.rapidpro.surveyor.net.responses.TokenResults;
import retrofit2.Call;
import retrofit2.mock.BehaviorDelegate;

/**
 * Mocked version of the Temba API which returns test data
 */
public class MockTembaAPI implements TembaAPI {

    private final BehaviorDelegate<TembaAPI> delegate;

    public MockTembaAPI(BehaviorDelegate<TembaAPI> service) {
        this.delegate = service;
    }

    @Override
    public Call<TokenResults> authenticate(String username, String password, String role) {
        TokenResults results = new TokenResults();
        results.setTokens(Arrays.asList(
                new Token("abc123", new Token.OrgReference("3252-3453", "Nyaruka")),
                new Token("cde456", new Token.OrgReference("7644-1245", "UNICEF"))
        ));

        return delegate.returningResponse(results).authenticate(username, password, role);
    }

    @Override
    public Call<Org> getOrg(String token) {
        Org org = new Org();
        org.setUuid("b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06");
        org.setName("Nyaruka");
        org.setPrimaryLanguage("eng");
        org.setLanguages(new String[]{"eng", "fra"});
        org.setTimezone("Africa/Kigali");
        org.setCountry("RW");
        return delegate.returningResponse(org).getOrg(token);
    }

    @Override
    public Call<PaginatedResults<Field>> getFields(String token, String cursor) {
        PaginatedResults<Field> page = new PaginatedResults<>();

        // first page
        if (TextUtils.isEmpty(cursor)) {
            List<Field> results = Arrays.asList(
                    new Field("gender", "Gender", "text"),
                    new Field("age", "Age", "numeric")
            );
            page.setResults(results);
            page.setNext("http://example.com/api/v2/flows.json?cursor=1234");

            // second page
        } else if (cursor.equals("1234")) {
            List<Field> results = Collections.singletonList(
                    new Field("join_date", "Join Date", "datetime")
            );
            page.setResults(results);
            page.setNext(null);
        } else {
            page.setResults(Collections.<Field>emptyList());
        }

        return delegate.returningResponse(page).getFields(token, cursor);
    }

    @Override
    public Call<PaginatedResults<Group>> getGroups(String token, String cursor) {
        PaginatedResults<Group> page = new PaginatedResults<>();

        // first page
        if (TextUtils.isEmpty(cursor)) {
            List<Group> results = Arrays.asList(
                    new Group("b2ad9e4d-71f1-4d54-8dd6-f7a94b685d06", "Testers", ""),
                    new Group("372aba66-16e2-44ee-8486-fb5cedfe51d9", "Customers", "")
            );
            page.setResults(results);
            page.setNext("http://example.com/api/v2/flows.json?cursor=1234");

            // second page
        } else if (cursor.equals("1234")) {
            List<Group> results = Collections.singletonList(
                    new Group("63867d07-c033-4ef1-957c-85fa9708c19c", "Youth", "age <= 18")
            );
            page.setResults(results);
            page.setNext(null);
        } else {
            page.setResults(Collections.<Group>emptyList());
        }

        return delegate.returningResponse(page).getGroups(token, cursor);
    }
}
