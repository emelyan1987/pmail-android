package com.planckmail.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.service.PlanckService;

import java.io.IOException;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Taras Matolinets on 14.10.15.
 */
public class RegistrationIntentService extends IntentService {

    private static final String SERVICE_TAG = "PlanckMailIntentService";
    private static final String[] TOPICS = {"global"};
    private List<AccountInfo> mListAccountInfo;

    public RegistrationIntentService() {
        super(PlanckMailApplication.TAG + SERVICE_TAG);
        mListAccountInfo = UserHelper.getEmailAccountList(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.i(PlanckMailApplication.TAG, "GCM Registration Token: " + token);

            PlanckMailApplication app = (PlanckMailApplication) getApplication();

            if (app.isSentToken())
                sendRegistrationToServer(token);

            // Subscribe to topic channels
            subscribeTopics(token);

        } catch (Exception e) {
            Log.e(PlanckMailApplication.TAG, "Failed to complete token refresh", e);
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(BundleKeys.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     * <p/>
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        for (AccountInfo accountInfo : mListAccountInfo) {
            RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
            PlanckService service = client.getPlankService();

            service.set_push_user(accountInfo.getEmail(), token, new Callback<String>() {
                @Override
                public void success(String o, Response response) {
                    Log.i(PlanckMailApplication.TAG, "token is saved " + response.getUrl());
                    PlanckMailApplication app = (PlanckMailApplication) RegistrationIntentService.this.getApplication();
                    app.setSendToken(false);
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
            });
        }
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]

}
