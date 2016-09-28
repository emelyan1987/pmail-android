package com.planckmail.web.restClient;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.planckmail.web.restClient.service.PlanckService;

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
 * Created by Taras Matolinets on 18.08.15.
 */
public class RestPlankMail {
    public static final String BASE_URL1 = "http://planckapi-test.elasticbeanstalk.com";
    public static final String BASE_URL2 = "http://planckapi-dev.elasticbeanstalk.com";
    public static final String BASE_URL3 = "http://planckapi-prioritizer.us-west-1.elasticbeanstalk.com";
    public static final String BASE_URL4 = "http://ec2-54-200-236-238.us-west-2.compute.amazonaws.com/planckmail/web/track";


    public final static String TrackingListFilterTimeLast31 = "last31";
    public final static String TrackingListFilterTimeLast7 = "last7";
    public final static String TrackingListFilterTimeToday = "today";

    public final static String TrackingListFilterStatusOpened = "opened";
    public final static String TrackingListFilterStatusUnopened = "unopened";


    private PlanckService devServer;
    private static RestPlankMail instance;

    public RestPlankMail(String url) {
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(url)
                .setConverter(new JsonConverter())
                .build();

        devServer = restAdapter.create(PlanckService.class);
    }

    public PlanckService getPlankService() {
        return devServer;
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
