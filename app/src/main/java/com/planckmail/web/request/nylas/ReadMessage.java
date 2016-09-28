package com.planckmail.web.request.nylas;

/**
 * Created by Taras Matolinets on 29.05.15.
 */
public class ReadMessage {
    public void setRead(boolean read) {
        this.unread = read;
    }

    public boolean unread;
}
