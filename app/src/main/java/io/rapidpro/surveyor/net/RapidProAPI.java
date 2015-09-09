package io.rapidpro.surveyor.net;

import java.util.List;

import io.rapidpro.flows.runner.RunState;
import io.rapidpro.surveyor.data.DBOrg;
import io.rapidpro.surveyor.data.Submission;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.Callback;

import retrofit.http.Query;

public interface RapidProAPI {

    @FormUrlEncoded
    @POST("/api/v1/authenticate")
    void getOrgs(
            @Field("email") String email,
            @Field("password") String password,
            Callback<List<DBOrg>> callback);

    @GET("/api/v1/org.json")
    void getOrg(@Header("Authorization") String token, Callback<DBOrg> callback);

    @GET("/api/v1/flows.json")
    void getFlows(
            @Header("Authorization") String token,
            @Query("type") String type,
            Callback<FlowList> callback);

    @GET("/api/v1/flowdefinition.json")
    void getFlowDefinition(
            @Header("Authorization") String token,
            @Query("uuid") String uuid,
            Callback<FlowDefinition> callback);

    @POST("/api/v1/steps.json")
    void addResults(
            @Header("Authorization") String token,
            @Body Submission state,
            Callback<Void> callback);

    @POST("/api/v1/contacts.json")
    void addContact(
            @Header("Authorization") String token,
            @Body Submission.Contact contact,
            Callback<Submission.Contact> callback);

}
