package com.planckmail.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.planckmail.application.PlanckMailApplication;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.wrapper.File;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.service.NylasService;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.InputStream;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Terry on 12/22/2015.
 */
public class ServiceFileDownload extends IntentService {

    private static final String PLANCK_FILE_SERVICE = "PLANCK_FILE_SERVICE";
    private static final String FILE = "file";
    private static final String SEPARATOR = "-";
    private static final String DOT = ".";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */

    public ServiceFileDownload() {
        super(PLANCK_FILE_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String accessToken = intent.getStringExtra(BundleKeys.ACCESS_TOKEN);
        String fileJson = intent.getStringExtra(BundleKeys.FILE);
        File file = JsonUtilFactory.getJsonUtil().fromJson(fileJson, File.class);

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accessToken, "");

        nylasServer.downloadFile(file.getId(), new Callback<Response>() {
            @Override
            public void success(Response serverResponse, Response response) {
                try {
                    InputStream fileInputStream = serverResponse.getBody().in();

                    DateTime date = new DateTime();
                    DateTimeFormatter formatterDateStart = DateTimeFormat.forPattern("hh-mm-ss");

                    String fileName = FILE + SEPARATOR + formatterDateStart.print(date) + DOT;
                    UserHelper.copyInputStreamToFile(fileInputStream, fileName);
                } catch (IOException e) {
                    Log.e(PlanckMailApplication.TAG, e.toString());
                }
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
