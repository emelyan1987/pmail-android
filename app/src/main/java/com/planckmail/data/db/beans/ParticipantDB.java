package com.planckmail.data.db.beans;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by Taras Matolinets on 23.07.15.
 */
@DatabaseTable
public class ParticipantDB {
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String STATUS = "status";
    public static final String THREAD_DB = "threadDB";
    public static final String MESSAGE_DB = "messageDB";

    //To serialize date change field private to public
    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField( columnName = NAME)
    public String name;
    @DatabaseField(columnName = EMAIL)
    public String email;
    @DatabaseField(columnName = STATUS)
    public String status;
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = THREAD_DB)
    private ThreadDB threadDB;
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = MESSAGE_DB)
    private MessageDB messageDB;

    public ThreadDB getThreadDB() {
        return threadDB;
    }

    public void setThreadDB(ThreadDB threadDB) {
        this.threadDB = threadDB;
    }

    public MessageDB getMessageDB() {
        return messageDB;
    }

    public void setMessageDB(MessageDB messageDB) {
        this.messageDB = messageDB;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
