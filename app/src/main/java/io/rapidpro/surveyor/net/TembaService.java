package io.rapidpro.surveyor.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.rapidpro.surveyor.BuildConfig;
import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.ResponseException;
import io.rapidpro.surveyor.SurveyorApplication;
import io.rapidpro.surveyor.TembaException;
import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.net.responses.Org;
import io.rapidpro.surveyor.net.responses.PaginatedResults;
import io.rapidpro.surveyor.net.responses.TokenResults;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TembaService {

    private TembaAPI m_api;

    public TembaService(String host) {
        m_api = createRetrofit(host).create(TembaAPI.class);
    }

    /**
     * For testing purposes so we can give it a mocked API instance
     *
     * @param api the API instance
     */
    protected TembaService(TembaAPI api) {
        m_api = api;
    }

    /**
     * Calls RapidPro's authenticate endpoint to give us the list of tokens and orgs we can access
     */
    public void authenticate(String username, String password, Callback<TokenResults> callback) {
        m_api.authenticate(username, password, "S").enqueue(callback);
    }

    /**
     * Gets the org associated with the given token
     */
    public Org getOrg(String token) {
        try {
            Response<Org> response = m_api.getOrg(asAuth(token)).execute();
            checkResponse(response);
            return response.body();
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    /**
     * Gets all of the contact fields
     */
    public List<Field> getFields(final String token) {
        return fetchAllPages(new PageCaller<Field>() {
            @Override
            public Call<PaginatedResults<Field>> createCall(String cursor) {
                return m_api.getFields(asAuth(token), cursor);
            }
        });
    }

    /**
     * Gets all of the contact fields
     */
    public List<Group> getGroups(final String token) {
        return fetchAllPages(new PageCaller<Group>() {
            @Override
            public Call<PaginatedResults<Group>> createCall(String cursor) {
                return m_api.getGroups(asAuth(token), cursor);
            }
        });
    }

    /**
     * Utility for fetching all pages of a given type
     */
    private interface PageCaller<T> {
        Call<PaginatedResults<T>> createCall(String cursor);
    }

    /**
     * Utility for fetching all pages of a given type
     */
    private <T> List<T> fetchAllPages(PageCaller<T> caller) {
        try {
            List<T> all = new ArrayList<>();
            PaginatedResults<T> page = null;

            do {
                String cursor = page != null ? page.getNextCursor() : null;
                Response<PaginatedResults<T>> response = caller.createCall(cursor).execute();

                if (response.isSuccessful()) {
                    page = response.body();
                    all.addAll(page.getResults());
                } else {
                    throw new TembaException("Server returned non-200 response");
                }

            } while (page.hasNext());

            return all;
        } catch (IOException e) {
            throw new TembaException(e);
        }
    }

    private static String asAuth(String token) {
        return "Token " + token;
    }

    private void checkResponse(Response<?> response) {

        if (!response.isSuccessful()) {

            String errorBody;
            try {
                errorBody = response.errorBody().string();
            } catch (Exception e) {
                throw new TembaException(e);
            }

            // make a note of the error in our log
            SurveyorApplication.LOG.d(errorBody);

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
                    throw new ResponseException(message);
                }
            }

            throw new TembaException("Error reading response");
        }
    }

    private static Retrofit createRetrofit(String host) {

        Gson gson = new GsonBuilder().create();
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
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new TembaException(e);
        }
    }

    public int getErrorMessage(Throwable t) {
        if (t == null) {
            return R.string.error_server_not_found;
        }
        return R.string.error_server_failure;
    }
}
