package com.planckmail.web.restClient.api;

import com.planckmail.web.restClient.RestWebClient;
import com.squareup.okhttp.OkHttpClient;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

/**
 * Created by Terry on 1/27/2016.
 */
public class AuthBoxDriveApi {
    private static RestAdapter.Builder builder = new RestAdapter.Builder().setClient(new OkClient(new OkHttpClient()));

    // No need to instantiate this class.
    private AuthBoxDriveApi() {
    }

    public static <S> S createService(Class<S> serviceClass, String baseUrl, String username, String password) {
        // set endpoint url

        RestWebClient.JsonConverter jacksonConverter = new RestWebClient.JsonConverter();
        builder.setEndpoint(baseUrl).setClient(new OkClient());

        if (username != null && password != null) {
            // concatenate username and password with colon for authentication
            final String credentials = "Bearer " + username;

            builder.setRequestInterceptor(new RequestInterceptor() {
                @Override
                public void intercept(RequestFacade request) {
                    request.addHeader("Authorization", credentials);
                }
            });
        }

        RestAdapter adapter = builder.setConverter(jacksonConverter).build();

        return adapter.create(serviceClass);
    }
}
