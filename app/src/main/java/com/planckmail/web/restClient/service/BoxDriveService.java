package com.planckmail.web.restClient.service;

import java.util.Map;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import retrofit.mime.TypedInput;

/**
 * Created by Terry on 1/26/2016.
 */
public interface BoxDriveService {
    @GET("/oauth2/authorize")
    void authorization(@Query("client_id") String clientId, @Query("response_type") String responseType, @Query("redirect_uri") String redirectUrl,
                       @Query("state") String scope, Callback<Object> callback);

    @FormUrlEncoded
    @POST("/oauth2/token")
    void getAccessToken(@Field("code") String code, @Field("client_id") String clientId, @Field("client_secret") String clientSecret, @Field("redirect_uri") String redirectUri,
                        @Field("grant_type") String grantType, Callback<String> callback);


    @FormUrlEncoded
    @POST("/oauth2/token")
    void refreshToken(@Field("client_id") String clientId, @Field("client_secret") String clientSecret, @Field("refresh_token") String refreshToken,
                      @Field("grant_type") String grantType, Callback<String> callback);

    @FormUrlEncoded
    @POST("/oauth2/revoke")
    void revokeToken(@Field("client_id") String clientId, @Field("client_secret") String clientSecret, @Field("token") String token, Callback<String> callback);

    @GET("/users/me")
    void getCurrentUser(Callback<String> callback);

    @GET("/search")
    void searchList(@QueryMap Map<String, String> options,Callback<Object> callback);

    @PUT("/files/{fileId}")
    void getShareLink(@Path("fileId") long fileId, @Body TypedInput body, Callback<String> callback);


    @GET("/folders/{folder_id}/items")
    void getFolderList(@Path("folder_id") long folderID, @QueryMap Map<String, String> options, Callback<String> callback);
}
