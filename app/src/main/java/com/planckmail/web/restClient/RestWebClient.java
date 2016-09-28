package com.planckmail.web.restClient;

import com.planckmail.web.restClient.service.NylasService;

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
import retrofit.mime.TypedString;

/**
 * Created by Taras Matolinets on 03.05.15.
 */
public class RestWebClient {
    //public static final String BASE_URL = "https://api.nylas.com";
    public static final String BASE_URL = "https://sync-dev.planckapi.com";
    private NylasService nylasServer;

    public RestWebClient() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(BASE_URL)
                .setConverter(new JsonConverter())
                .build();

        nylasServer = restAdapter.create(NylasService.class);
    }

    public NylasService getNylasService() {
        return nylasServer;
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
        public TypedOutput toBody(Object object) {
            return new TypedString((String) object);
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
