package com.planckmail.web.restClient.service;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

/**
 * Created by Taras Matolinets on 18.08.15.
 */
public interface PlanckService {

    @FormUrlEncoded
    @POST("/server/save_token")
    void storeToken(@Field("email_id") String email_id, @Field("token") String token, @Field("access_token") String access_token, Callback<String> callback);

    @FormUrlEncoded
    @POST("/server/get_token")
    Response readToken(@Field("email_id") String email_id, @Field("access_token") String access_token);

    @FormUrlEncoded
    @POST("/server/delete_token")
    void deleteToken(@Field("email_id") String email_id, @Field("access_token") String access_token, Callback<Object> callback);

    @FormUrlEncoded
    @POST("/server/add_user_to_priority_list")
    void addUserToPriorityToken(@Field("email_id") String email_id, @Field("token") String access_token, Callback<Object> callback);

    @FormUrlEncoded
    @POST("/server/get_top_threads_with_msgs")
    void getThreadsList(@Field("token") String token, @Field("thread_count") int count, Callback<String> callback);

    @FormUrlEncoded
    @POST("/server/set_push_user")
    void set_push_user(@Field("email_id") String email, @Field("android_id") String deviceId, Callback<String> callback);

    @FormUrlEncoded
    @POST("/server/reset_push_user")
    void reset_push_user(@Field("email_id") String email, Callback<String> callback);

    @FormUrlEncoded
    @POST("/api/keyphrases/")
    void getKeyphrases(@Field("SUBJECT") String subject, @Field("TEXT") String text, Callback<String> callback);

    @FormUrlEncoded
    @POST("/api/basicsummarize/")
    void getBasicSummarize(@Field("METHOD") String method, @Field("TEXT") String text, Callback<String> callback);

    @FormUrlEncoded
    @POST("/server/add_thread_to_followup")
    void add_thread_to_snooze(@Field("email_id") String email, @Field("thread_id") String threadId, @Field("current_status") String currentStatus,
                              @Field("token") String token, @Field("snooze_till") long time, Callback<String> callback);

    @FormUrlEncoded
    @POST("/server/add_thread_to_reminder_list")
    void add_thread_to_notify(@Field("email_id") String email, @Field("thread_id") String threadId, @Field("msg_id") String messageId, @Field("auto_ask") int autoAsk,
                              @Field("subject") String subject, @Field("max_time") long time, Callback<String> callback);

    @FormUrlEncoded
    @POST("/va/get_free_slots/")
    void getAvailability(@Field("timezone") String timezone,@Field("email_id") String email, @Field("start_date") String startTime, @Field("time_frame") int timeFrame, @Field("duration") int seconds, Callback<String> callback);

    @FormUrlEncoded
    @POST("/va/create_event/")
    void createEvent(@Field("sender") String sender, @Field("starttime") long startTime, @Field("endtime") long endTime,
                     @Field("location") String location, @Field("participants") String participants, Callback<String> callback);

    @FormUrlEncoded
    @POST("/api/add_to_blacklist/")
    void addToBlackList(@Field("email_id") String emailId, @Field("blacklist_id") String blackListId, Callback<String> callback);

    @FormUrlEncoded
    @POST("/api/get_blacklist/")
    void getBlackListIds(@Field("email_id") String emailId, Callback<String> callback);

    @FormUrlEncoded
    @POST("/api/remove_from_blacklist/")
    void removeFromBlackList(@Field("email_id") String emailId, @Field("blacklist_id") String blackListId, Callback<String> callback);



    /* ============ Email Tracking ============= */

    @GET("/list")
    void getTrackingList(@Query("owner_email") String ownerEmail, @Query("status") String status, @Query("time") String time, Callback<String> callback);

    @GET("/details")
    void getTrackDetails(@Query("track_id") String trackId, Callback<String> callback);

    @POST("/create")
    void createTrack(Callback<String> callback);

    @FormUrlEncoded
    @POST("/update")
    void updateTrack(@Field("id") String id, @Field("thread_id") String threadId, @Field("message_id") String messageId, @Field("subject") String subject, @Field("owner_email") String owner_email, @Field("target_emails") String target_emails, Callback<String> callback);
}
