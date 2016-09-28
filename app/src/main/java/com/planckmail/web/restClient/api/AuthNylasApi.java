package com.planckmail.web.restClient.api;

import android.util.Base64;

import com.planckmail.web.restClient.RestWebClient;
import com.squareup.okhttp.OkHttpClient;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

/**
 * Created by Taras Matolinets on 05.05.15.
 */
public class AuthNylasApi {
    private static RestAdapter.Builder builder = new RestAdapter.Builder().setClient(new OkClient(new OkHttpClient()));

    // No need to instantiate this class.
    private AuthNylasApi() {
    }

    public static <S> S createService(Class<S> serviceClass, String baseUrl, String username, String password) {
        // set endpoint url

        RestWebClient.JsonConverter jacksonConverter = new RestWebClient.JsonConverter();
        builder.setEndpoint(baseUrl).setClient(new OkClient());

        if (username != null && password != null) {
            // concatenate username and password with colon for authentication
            final String credentials = username + ":" + password;

            builder.setRequestInterceptor(new RequestInterceptor() {
                @Override
                public void intercept(RequestFacade request) {
                    // create Base64 encodet string
                    String string = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    request.addHeader("Authorization", string);
                }
            });
        }

        RestAdapter adapter = builder.setConverter(jacksonConverter).build();

        return adapter.create(serviceClass);
    }
}
