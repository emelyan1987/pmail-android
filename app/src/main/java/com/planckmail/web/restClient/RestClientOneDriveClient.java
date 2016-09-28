package com.planckmail.web.restClient;

import android.support.annotation.NonNull;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.planckmail.web.restClient.service.OneDriveService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Created by Terry on 1/17/2016.
 */
public class RestClientOneDriveClient {
    public static final String BASE_URL = "https://login.live.com";
    public static final String BASE_URL1 = "https://api.onedrive.com/v1.0";

    private OneDriveService dropBoxServer;
    private static RestClientOneDriveClient instance;

    private RestClientOneDriveClient() {
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(BASE_URL)
                .setConverter(new JsonConverter()).setRequestInterceptor(getRequestInterceptor())
                .build();

        dropBoxServer = restAdapter.create(OneDriveService.class);
    }

    @NonNull
    private RequestInterceptor getRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            }
        };
    }


    public static RestClientOneDriveClient getInstance() {
        if (instance == null)
            instance = new RestClientOneDriveClient();

        return instance;
    }

    public OneDriveService getOneDriveService() {
        return dropBoxServer;
    }

    public static class JsonConverter implements Converter {

        @Override
        public Object fromBody(TypedInput typedInput, Type type) throws ConversionException {
            String text = null;
            try {
                text = fromStream(typedInput.in());
            } catch (IOException ignored) {/*NOP*/ }

            return text;
        }

        @Override
        public TypedOutput toBody(Object o) {
            return null;
        }

        public static String fromStream(InputStream in) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            return out.toString();
        }
    }
}
