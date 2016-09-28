package com.planckmail.web.restClient.service;

import java.util.Map;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import retrofit.http.Streaming;
import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedInput;

/**
 * Created by Taras Matolinets on 03.05.15.
 */
public interface NylasService {
    @GET("/oauth/authorize")
    void authorization(@Query("client_id") String clientId, @Query("response_type") String token,
                       @Query("scope") String email, @Query("login_hint") String loginHint,
                       @Query("redirect_uri") String redirectUrl, Callback<Object> callback
    );

    @GET("/oauth/authorize")
    void authorization(@Query("client_id") String clientId, @Query("response_type") String token,
                       @Query("scope") String email, @Query("redirect_uri") String redirectUrl,
                       Callback<Object> callback
    );


    @GET("/account")
    void getAccounts(Callback<Object> callback);

    @GET("/threads")
    void getThreadInMail(@QueryMap Map<String, String> mapPrams, Callback<Object> callback);

    @GET("/messages")
    void getMessagesFromMail(@QueryMap Map<String, String> mapPrams, Callback<Object> callback);

    @GET("/threads")
    void getThreadsInbox(@QueryMap Map<String, String> mapPrams, @Query("limit") int count,
                         @Query("offset") int offset, Callback<Object> callback);

    @GET("/messages")
    void getMessages(@Query("thread_id") String threadId, Callback<Object> callback);

    @GET("/messages/{message_id}")
    void getMessage(@Path("message_id") String messageId, Callback<String> callback);

    @PUT("/messages/{message_id}")
    void markReadMessage(@Path("message_id") String messageId, @Body TypedInput body, Callback<Object> callback);

    @DELETE("/messages/{message_id}")
    void deleteMessage(@Path("message_id") String messageId, Callback<Object> callback);

    @GET("/contacts")
    void getContacts(@Query("limit") int count, @Query("filter") String symbol, Callback<Object> callback);

    @GET("/contacts")
    void getAllContacts(@Query("limit") int limit, Callback<Object> callback);

    @POST("/send")
    void sendMessage(@Body TypedInput body, Callback<Object> callback);

    @GET("/threads/search")
    void getAllMailsInbox(@Query("q=subject") String email, Callback<Object> callback);

    @GET("/calendars")
    void getAllCalendars(Callback<Object> callback);

    @GET("/events")
    void getAllEvents(@Query("limit") int limit, Callback<Object> callback);

    @POST("/events")
    void addNewEvent(@Body TypedInput body, Callback<Object> callback);

    @PUT("/events/{event_id}")
    void updateEvent(@Path("event_id") String eventId, @Body TypedInput body, Callback<Object> callback);

    @DELETE("/events/{event_id}")
    void deleteEvent(@Path("event_id") String eventId, Callback<Object> callback);

    @PUT("/threads/{thread_id}")
    void updateRemoveThread(@Path("thread_id") String threadId, @Body TypedInput body, Callback<Object> callback);

    @PUT("/messages/{message_id}")
    void updateRemoveMessage(@Path("message_id") String threadId, @Body TypedInput body, Callback<Object> callback);

    @POST("/delta/generate_cursor")
    void generateCursor(@Body TypedInput body, Callback<Object> callback);

    @GET("/delta")
    void pullThreads(@Query("cursor") String cursorId, @QueryMap Map<String, String> options, Callback<Object> callback);

    @POST("/drafts")
    void createDraft(@Body TypedInput body, Callback<Object> callback);

    @GET("/drafts/{draft_id}")
    void getDraft(@Path("draft_id") String draftId, Callback<Object> callback);

    @DELETEDRAFT("/drafts/{draft_id}")
    void deleteDraft(@Path("draft_id") String draftId, @Body TypedInput body, Callback<Object> callback);

    @PUT("/drafts/{draft_id}")
    void updateDraft(@Path("draft_id") String draftId, @Body TypedInput body, Callback<Object> callback);

    @POST("/files")
    void uploadFile(@Body MultipartTypedOutput attachments, Callback<Object> callback);

    @GET("/files")
    void getFiles(Callback<Object> callback);

    @GET("/files{id}/download")
    @Streaming
    void downloadFile(@Path("id") String fileId, Callback<Response> callback);

    @GET("/{folder}")
    void getFolders(@Path("folder") String folder, Callback<Object> callback);

    @POST("/folders")
    void createFolder(@Body TypedInput body, Callback<Object> callback);

    @POST("/oauth/revoke")
    void revokeAccessToken(Callback<Object> callback);

}
