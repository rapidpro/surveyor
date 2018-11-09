package io.rapidpro.surveyor.net;

import io.rapidpro.surveyor.net.responses.Field;
import io.rapidpro.surveyor.net.responses.Group;
import io.rapidpro.surveyor.net.responses.Org;
import io.rapidpro.surveyor.net.responses.PaginatedResults;
import io.rapidpro.surveyor.net.responses.TokenResults;
import retrofit2.Call;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface TembaAPI {

    @FormUrlEncoded
    @POST("/api/v2/authenticate")
    Call<TokenResults> authenticate(
            @retrofit2.http.Field("username") String username,
            @retrofit2.http.Field("password") String password,
            @retrofit2.http.Field("role") String role);

    @GET("/api/v2/org.json")
    Call<Org> getOrg(@Header("Authorization") String token);

    @GET("/api/v2/fields.json")
    Call<PaginatedResults<Field>> getFields(@Header("Authorization") String token, @Query("cursor") String cursor);

    @GET("/api/v2/groups.json")
    Call<PaginatedResults<Group>> getGroups(@Header("Authorization") String token, @Query("cursor") String cursor);
}
