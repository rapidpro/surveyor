package io.rapidpro.surveyor.net;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBLocation;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.data.Submission;
import io.realm.Realm;
import io.realm.RealmObject;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

public class RapidProService {

    private RapidProAPI m_api;
    private String m_token;
    private FlowList m_flowList;

    public RapidProService() {
        m_api = getAPIAccessor();
    }

    public FlowList getLastFlows() { return m_flowList; }

    public void setToken(String token) {
        m_token = "Token " + token;
    }

    public String getToken() {
        return m_token;
    }

    public void getOrgs(String email, String password, Callback<List<DBOrg>> callback) {
        m_api.getOrgs(email, password, "S", callback);
    }

    public DBOrg getOrg() {
        return m_api.getOrg(getToken());
    }

    public void getFlows(final Callback<FlowList> callback) {
        m_api.getFlows(getToken(), "S", new Callback<FlowList>() {
            @Override
            public void success(FlowList flowList, Response response) {
                m_flowList = flowList;
                callback.success(flowList, response);
            }

            @Override
            public void failure(RetrofitError error) {
                callback.failure(error);
            }
        });
    }

    public void getFlowDefinition(final DBFlow flow, final Callback<FlowDefinition> callback) {

        final Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        flow.setFetching(true);
        realm.commitTransaction();

        m_api.getFlowDefinition(getToken(), flow.getUuid(), new Callback<FlowDefinition>() {
            @Override
            public void success(FlowDefinition flowDefinition, Response response) {

                realm.beginTransaction();
                flow.setFetching(false);
                realm.commitTransaction();
                realm.close();

                callback.success(flowDefinition, response);
            }

            @Override
            public void failure(RetrofitError error) {

                realm.beginTransaction();
                flow.setFetching(false);
                realm.commitTransaction();
                realm.close();

                callback.failure(error);
            }
        });
    }

    public Contact addContact(final Contact contact) {

        if ("base".equals(contact.getLanguage())) {
            contact.setLanguage(null);
        }

        Contact result =  m_api.addContact(getToken(), contact);
        contact.setUuid(result.getUuid());
        return contact;
    }

    public void addResults(final Submission submission) {
        try {
            m_api.addResults(getToken(), submission);
            submission.delete();
        } catch (RetrofitError e) {
            Surveyor.LOG.e("Failed submitting results", e);
        }
    }

    public List<DBLocation> getLocations() {

        List<DBLocation> locations = new ArrayList<>();

        int pageNumber = 1;
        // fetch our first page
        LocationResultPage page = m_api.getLocationPage(getToken(), true, pageNumber);
        locations.addAll(page.results);

        // fetch subsequent pages until we are done
        while (page != null && page.next != null && page.next.trim().length() != 0) {
            page = m_api.getLocationPage(getToken(), true, ++pageNumber);
            locations.addAll(page.results);
        }

        return locations;
    }

    private RapidProAPI getAPIAccessor() {

        Gson gson = JsonUtils.getGsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getDeclaringClass().equals(RealmObject.class);
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).registerTypeAdapterFactory(new FlowListTypeAdapterFactory())
                .registerTypeAdapter(Contact.class, new Submission.ContactSerializer()).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Surveyor.BASE_URL)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setConverter(new GsonConverter(gson))
                .build();

        return restAdapter.create(RapidProAPI.class);
    }

    public int getErrorMessage(RetrofitError e) {
        int status = e.getResponse().getStatus();
        if (status == 404 || status == 502) {
            return R.string.error_server_not_found;
        }
        else {
            return R.string.error_server_failure;
        }
    }


    private class FlowListTypeAdapterFactory extends CustomizedTypeAdapterFactory<FlowList> {
        private FlowListTypeAdapterFactory() {
            super(FlowList.class);
        }

        @Override protected void beforeWrite(FlowList flow, JsonElement json) {}

        @Override protected void afterRead(JsonElement deserialized) {
            JsonObject custom = deserialized.getAsJsonObject();
            JsonArray flows = custom.get("results").getAsJsonArray();
            for (int i=0; i<flows.size(); i++) {
                int questionCount = 0;
                JsonObject flow = flows.get(i).getAsJsonObject();
                JsonArray rulesets = flow.get("rulesets").getAsJsonArray();
                for (int j=0; j<rulesets.size(); j++) {
                    if ("wait_message".equals(rulesets.get(j).getAsJsonObject().get("ruleset_type").getAsString())) {
                        questionCount++;
                    }
                }

                flow.add("questionCount", new JsonPrimitive(questionCount));
                if (questionCount == 0) {
                    flows.remove(i);
                    i--;
                }
            }
        }
    }
}
