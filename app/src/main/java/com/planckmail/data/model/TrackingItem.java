package com.planckmail.data.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by lionstar on 7/11/16.
 */
public class TrackingItem implements Serializable{
    private String id;
    private String threadId;
    private String messagetId;
    private String subject;
    private String ownerEmail;
    private String targetEmails;
    private int opens;
    private int links;
    private int replies;
    private Date createdTime;
    private Date modifiedTime;

    public TrackingItem() {};

    public TrackingItem(JSONObject json) {
        try {
            this.id = json.getString("id");

            this.subject = json.getString("subject");
            this.ownerEmail = json.getString("owner_email");
            this.targetEmails = json.getString("target_emails");
            this.messagetId = json.getString("message_id");
            this.threadId = json.getString("thread_id");
            this.opens = !json.getString("opens").equalsIgnoreCase("null")?json.getInt("opens"):0;
            this.links = !json.getString("links").equalsIgnoreCase("null")?json.getInt("links"):0;
            this.replies = !json.getString("replies").equalsIgnoreCase("null")?json.getInt("replies"):0;

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            this.createdTime = dateFormat.parse(json.getString("created_time"));
            this.modifiedTime = dateFormat.parse(json.getString("modified_time"));

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();

        try {
            json.put("id", this.id);
            json.put("subject", this.subject);
            json.put("owner_email", this.ownerEmail);
            json.put("target_emails", this.targetEmails);
            json.put("message_id", this.messagetId);
            json.put("thread_id", this.threadId);
            json.put("opens", this.opens);
            json.put("links", this.links);
            json.put("replies", this.replies);
            json.put("created_time", this.createdTime);
            json.put("modified_time", this.modifiedTime);
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

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getMessagetId() {
        return messagetId;
    }

    public void setMessagetId(String messagetId) {
        this.messagetId = messagetId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getTargetEmails() {
        return targetEmails;
    }

    public void setTargetEmails(String targetEmails) {
        this.targetEmails = targetEmails;
    }

    public int getOpens() {
        return opens;
    }

    public void setOpens(int opens) {
        this.opens = opens;
    }

    public int getLinks() {
        return links;
    }

    public void setLinks(int links) {
        this.links = links;
    }

    public int getReplies() {
        return replies;
    }

    public void setReplies(int replies) {
        this.replies = replies;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }


}
