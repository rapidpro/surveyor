package io.rapidpro.surveyor.net;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.surveyor.BuildConfig;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.ResponseException;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.TembaException;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.DBLocation;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.net.responses.Definitions;
import io.rapidpro.surveyor.net.responses.FieldPage;
import io.rapidpro.surveyor.net.responses.FlowPage;
import io.rapidpro.surveyor.net.responses.LocationResultPage;
import io.rapidpro.surveyor.net.responses.TokenResults;
import io.realm.Realm;
import io.realm.RealmObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
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
    private FlowPage m_flowPage;

    public TembaService(String host) {
        m_api = getAPIAccessor(host);
    }

    public FlowPage getLastFlows() {
        return m_flowPage;
    }

    public void setToken(String token) {
        m_token = "Token " + token;
    }

    public String getToken() {
        return m_token;
    }

    public void getOrgs(String username, String password, Callback<TokenResults> callback) {
        m_api.getTokens(username, password, "S").enqueue(callback);
    }

    public DBOrg getOrg() {
        try {
            Response<DBOrg> response = m_api.getOrg(getToken()).execute();
            checkResponse(response);
            return response.body();
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    public void getFlows(final Callback<FlowPage> callback) {
        m_api.getFlows(getToken(), "survey", false).enqueue(new Callback<FlowPage>() {
            @Override
            public void onResponse(Call<FlowPage> call, Response<FlowPage> response) {
                checkResponse(response);
                m_flowPage = response.body();
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<FlowPage> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void getFlowDefinition(final DBFlow flow, final Callback<Definitions> callback) {

        final Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        flow.setFetching(true);
        realm.commitTransaction();

        m_api.getFlowDefinition(getToken(), flow.getUuid()).enqueue(new Callback<Definitions>() {
            @Override
            public void onResponse(final Call<Definitions> call, final Response<Definitions> response) {
                checkResponse(response);

                realm.beginTransaction();
                flow.setFetching(false);
                realm.commitTransaction();
                realm.close();
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Definitions> call, Throwable t) {
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
            Response<JsonObject> result = m_api.addContact(getToken(), contact.toJson()).execute();

            checkResponse(result);

            String uuid = result.body().get("uuid").getAsString();
            contact.setUuid(uuid);
            return contact;

        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    public void checkResponse(Response<?> response) {

        if (!response.isSuccessful()) {

            String errorBody;
            try {
                errorBody = response.errorBody().string();
            } catch (Exception e) {
                throw new TembaException(e);
            }

            // make a note of the error in our log
            Surveyor.LOG.d(errorBody);

            // see if the server had anything interesting to say
            JsonObject error = JsonUtils.getGson().fromJson(errorBody, JsonObject.class);
            if (error != null) {
                JsonElement detail = error.get("detail");
                if (detail != null) {

                    String message = detail.getAsString();
                    if (message.equals("Invalid token")) {
                        message = "Login failure, please logout and try again.";
                    }
                    throw new ResponseException(message);
                }
            }

            throw new TembaException("Error reading response");
        }
    }

    /**
     * Uploads a media file and returns the remove URL
     *
     * @param file the local file to upload
     * @return the relative path to media
     */
    public String uploadMedia(File file, String extension) {

        Map<String, RequestBody> map = new HashMap<>();

        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        map.put("media_file\"; filename=\"" + file.getName(), fileBody);

        RequestBody extBody = RequestBody.create(MediaType.parse("text/plain"), extension);
        map.put("extension", extBody);

        Response<JsonObject> result;
        try {
            result = m_api.uploadMedia(getToken(), map).execute();
            checkResponse(result);
        } catch (IOException e) {
            throw new TembaException("Error uploading media", e);
        }

        return result.body().get("location").getAsString();
    }

    public void addResults(final Submission submission) {
        try {

            boolean success = false;
            for (JsonObject result : submission.getResultsJson()) {
                Response response = m_api.addResults(getToken(), result).execute();
                if (response.isSuccessful()) {
                    success = response.isSuccessful();
                }

            }
            if (success) {
                submission.delete();
            } else {
                throw new TembaException("Error submitting results");
            }
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    public List<DBLocation> getLocations() {

        List<DBLocation> locations = new ArrayList<>();

        try {
            int pageNumber = 1;
            // fetch our first page
            LocationResultPage page = m_api.getLocationPage(getToken(), true, pageNumber).execute().body();
            locations.addAll(page.getResults());

            // fetch subsequent pages until we are done
            while (page != null && page.hasNext()) {
                page = m_api.getLocationPage(getToken(), true, ++pageNumber).execute().body();
                locations.addAll(page.getResults());
            }

            return locations;
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    public List<Field> getFields() {

        try {
            List<Field> fields = new ArrayList<>();
            FieldPage page = null;

            do {
                String cursor = page != null ? page.getNextCursor() : null;
                page = m_api.getFieldPage(getToken(), cursor).execute().body();
                fields.addAll(page.toRunnerFields());

            } while (page.hasNext());

            return fields;
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    private TembaAPI getAPIAccessor(String host) {

        Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getDeclaringClass().equals(RealmObject.class);
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).create();


        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS);

        // add extra logging for debug mode
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(interceptor);
        }

        final OkHttpClient okHttpClient = builder.build();

        try {
            m_retrofit = new Retrofit.Builder()
                    .baseUrl(host)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)

                    .build();
        } catch (IllegalArgumentException e) {
            throw new TembaException(e);
        }

        return m_retrofit.create(TembaAPI.class);
    }

    public APIError parseError(Response<?> response) {
        Converter<ResponseBody, APIError> converter =
                m_retrofit.responseBodyConverter(APIError.class, new Annotation[0]);

        APIError error = new APIError(response.code(), null);
        try {
            error = converter.convert(response.errorBody());
        } catch (IOException e) {
            try {
                error = new APIError(response.code(), response.errorBody().string());
            } catch (IOException last) {
            }
        }

        return error;
    }

    public int getErrorMessage(Throwable t) {
        if (t == null) {
            return R.string.error_server_not_found;
        }
        return R.string.error_server_failure;
    }
}
