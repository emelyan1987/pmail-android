package com.planckmail.data.dao;

/**
 * Created by Terry on 12/7/2015.
 */
public class AutoCompletePlace {
    private String address;
    private String name;

    public AutoCompletePlace(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String name) {
        this.name = name;
    }

    public String getCity() {
        return address;
    }

    public void setId(String address) {
        this.address = address;
    }
}
