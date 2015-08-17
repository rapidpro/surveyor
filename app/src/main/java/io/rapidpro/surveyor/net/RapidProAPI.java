package io.rapidpro.surveyor.net;

import java.util.List;

import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.Callback;

import io.rapidpro.surveyor.data.Org;

public interface RapidProAPI {

    @FormUrlEncoded
    @POST("/api/v1/authenticate")
    void getOrgs(
            @Field("email") String email,
            @Field("password") String password,
            Callback<List<Org>> callback);

    @GET("/api/v1/flows.json")
    void getFlows(@Header("Authorization") String token, Callback<FlowList> callback);
}
