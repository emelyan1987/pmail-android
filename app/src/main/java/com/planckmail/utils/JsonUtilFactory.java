package com.planckmail.utils;

import android.content.Context;

import com.google.gson.Gson;

/**
 * Created by Taras Matolinets on 09.04.15.
 */
public class JsonUtilFactory {
    private static JsonUtil mJsonUtil;
    private static Gson mGson;

    private static Context mContext;

    public static void initUtils(Context applicationContext){
        mContext = applicationContext;
        setGson(new Gson());
        setJsonUtil(new JsonUtil(mGson));
    }

    public static JsonUtil getJsonUtil() {
        return mJsonUtil;
    }

    public static void setJsonUtil(JsonUtil jsonUtil) {
        mJsonUtil = jsonUtil;
    }

    public static Gson getGson() {
        return mGson;
    }

    public static void setGson(Gson gson) {
        mGson = gson;
    }

    public static void releaseUtils(){
        setGson(null);
        setJsonUtil(null);
    }
}
