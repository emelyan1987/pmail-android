package com.planckmail.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taras Matolinets on 19.03.2015.
 */
public class JsonUtil {

    private Gson mGson;

    public JsonUtil(Gson gson)
    {
        this.mGson = gson;
    }

    public void setGson(Gson gson) {
        this.mGson = gson;
    }

    public <T> T fromJson(String s, Class<T> c)
    {
        return mGson.fromJson(s,c);
    }
    public <T> T fromJson(String s, Type t)
    {
        return mGson.fromJson(s,t);
    }
    public <T> ArrayList<T> fromJsonArray(String s, Class<T> c){
        ObjectMapper objectMapper = new ObjectMapper();
        // this flag indicates that mapper will be ignore unknown properties
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        ArrayList<T> navigation = null;
        try {
            navigation = objectMapper.readValue(
                    s,objectMapper.getTypeFactory().constructCollectionType(List.class, c));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return navigation;
    }

    public String toJson(Object o)
    {
        return mGson.toJson(o);
    }

}
