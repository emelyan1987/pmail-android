package com.planckmail.web.restClient;

import android.content.Context;

import com.planckmail.data.db.beans.AccountInfo;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.picasso.OkHttpDownloader;

import java.io.IOException;

/**
 * Created by Terry on 2/22/2016.
 */
public class CustomOkHttpDownloader extends OkHttpDownloader {
    public CustomOkHttpDownloader(Context context, final AccountInfo accountInfo) {
        super(context);
        final String credentials = "Bearer " + accountInfo.accessToken;
        getClient().interceptors().add(new Interceptor() {
            @Override
            public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                Request newRequest = chain.request().newBuilder()
                        .addHeader("Authorization", credentials)
                        .build();
                return chain.proceed(newRequest);
            }
        });
    }
}
