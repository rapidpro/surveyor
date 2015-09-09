package io.rapidpro.surveyor.net;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

import io.rapidpro.flows.definition.Flow;
import io.rapidpro.flows.definition.GroupRef;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.adapter.FlowListAdapter;
import io.rapidpro.surveyor.data.DBFlow;
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
        m_api.getOrgs(email, password, callback);
    }

    public void getOrg(Callback<DBOrg> callback) {
        m_api.getOrg(getToken(), callback);
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

    public void addContact(final Submission.Contact contact, final Submission.ContactAddListener onContactAddListener) {

        Surveyor.LOG.d("Adding contact: " + contact);
        m_api.addContact(getToken(), contact, new Callback<Submission.Contact>() {
            @Override
            public void success(Submission.Contact posted, Response response) {
                contact.setUuid(posted.getUuid());
                if (onContactAddListener != null) {
                    onContactAddListener.onContactAdded();
                }

            }

            @Override
            public void failure(RetrofitError error) {
                Surveyor.LOG.e("Failed to add contact", error);
            }
        });
    }

    public void addResults(final Submission submission, final Submission.OnSubmitListener onSubmitListener) {
        m_api.addResults(getToken(), submission, new Callback<Void>() {
            @Override
            public void success(Void aVoid, Response response) {
                Surveyor.LOG.d("Success!");

                // remove our submission
                submission.delete();

                if (onSubmitListener != null) {
                    onSubmitListener.onSuccess();
                }


            }

            @Override
            public void failure(RetrofitError error) {
                Surveyor.LOG.d("Failure");
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
        }).registerTypeAdapterFactory(new FlowListTypeAdapterFactory())
                .registerTypeAdapter(Submission.Contact.class, new Submission.Contact.Serializer()).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Surveyor.BASE_URL)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setConverter(new GsonConverter(gson))
                .build();

        return restAdapter.create(RapidProAPI.class);
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
