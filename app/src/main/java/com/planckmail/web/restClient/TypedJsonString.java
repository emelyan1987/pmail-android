package com.planckmail.web.restClient;

import retrofit.mime.TypedString;

/**
 * Created by Taras Matolinets on 30.05.15.
 */

/**
 * class for convert object to raw json format
 */
public class TypedJsonString extends TypedString {
    public TypedJsonString(String body) {
        super(body);
    }

    @Override
    public String mimeType() {
        return "application/json";
    }

}