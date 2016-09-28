package com.planckmail.web.restClient;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.planckmail.web.restClient.service.BoxDriveService;
import com.planckmail.web.restClient.service.DropBoxService;
import com.planckmail.web.restClient.service.GoogleDriveService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import retrofit.RestAdapter;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Created by Terry on 1/26/2016.
 */
public class RestClientBoxDriveClient {
    public static final String BASE_URL = " https://app.box.com/api";
    public static final String BASE_URL1 = "https://app.box.com/api/2.0";

    private BoxDriveService googleDriveServer;
    private static RestClientGoogleDriveClient instance;

    public RestClientBoxDriveClient(String url) {
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(url)
                .setConverter(new JsonConverter())
                .build();

        googleDriveServer = restAdapter.create(BoxDriveService.class);
    }


    public BoxDriveService getBoxDriveService() {
        return googleDriveServer;
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
