package com.planckmail.web.restClient.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import retrofit.http.RestMethod;

/**
 * Created by Taras Matolinets on 17.07.15.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RestMethod(value = "DELETE", hasBody = true)

public @interface DELETEDRAFT {
    String value();
}
