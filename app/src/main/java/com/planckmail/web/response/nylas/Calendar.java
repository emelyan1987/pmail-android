package com.planckmail.web.response.nylas;

/**
 * Created by Taras Matolinets on 05.06.15.
 */
public class Calendar {
    public String description;
    public String id;
    public String name;
    public String account_id;
    public String object;
    public boolean read_only;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
