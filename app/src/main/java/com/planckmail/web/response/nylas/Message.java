package com.planckmail.web.response.nylas;

import com.planckmail.web.response.nylas.wrapper.File;
import com.planckmail.web.response.nylas.wrapper.Participant;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taras Matolinets on 19.05.15.
 */
public class Message {
    public String id;
    public String object;
    public List<Participant> from;
    public List<Participant> replyTo;
    public List<Participant> to;
    public List<Participant> cc;
    public List<Participant> bcc;
    public long date;
    public String thread_id;
    public String account_id;

    public List<File> files = new ArrayList<>();
    public String subject;
    public String snippet;
    public String body;
    public boolean unread;

    public void setFiles(List<File> files) {
        this.files = files;
    }

}
