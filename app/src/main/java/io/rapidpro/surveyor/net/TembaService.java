package io.rapidpro.surveyor.net;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.rapidpro.surveyor.BuildConfig;
import io.rapidpro.surveyor.Logger;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.engine.Engine;
import io.rapidpro.surveyor.net.requests.SubmissionPayload;
import io.rapidpro.surveyor.net.responses.Boundary;
import io.rapidpro.surveyor.net.responses.Definitions;
import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Flow;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.net.responses.Org;
import io.rapidpro.surveyor.net.responses.PaginatedResults;
import io.rapidpro.surveyor.net.responses.TokenResults;
import io.rapidpro.surveyor.utils.JsonUtils;
import io.rapidpro.surveyor.utils.RawJson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TembaService {

    private TembaAPI api;

    public TembaService(String host) {
        this.api = createRetrofit(host).create(TembaAPI.class);
    }

    /**
     * Utility to create a Authorization header value from a token
     */
    private static String asAuth(String token) {
        return "Token " + token;
    }

    private static Retrofit createRetrofit(String host) {

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
            return new Retrofit.Builder()
                    .baseUrl(host)
                    .addConverterFactory(GsonConverterFactory.create(JsonUtils.getGson()))
                    .client(okHttpClient)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls RapidPro's authenticate endpoint to give us the list of tokens and orgs we can access
     *
     * @param username the username
     * @param password the password
     */
    public void authenticate(String username, String password, Callback<TokenResults> callback) {
        api.authenticate(username, password, "S").enqueue(callback);
    }

    /**
     * Gets all of the admin boundaries
     *
     * @param token the authentication token
     */
    public List<Boundary> getBoundaries(final String token) throws TembaException {
        return fetchAllPages(new PageCaller<Boundary>() {
            @Override
            public Call<PaginatedResults<Boundary>> createCall(String cursor) {
                return api.getBoundaries(asAuth(token), cursor);
            }
        });
    }

    /**
     * Gets the org associated with the given token
     *
     * @param token the authentication token
     */
    public Org getOrg(String token) throws TembaException {
        try {
            Response<Org> response = api.getOrg(asAuth(token)).execute();
            checkResponse(response);

            return response.body();
        } catch (IOException e) {
            throw new TembaException("Unable to fetch org", e);
        }
    }

    /**
     * Gets all of the contact fields
     *
     * @param token the authentication token
     */
    public List<Field> getFields(final String token) throws TembaException {
        return fetchAllPages(new PageCaller<Field>() {
            @Override
            public Call<PaginatedResults<Field>> createCall(String cursor) {
                return api.getFields(asAuth(token), cursor);
            }
        });
    }

    /**
     * Gets all of the non-archived surveyor flows
     *
     * @param token the authentication token
     */
    public List<Flow> getFlows(final String token) throws TembaException {
        return fetchAllPages(new PageCaller<Flow>() {
            @Override
            public Call<PaginatedResults<Flow>> createCall(String cursor) {
                return api.getFlows(asAuth(token), "survey", false, cursor);
            }
        });
    }

    /**
     * Gets all of the contact groups
     *
     * @param token the authentication token
     */
    public List<Group> getGroups(final String token) throws TembaException {
        return fetchAllPages(new PageCaller<Group>() {
            @Override
            public Call<PaginatedResults<Group>> createCall(String cursor) {
                return api.getGroups(asAuth(token), cursor);
            }
        });
    }

    /**
     * Gets full definitions for the given flows
     *
     * @param token the authentication token
     * @param flows the list of flows
     */
    public List<RawJson> getDefinitions(final String token, final List<Flow> flows) throws TembaException {
        // gather up flow UUIDs
        final List<String> flowUUIDs = new ArrayList<>(flows.size());
        for (Flow flow : flows) {
            flowUUIDs.add(flow.getUuid());
        }

        try {
            Response<Definitions> response = api.getDefinitions(asAuth(token), flowUUIDs, "none").execute();
            checkResponse(response);

            Definitions definitions = response.body();
            return definitions.getFlows();

        } catch (IOException e) {
            throw new TembaException("Unable to fetch definitions", e);
        }
    }

    /**
     * Uploads a media file and returns the remove URL
     *
     * @param token the authentication token
     * @param uri   the local file to upload
     * @return the new media URL
     */
    public String uploadMedia(String token, Uri uri) throws TembaException {
        String uriString = uri.toString();
        String baseName = FilenameUtils.getBaseName(uriString);
        String extension = FilenameUtils.getExtension(uriString);

        // build multipart request
        Map<String, RequestBody> map = new HashMap<>();
        map.put("extension", RequestBody.create(MediaType.parse("text/plain"), extension));

        try {
            InputStream stream = SurveyorApplication.get().getContentResolver().openInputStream(uri);
            byte[] bytes = IOUtils.toByteArray(stream);

            RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), bytes);
            map.put("media_file\"; filename=\"" + baseName, fileBody);

            Response<JsonObject> result = api.uploadMedia(asAuth(token), map).execute();
            checkResponse(result);

            return result.body().get("location").getAsString();

        } catch (IOException e) {
            throw new TembaException("Error uploading media", e);
        }
    }

    /**
     * Submits a submission payload
     *
     * @param token      the authentication token
     * @param submission the payload
     */
    public void submit(String token, SubmissionPayload submission) throws TembaException {
        try {
            Response<JsonObject> result = api.submit(asAuth(token), submission).execute();
            checkResponse(result);

        } catch (IOException e) {
            throw new TembaException("Error submitting", e);
        }
    }

    /**
     * Utility for fetching all pages of a given type
     */
    private <T> List<T> fetchAllPages(PageCaller<T> caller) throws TembaException {
        try {
            List<T> all = new ArrayList<>();
            PaginatedResults<T> page = null;

            do {
                String cursor = page != null ? page.getNextCursor() : null;
                Call<PaginatedResults<T>> call = caller.createCall(cursor);
                Response<PaginatedResults<T>> response = call.execute();

                if (response.isSuccessful()) {
                    page = response.body();
                    all.addAll(page.getResults());
                } else {
                    throw new TembaException("Server returned non-200 response for " + call.request().url().toString());
                }

            } while (page.hasNext());

            return all;
        } catch (IOException e) {
            throw new TembaException("Unable to fetch page from API", e);
        }
    }

    private void checkResponse(Response<?> response) throws TembaException {

        if (!response.isSuccessful()) {

            String errorBody;
            try {
                errorBody = response.errorBody().string();
            } catch (Exception e) {
                throw new TembaException("Unable to extract error body", e);
            }

            // make a note of the error in our log
            Logger.w(errorBody);

            // see if the server had anything interesting to say
            Gson gson = new Gson();
            JsonObject error = gson.fromJson(errorBody, JsonObject.class);
            if (error != null) {
                JsonElement detail = error.get("detail");
                if (detail != null) {

                    String message = detail.getAsString();
                    if (message.equals("Invalid token")) {
                        message = "Login failure, please logout and try again.";
                    }
                    throw new TembaException(message);
                }
            }

            throw new TembaException("Error reading response");
        }
    }

    /**
     * Utility for fetching all pages of a given type
     */
    private interface PageCaller<T> {
        Call<PaginatedResults<T>> createCall(String cursor);
    }
}
