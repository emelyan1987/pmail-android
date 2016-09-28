package com.planckmail.data.db.beans;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Collection;
import java.util.List;

/**
 * Created by Taras Matolinets on 23.07.15.
 */
@DatabaseTable
public class MessageDB {
    public static final String OBJECT = "object";
    public static final String FROM = "from";
    public static final String REPLY_TO = "replyTo";
    public static final String TO = "to";
    public static final String CC = "cc";
    public static final String BCC = "bcc";
    public static final String DATE = "date";
    public static final String THREAD_ID = "thread_id";
    public static final String SUBJECT = "subject";
    public static final String SNIPPET = "snippet";
    public static final String BODY = "body";
    public static final String UNREAD = "unread";
    public static final String THREAD_DB = "threadDB";

    //To serialize date change field private to public
    @DatabaseField(generatedId = true)
    private int messageId;
    @DatabaseField
    public String id;
    @DatabaseField(columnName = OBJECT)
    public String object;
    @ForeignCollectionField(eager = true, columnName = FROM)
    public Collection<ParticipantDB> from;
    @ForeignCollectionField(eager = true, columnName = REPLY_TO)
    public Collection<ParticipantDB> replyTo;
    @ForeignCollectionField(eager = true, columnName = TO)
    public Collection<ParticipantDB> to;
    @ForeignCollectionField(eager = true, columnName = CC)
    public Collection<ParticipantDB> cc;
    @ForeignCollectionField(eager = true, columnName = BCC)
    public Collection<ParticipantDB> bcc;
    @DatabaseField(columnName = DATE)
    public long date;
    @DatabaseField(columnName = THREAD_ID)
    public String thread_id;
    /*    @ForeignCollectionField
        public List<FileDB> files;*/
    @DatabaseField(columnName = SUBJECT)
    public String subject;
    @DatabaseField(columnName = SNIPPET)
    public String snippet;
    @DatabaseField(columnName = BODY)
    public String body;
    @DatabaseField(columnName = UNREAD)
    public boolean unread;
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = THREAD_DB)
    private ThreadDB threadDB;

    public ThreadDB getThreadDB() {
        return threadDB;
    }

    public void setThreadDB(ThreadDB threadDB) {
        this.threadDB = threadDB;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Collection<ParticipantDB> getFrom() {
        return from;
    }

    public void setFrom(Collection<ParticipantDB> from) {
        this.from = from;
    }

    public Collection<ParticipantDB> getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(List<ParticipantDB> replyTo) {
        this.replyTo = replyTo;
    }

    public Collection<ParticipantDB> getTo() {
        return to;
    }

    public void setTo(Collection<ParticipantDB> to) {
        this.to = to;
    }

    public Collection<ParticipantDB> getCc() {
        return cc;
    }

    public void setCc(Collection<ParticipantDB> cc) {
        this.cc = cc;
    }

    public Collection<ParticipantDB> getBcc() {
        return bcc;
    }

    public void setBcc(Collection<ParticipantDB> bcc) {
        this.bcc = bcc;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getThread_id() {
        return thread_id;
    }

    public void setThread_id(String thread_id) {
        this.thread_id = thread_id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }
}
