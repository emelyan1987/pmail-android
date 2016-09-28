package com.planckmail.web.restClient.service;

import java.util.Map;

import retrofit.Callback;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;

/**
 * Created by Terry on 1/21/2016.
 */
public interface GoogleDriveService {
    @GET("/o/oauth2/v2/auth")
    void authorization(@Query("client_id") String clientId, @Query("response_type") String responseType, @Query("redirect_uri") String redirectUrl,
                       @Query("scope") String scope, @Query("access_type") String accessType, @Query("prompt") String prompt, Callback<Object> callback);

    @FormUrlEncoded
    @POST("/oauth2/v4/token")
    void getAccessToken(@Field("code") String code, @Field("client_id") String clientId, @Field("client_secret") String clientSecret, @Field("redirect_uri") String redirectUri,
                        @Field("grant_type") String grantType, Callback<String> callback);


    @FormUrlEncoded
    @POST("/oauth2/v4/token")
    void refreshToken(@Field("client_id") String clientId, @Field("client_secret") String clientSecret, @Field("refresh_token") String refreshToken,
                      @Field("grant_type") String grantType, Callback<String> callback);


    @GET("/drive/v2/about")
    void about(Callback<String> callback);

    @GET("/o/oauth2/revoke")
    void revokeAccessToken(@Query("token") String token, Callback<String> callback);

    @GET("/drive/v3/files")
    void getFilesList(@QueryMap Map<String, String> options, Callback<String> callback);

    @GET("/drive/v2/permissionIds")
    void shareFiles(@Query("email") String email, Callback<String> callback);

    @GET("/drive/v3/files")
    void getSearchFiles(@QueryMap(encodeValues = false) Map<String, String> options, Callback<String> callback);
}
