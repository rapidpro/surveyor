package io.rapidpro.surveyor.net;

import android.content.Context;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.data.Org;
import io.rapidpro.surveyor.data.Flow;
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
        m_token = token;
    }

    public void getOrgs(String email, String password, Callback<List<Org>> callback) {
        m_api.getOrgs(email, password, callback);
    }

    public void getFlows(final Callback<FlowList> callback) {
        m_api.getFlows("Token " + m_token, new Callback<FlowList>() {
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
        }).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Surveyor.BASE_URL)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setConverter(new GsonConverter(gson))
                .build();

        return restAdapter.create(RapidProAPI.class);
    }
}
