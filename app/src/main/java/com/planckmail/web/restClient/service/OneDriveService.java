package com.planckmail.web.restClient.service;

import java.util.Map;

import retrofit.Callback;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import retrofit.mime.TypedString;

/**
 * Created by Terry on 1/17/2016.
 */
public interface OneDriveService {
    @FormUrlEncoded
    @POST("/oauth20_token.srf")
    void authorization(@Field("client_id") String clientId, @Field("redirect_uri") String redirectUrl, @Field("client_secret") String clientSecret,
                       @Field("code") String code, @Field("grant_type") String grantType, Callback<Object> callback);

    @GET("/oauth20_authorize.srf")
    void authorization(@Query("client_id") String clientId, @Query("scope") String scope, @Query("response_type") String responseType,
                       @Query("redirect_uri") String redirectUrl, Callback<Object> callback);

    @FormUrlEncoded
    @POST("/oauth20_token.srf")
    void getNewRefreshToken(@Field("client_id") String clientId, @Field("redirect_uri") String redirectUrl, @Field("client_secret") String clientSecret,
                            @Field("refresh_token") String refreshToken, @Field("grant_type") String grantType, Callback<Object> callback);


    @GET("/drive")
    void getDriveUserInfo(Callback<Object> callback);

    @GET("/drive/items/{item-id}/thumbnails?select=medium")
    void getOneDriveThumbnail(@Path("item-id") String itemId, Callback<Object> callback);

    @GET("/drive/root/view.search")
    void searchFiles(@QueryMap Map<String, String> options, Callback<Object> callback);

    @GET("/{folderPath}?expand=children")
    void getOneDriveMetaData(@Path(value = "folderPath", encode = false) String folderName, Callback<Object> callback);
}
