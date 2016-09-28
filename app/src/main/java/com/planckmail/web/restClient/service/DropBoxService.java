package com.planckmail.web.restClient.service;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

/**
 * Created by Terry on 12/24/2015.
 */
public interface DropBoxService {
    @GET("/oauth2/authorize")
    void authorization(@Query("client_id") String clientId, @Query("response_type") String token, @Query("redirect_uri") String redirectUrl, Callback<Object> callback);

    @GET("/metadata/auto")
    void getFiles(@Query("path") String path, Callback<Object> callback);

    @GET("/shares/auto")
    void shares(@Query("path") String path, Callback<Object> callback);

    @GET("/account/info")
    void getAccountInfo(Callback<Object> callback);

    @GET("/media/auto/")
    void getFileMedia(@Query("path") String path,Callback<Object> callback);

    @GET("/search/auto")
    void searchFile(@Query("query") String path,Callback<Object> callback);

    @POST("/disable_access_token")
    void disableAccessToken(Callback<Object> callback);
}
