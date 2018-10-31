package io.rapidpro.surveyor.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.net.responses.Definitions;
import io.rapidpro.surveyor.net.responses.FieldPage;
import io.rapidpro.surveyor.net.responses.FlowPage;
import io.rapidpro.surveyor.net.responses.LocationResultPage;
import io.rapidpro.surveyor.net.responses.TokenResults;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.mock.BehaviorDelegate;

public class MockTembaAPI implements TembaAPI {

    private final BehaviorDelegate<TembaAPI> delegate;

    public MockTembaAPI(BehaviorDelegate<TembaAPI> service) {
        this.delegate = service;
    }

    @Override
    public Call<TokenResults> getTokens(String username, String password, String role) {
        return null;
    }

    @Override
    public Call<DBOrg> getOrg(String token) {
        DBOrg org = new DBOrg();
        org.setName("Test Org");
        org.setPrimaryLanguage("eng");
        org.setCountry("RW");
        return delegate.returningResponse(org).getOrg(token);
    }

    @Override
    public Call<FlowPage> getFlows(String token, String type, boolean archived) {
        return null;
    }

    @Override
    public Call<Definitions> getFlowDefinition(String token, String uuid) {
        return null;
    }

    @Override
    public Call<JsonObject> addResults(String token, JsonElement submissionJson) {
        return null;
    }

    @Override
    public Call<JsonObject> addContact(String token, JsonElement contact) {
        return null;
    }

    @Override
    public Call<LocationResultPage> getLocationPage(String token, boolean aliases, int page) {
        return null;
    }

    @Override
    public Call<FieldPage> getFieldPage(String token, String cursor) {
        FieldPage page = new FieldPage();

        // first page
        if (StringUtils.isEmpty(cursor)) {
            List<JsonElement> fields = Arrays.<JsonElement>asList(
                    JsonUtils.object("key", "gender", "label", "Gender", "value_type", "text"),
                    JsonUtils.object("key", "age", "label", "Age", "value_type", "numeric")
            );
            page.setResults(fields);
            page.setNext("http://example.com/api/v2/flows.json?cursor=1234");

            // second page
        } else if (cursor.equals("1234")) {
            List<JsonElement> fields = Collections.singletonList(
                    (JsonElement) JsonUtils.object("key", "join_date", "label", "Join Date", "value_type", "datetime")
            );
            page.setResults(fields);
            page.setNext(null);
        } else {
            page.setResults(Collections.<JsonElement>emptyList());
        }

        return delegate.returningResponse(page).getFieldPage(token, cursor);
    }

    @Override
    public Call<JsonObject> uploadMedia(String token, Map<String, RequestBody> params) {
        return null;
    }
}
