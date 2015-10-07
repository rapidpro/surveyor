package io.rapidpro.surveyor.net;

import android.location.Location;

import java.util.List;

import io.rapidpro.flows.runner.Contact;
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
    DBOrg getOrg(@Header("Authorization") String token);

    @GET("/api/v1/flows.json")
    void getFlows(
            @Header("Authorization") String token,
            @Query("type") String type,
            Callback<FlowList> callback);

    @GET("/api/v1/flow_definition.json")
    void getFlowDefinition(
            @Header("Authorization") String token,
            @Query("uuid") String uuid,
            Callback<FlowDefinition> callback);

    @POST("/api/v1/steps.json")
    Void addResults(
            @Header("Authorization") String token,
            @Body Submission state);

    @POST("/api/v1/contacts.json")
    Contact addContact(
            @Header("Authorization") String token,
            @Body Contact contact);

    @GET("/api/v1/boundaries.json")
    LocationResultPage getLocationPage(
            @Header("Authorization") String token,
            @Query("aliases") boolean aliases,
            @Query("page") int page);

}
