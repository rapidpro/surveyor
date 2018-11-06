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
import io.rapidpro.surveyor.net.responses.Location;
import io.rapidpro.surveyor.net.responses.LocationPage;
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
    public Call<LocationPage> getLocationPage(String token, String cursor) {
        LocationPage page = new LocationPage();

        Location ecuador = new Location("123", "Ecuador", 0, null, Collections.singletonList("Ecuator"));
        Location azuay = new Location("2535", "Azuay", 1, ecuador.toReference(), null);
        Location cuenca = new Location("35355", "Cuenca", 1, azuay.toReference(), null);

        // first page
        if (StringUtils.isEmpty(cursor)) {
            page.setResults(Arrays.asList(ecuador, azuay));
            page.setNext("http://example.com/api/v2/boundaries.json?cursor=1234");

            // second page
        } else if (cursor.equals("1234")) {
            page.setResults(Collections.singletonList(cuenca));
            page.setNext(null);
        } else {
            page.setResults(Collections.<Location>emptyList());
        }

        return delegate.returningResponse(page).getLocationPage(token, cursor);
    }

    @Override
    public Call<FieldPage> getFieldPage(String token, String cursor) {
        FieldPage page = new FieldPage();

        // first page
        if (StringUtils.isEmpty(cursor)) {
            List<JsonObject> fields = Arrays.asList(
                    JsonUtils.object("key", "gender", "label", "Gender", "value_type", "text"),
                    JsonUtils.object("key", "age", "label", "Age", "value_type", "numeric")
            );
            page.setResults(fields);
            page.setNext("http://example.com/api/v2/flows.json?cursor=1234");

            // second page
        } else if (cursor.equals("1234")) {
            List<JsonObject> fields = Collections.singletonList(
                    JsonUtils.object("key", "join_date", "label", "Join Date", "value_type", "datetime")
            );
            page.setResults(fields);
            page.setNext(null);
        } else {
            page.setResults(Collections.<JsonObject>emptyList());
        }

        return delegate.returningResponse(page).getFieldPage(token, cursor);
    }

    @Override
    public Call<JsonObject> uploadMedia(String token, Map<String, RequestBody> params) {
        return null;
    }
}
