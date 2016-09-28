package com.planckmail.data.db.beans;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Taras Matolinets on 22.07.15.
 */
@DatabaseTable
public class ThreadDB {
    public static final String OBJECT = "object";
    public static final String ACCOUNT_ID = "from";
    public static final String SUBJECT = "subject";
    public static final String MESSAGE_BODY_LIST = "message_body_list";
    public static final String VERSION = "version";
    public static final String LAST_MESSAGE_TIMESTAMP = "last_message_timestamp";
    public static final String FIRST_MESSAGE_TIMESTAMP = "first_message_timestamp";
    public static final String SNIPPET = "snippet";
    public static final String PARTICIPANTS = "participants";
    public static final String THREAD_ID = "threadId";

    //Gson don't serialize private fields
    @DatabaseField(generatedId = true, columnName = THREAD_ID)
    private int threadId;
    @DatabaseField
    public String id;
    @DatabaseField(columnName = OBJECT)
    public String object;
    @DatabaseField(columnName = ACCOUNT_ID)
    public String account_id;
    @DatabaseField(columnName = SUBJECT)
    public String subject;
    @DatabaseField(columnName = LAST_MESSAGE_TIMESTAMP)
    public long last_message_timestamp;
    @DatabaseField(columnName = FIRST_MESSAGE_TIMESTAMP)
    public long first_message_timestamp;
    @DatabaseField(columnName = SNIPPET)
    public String snippet;
    @ForeignCollectionField(eager = true, columnName = PARTICIPANTS)
    public Collection<ParticipantDB> participants = new ArrayList<>();
    @ForeignCollectionField(eager = true, columnName = MESSAGE_BODY_LIST)
    public Collection<MessageDB> message_body_list = new ArrayList<>();
    @DatabaseField(columnName = VERSION)
    public int version;

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

    public String getAccount_id() {
        return account_id;
    }

    public void setAccount_id(String account_id) {
        this.account_id = account_id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Collection<ParticipantDB> getParticipants() {
        return participants;
    }

    public void setParticipants(Collection<ParticipantDB> participants) {
        this.participants = participants;
    }

    public Collection<MessageDB> getMessageList() {
        return message_body_list;
    }

    public void setMessageList(Collection<MessageDB> message_ids) {
        this.message_body_list = message_ids;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

}
