package io.rapidpro.surveyor.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

import io.rapidpro.surveyor.data.DBOrg;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

public interface TembaAPI {

    @FormUrlEncoded
    @POST("/api/v2/authenticate")
    Call<TokenResults> getTokens(
            @Field("username") String username,
            @Field("password") String password,
            @Field("role") String role);

    @GET("/api/v2/org.json")
    Call<DBOrg> getOrg(@Header("Authorization") String token);

    @GET("/api/v2/flows.json")
    Call<FlowPage> getFlows(
            @Header("Authorization") String token,
            @Query("type") String type,
            @Query("archived") boolean archived);

    @GET("/api/v2/definitions.json")
    Call<Definitions> getFlowDefinition(
            @Header("Authorization") String token,
            @Query("flow_uuid") String uuid);

    @POST("/api/v1/steps.json")
    Call<JsonObject> addResults(
            @Header("Authorization") String token,
            @Body JsonElement submissionJson);

    @POST("/api/v1/contacts.json")
    Call<JsonObject> addContact(
            @Header("Authorization") String token,
            @Body JsonElement contact);

    @GET("/api/v1/boundaries.json")
    Call<LocationResultPage> getLocationPage(
            @Header("Authorization") String token,
            @Query("aliases") boolean aliases,
            @Query("page") int page);

    @GET("/api/v2/fields.json")
    Call<FieldPage> getFieldPage(
            @Header("Authorization") String token,
            @Query("page") String cursor);

    @Multipart
    @POST("/api/v2/media.json")
    Call<JsonObject> uploadMedia(
            @Header("Authorization") String token,
            @PartMap Map<String, RequestBody> params);

}
