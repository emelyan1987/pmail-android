package com.planckmail.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.PlanckMailSharePreferences;
import com.planckmail.helper.InternetConnection;
import com.planckmail.utils.BundleKeys;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.TypedJsonString;
import com.planckmail.web.restClient.service.NylasService;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;

/**
 * Created by Taras Matolinets on 31.07.15.
 */
public class SendMessageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {

        if (InternetConnection.isNetworkConnected(context)) {
            String message = PlanckMailSharePreferences.getSharedPreferences().getString(BundleKeys.MESSAGE, null);
            String token = PlanckMailSharePreferences.getSharedPreferences().getString(BundleKeys.ACCESS_TOKEN, null);

            if (message != null && token != null) {
                sendMessage(context, message, token);
            }
        }
    }

    public void sendMessage(final Context context, String message, String token) {
        final TypedInput in = new TypedJsonString(message);

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, token, "");
        nylasServer.sendMessage(in, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.i(PlanckMailApplication.TAG, "response url" + response.getUrl());

                Toast.makeText(context, context.getString(R.string.messageSent), Toast.LENGTH_SHORT).show();

                //remove data from sharePreferences
                SharedPreferences.Editor editor = PlanckMailSharePreferences.getSharedPreferences().edit();

                editor.remove(BundleKeys.MESSAGE);
                editor.remove(BundleKeys.ACCESS_TOKEN);
                editor.apply();
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null) {
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
                Toast.makeText(context, context.getString(R.string.sendEmailError), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
