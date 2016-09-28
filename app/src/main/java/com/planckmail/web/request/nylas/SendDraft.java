package com.planckmail.web.request.nylas;

import com.planckmail.web.response.nylas.Contact;
import com.planckmail.web.response.nylas.wrapper.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taras Matolinets on 15.07.15.
 */
public class SendDraft {
    public String body;
    public String subject;
    public List<Contact> to;
    public List<Contact> cc;
    public List<Contact> bcc;
    public List<String> file_ids = new ArrayList<>();

    public List<String> getFile_ids() {
        return file_ids;
    }

    public void setFile_ids(List<String> file_ids) {
        this.file_ids = file_ids;
    }

    public List<Tag> tags = new ArrayList<>();

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

    public List<Contact> getCc() {
        return cc;
    }

    public void setCc(List<Contact> cc) {
        this.cc = cc;
    }

    public List<Contact> getBcc() {
        return bcc;
    }

    public void setBcc(List<Contact> bcc) {
        this.bcc = bcc;
    }
}
