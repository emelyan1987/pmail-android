package com.planckmail.web.response.nylas;

import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.response.nylas.wrapper.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taras Matolinets on 14.07.15.
 */
public class Draft {
    public String id;
    public String subject;
    public String object;
    public String account_id;
    public String thread_id;
    public long last_message_timestamp;
    public long first_message_timestamp;
    public String snippet;
    public List<Participant> participants;
    public List<String> file_ids = new ArrayList<>();
    public int version;
    public List<Tag> tags = new ArrayList<>();
    public List<String> message_ids;
    public List<Contact> to;
    public List<Contact> cc;
    public List<Contact> bcc;
    public String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<Contact> getTo() {
        return to;
    }

    public void setTo(List<Contact> to) {
        this.to = to;
    }

    public List<Contact> getBcc() {
        return bcc;
    }

    public void setBcc(List<Contact> bcc) {
        this.bcc = bcc;
    }

    public List<Contact> getCc() {
        return cc;
    }

    public void setCc(List<Contact> cc) {
        this.cc = cc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<String> getFile_ids() {
        return file_ids;
    }

    public void setFile_ids(List<String> file_ids) {
        this.file_ids = file_ids;
    }

    public List<String> getFiles() {
        return file_ids;
    }

    public void setFiles(List<String> files) {
        this.file_ids = files;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

}
