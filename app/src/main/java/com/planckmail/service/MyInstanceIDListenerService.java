package com.planckmail.service;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.planckmail.application.PlanckMailApplication;

/**
 * Created by Taras Matolinets on 14.10.15.
 */
public class MyInstanceIDListenerService extends InstanceIDListenerService {
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        // Fetch updated Instance THREAD_ID token and notify our app's server of any changes (if applicable).
        PlanckMailApplication app = (PlanckMailApplication) getApplication();
        app.setSendToken(true);

        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}
