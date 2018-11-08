package io.rapidpro.surveyor.net;

import io.rapidpro.surveyor.net.responses.Org;
import io.rapidpro.surveyor.net.responses.TokenResults;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface TembaAPI {

    @FormUrlEncoded
    @POST("/api/v2/authenticate")
    Call<TokenResults> authenticate(
            @Field("username") String username,
            @Field("password") String password,
            @Field("role") String role);

    @GET("/api/v2/org.json")
    Call<Org> getOrg(@Header("Authorization") String token);
}
