package com.planckmail.data.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by lionstar on 7/11/16.
 */
public class TrackedItem {
    private String id;
    private String trackId;
    private String actor;
    private String action;
    private String ip;
    private String location;
    private boolean isMobile;
    private Date createdTime;

    public TrackedItem() {};

    public TrackedItem(JSONObject json) {
        try {
            this.setId(json.getString("id"));

            this.trackId = json.getString("track_id");
            this.actor = json.getString("actor_email");
            this.action = json.getString("action_type");
            this.ip = json.getString("ip_address");
            this.location = json.getString("location");
            this.isMobile = !json.getString("is_mobile").equalsIgnoreCase("null") && json.getInt("is_mobile")==1 ? true : false;

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            this.setCreatedTime(dateFormat.parse(json.getString("created_time")));

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();

        try {
            json.put("id", this.getId());
            json.put("track_id", this.trackId);
            json.put("actor_email", this.actor);
            json.put("action_type", this.action);
            json.put("ip_address", this.ip);
            json.put("location", this.location);
            json.put("is_mobile", this.isMobile);
            json.put("created_time", this.getCreatedTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isMobile() {
        return isMobile;
    }

    public void setMobile(boolean mobile) {
        isMobile = mobile;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
}
