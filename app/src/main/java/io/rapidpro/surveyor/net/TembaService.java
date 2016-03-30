package io.rapidpro.surveyor.net;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.rapidpro.flows.runner.Contact;
import io.rapidpro.flows.runner.Field;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.TembaException;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBLocation;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.data.Submission;
import io.realm.Realm;
import io.realm.RealmObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TembaService {

    private TembaAPI m_api;
    private Retrofit m_retrofit;
    private String m_token;
    private FlowList m_flowList;

    public TembaService() {
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
        m_api.getOrgs(email, password, "S").enqueue(callback);
    }

    public DBOrg getOrg() {
        try {
            return m_api.getOrg(getToken()).execute().body();
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    public void getFlows(final Callback<FlowList> callback) {
        m_api.getFlows(getToken(), "S", false).enqueue(new Callback<FlowList>() {
            @Override
            public void onResponse(Call<FlowList> call, Response<FlowList> response) {
                m_flowList = response.body();
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<FlowList> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void getFlowDefinition(final DBFlow flow, final Callback<FlowDefinition> callback) {

        final Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        flow.setFetching(true);
        realm.commitTransaction();

        m_api.getFlowDefinition(getToken(), flow.getUuid()).enqueue(new Callback<FlowDefinition>() {
            @Override
            public void onResponse(Call<FlowDefinition> call, Response<FlowDefinition> response) {

                realm.beginTransaction();
                flow.setFetching(false);
                realm.commitTransaction();
                realm.close();

                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<FlowDefinition> call, Throwable t) {
                realm.beginTransaction();
                flow.setFetching(false);
                realm.commitTransaction();
                realm.close();
                callback.onFailure(call, t);
            }
        });
    }

    public Contact addContact(final Contact contact) {

        if ("base".equals(contact.getLanguage())) {
            contact.setLanguage(null);
        }

        try {
            JsonObject result = m_api.addContact(getToken(), contact.toJson()).execute().body();
            String uuid = result.get("uuid").getAsString();
            contact.setUuid(uuid);
            return contact;
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    /**
     * Uploads a media file and returns the remove URL
     * @param file the local file to upload
     * @param flowUuid the flow this media file is associated with
     * @return the relative path to media
     */
    public String uploadMedia(File file, String flowUuid) {

        Map<String, RequestBody> map = new HashMap<>();
        map.put("flow", RequestBody.create(MediaType.parse("text/plain"), flowUuid));

        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        map.put("media_file\"; filename=\"" + file.getName(), fileBody);

        try {
            JsonObject result = m_api.uploadMedia(getToken(), map).execute().body();

            return result.get("location").getAsString();
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    public void addResults(final Submission submission) {
        JsonObject obj = submission.toJson().getAsJsonObject();

        // replace contact with uuid
        JsonObject contact = obj.get("contact").getAsJsonObject();
        obj.addProperty("contact", contact.get("uuid").getAsString());

        try {
            m_api.addResults(getToken(), obj).execute();
            submission.delete();
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    public void addCreatedFields(HashMap<String, Field> fields) {
        for (Field field : fields.values()) {
            m_api.addCreatedField(getToken(), field);
        }
    }

    public List<DBLocation> getLocations() {

        List<DBLocation> locations = new ArrayList<>();

        try {
            int pageNumber = 1;
            // fetch our first page
            LocationResultPage page = m_api.getLocationPage(getToken(), true, pageNumber).execute().body();
            locations.addAll(page.results);

            // fetch subsequent pages until we are done
            while (page != null && page.next != null && page.next.trim().length() != 0) {
                page = m_api.getLocationPage(getToken(), true, ++pageNumber).execute().body();
                locations.addAll(page.results);
            }

            return locations;
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    public List<Field> getFields() {

        try {
            List<Field> fields = new ArrayList<>();
            int pageNumber = 1;
            FieldResultPage page = m_api.getFieldPage(getToken(), pageNumber).execute().body();
            fields.addAll(page.getRunnerFields());

            while (page != null && page.next != null && page.next.trim().length() != 0) {
                page = m_api.getFieldPage(getToken(), ++pageNumber).execute().body();
                fields.addAll(page.getRunnerFields());
            }
            return fields;
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    private TembaAPI getAPIAccessor() {

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

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        m_retrofit = new Retrofit.Builder()
                .baseUrl(Surveyor.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();



        // .setLogLevel(RestAdapter.LogLevel.FULL)

        return m_retrofit.create(TembaAPI.class);
    }

    public APIError parseError(Response<?> response){
        Converter<ResponseBody, APIError> converter =
                m_retrofit.responseBodyConverter(APIError.class, new Annotation[0]);
        APIError error;
        try {
            error = converter.convert(response.errorBody());
        } catch (IOException e) {
            error = new APIError(response.code());
        }

        return error;
    }

    public int getErrorMessage(Throwable t) {

        if (t == null) {
            return R.string.error_server_not_found;
        }

        return R.string.error_server_failure;
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
