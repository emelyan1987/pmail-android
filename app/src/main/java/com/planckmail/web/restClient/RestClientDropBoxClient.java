package com.planckmail.web.restClient;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.planckmail.web.restClient.service.DropBoxService;

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
 * Created by Terry on 12/24/2015.
 */
public class RestClientDropBoxClient {
    public static final String BASE_URL = "https://www.dropbox.com/1";
    public static final String BASE_URL1 = "https://api.dropbox.com/1";
    public static final String BASE_URL2 = "https://content.dropboxapi.com/1";

    private DropBoxService dropBoxServer;
    private static RestClientDropBoxClient instance;

    private RestClientDropBoxClient() {
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(BASE_URL)
                .setConverter(new JsonConverter())
                .build();

        dropBoxServer = restAdapter.create(DropBoxService.class);
    }

    public static RestClientDropBoxClient getInstance() {
        if (instance == null)
            instance = new RestClientDropBoxClient();

        return instance;
    }

    public DropBoxService getDropBoxService() {
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
