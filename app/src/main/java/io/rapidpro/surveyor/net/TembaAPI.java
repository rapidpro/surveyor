package io.rapidpro.surveyor.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

import io.rapidpro.surveyor.net.requests.SubmissionPayload;
import io.rapidpro.surveyor.net.responses.Boundary;
import io.rapidpro.surveyor.net.responses.Definitions;
import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Flow;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.net.responses.Org;
import io.rapidpro.surveyor.net.responses.PaginatedResults;
import io.rapidpro.surveyor.net.responses.TokenResults;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
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
    Call<TokenResults> authenticate(
            @retrofit2.http.Field("username") String username,
            @retrofit2.http.Field("password") String password,
            @retrofit2.http.Field("role") String role
    );

    @GET("/api/v2/boundaries.json")
    Call<PaginatedResults<Boundary>> getBoundaries(
            @Header("Authorization") String token,
            @Query("cursor") String cursor
    );

    @GET("/api/v2/definitions.json")
    Call<Definitions> getDefinitions(
            @Header("Authorization") String token,
            @Query("flow") List<String> flowUUIDs,
            @Query("dependencies") String dependencies
    );

    @GET("/api/v2/org.json")
    Call<Org> getOrg(@Header("Authorization") String token);

    @GET("/api/v2/fields.json")
    Call<PaginatedResults<Field>> getFields(
            @Header("Authorization") String token,
            @Query("cursor") String cursor
    );

    @GET("/api/v2/flows.json")
    Call<PaginatedResults<Flow>> getFlows(
            @Header("Authorization") String token,
            @Query("type") String type,
            @Query("archived") Boolean archived,
            @Query("cursor") String cursor
    );

    @GET("/api/v2/groups.json")
    Call<PaginatedResults<Group>> getGroups(
            @Header("Authorization") String token,
            @Query("cursor") String cursor
    );

    @Multipart
    @POST("/api/v2/media.json")
    Call<JsonObject> uploadMedia(
            @Header("Authorization") String token,
            @PartMap Map<String, RequestBody> params
    );

    @POST("/mr/surveyor/submit")
    Call<JsonObject> submit(
            @Header("Authorization") String token,
            @Body SubmissionPayload submission
    );

    /* Legacy endpoints to be removed */

    @Deprecated
    @POST("/api/v1/fields.json")
    Void legacyAddCreatedField(
            @Header("Authorization") String token,
            @Body io.rapidpro.flows.runner.Field field
    );

    @Deprecated
    @POST("/api/v1/contacts.json")
    Call<JsonObject> legacyAddContact(
            @Header("Authorization") String token,
            @Body JsonElement contact
    );

    @Deprecated
    @POST("/api/v1/steps.json")
    Call<JsonObject> legacyAddResults(
            @Header("Authorization") String token,
            @Body JsonElement submissionJson
    );
}
