package com.planckmail.web.response.nylas;

import android.support.annotation.Nullable;

/**
 * Created by Taras Matolinets on 25.05.15.
 */
public class Contact implements Comparable<Contact> {

    public String name;
    public String email;
    public String id;
    public String account_id;
    public String object;
    public String address;
    public String phone;
    private int color;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccount_id() {
        return account_id;
    }

    public void setAccount_id(String account_id) {
        this.account_id = account_id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    @Override
    public int compareTo(@Nullable Contact another) {
        int comp = 0;

        if (another == null)
            return comp;

        if (getName() != null && another.getName() != null)
            comp = getName().compareTo(another.getName());
        else if (getEmail() != null && another.getEmail() != null)
            comp = getEmail().compareTo(another.getEmail());

        return comp;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
