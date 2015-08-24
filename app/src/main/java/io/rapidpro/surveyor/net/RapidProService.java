package io.rapidpro.surveyor.net;

import android.content.Context;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapterFactory;

import java.lang.reflect.Type;
import java.util.List;

import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.Flow;
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

    public void getOrgs(String email, String password, Callback<List<Org>> callback) {
        m_api.getOrgs(email, password, callback);
    }

    public void getFlows(final Callback<FlowList> callback) {
        m_api.getFlows(getToken(), "F", new Callback<FlowList>() {
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

    public void getFlowDefinition(final Flow flow, final Callback<FlowDefinition> callback) {

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

    private RapidProAPI getAPIAccessor() {
        Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getDeclaringClass().equals(RealmObject.class);
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).registerTypeAdapterFactory(new FlowListTypeAdapterFactory()).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Surveyor.BASE_URL)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setConverter(new GsonConverter(gson))
                .build();

        return restAdapter.create(RapidProAPI.class);
    }

    private class FlowTypeAdapterFactory extends CustomizedTypeAdapterFactory<Flow> {
        private FlowTypeAdapterFactory() {
            super(Flow.class);
        }

        @Override protected void beforeWrite(Flow flow, JsonElement json) {}

        @Override protected void afterRead(JsonElement deserialized) {
            JsonObject custom = deserialized.getAsJsonObject();
            JsonArray rulesets = custom.get("rulesets").getAsJsonArray();
            int questionCount = 0;
            for (int i=0; i<rulesets.size(); i++) {
                if ("wait_message".equals(rulesets.get(i).getAsJsonObject().get("ruleset_type").getAsString())) {
                    questionCount++;
                }
            }
            custom.add("questionCount", new JsonPrimitive(questionCount));
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
