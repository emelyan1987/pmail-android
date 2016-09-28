package com.planckmail.web.response.nylas;

import com.planckmail.web.response.nylas.wrapper.Folder;
import com.planckmail.web.response.nylas.wrapper.Participant;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taras Matolinets on 07.05.15.
 */

public class Thread {
    public String id;
    public String object;
    public String account_id;
    public String subject;
    public long last_message_timestamp;
    public long first_message_timestamp;
    public long received_recent_date;
    public List<Participant> participants = new ArrayList<>();
    public String snippet;
    public List<String> message_ids = new ArrayList<>();
    public ArrayList<String> draft_ids = new ArrayList<>();
    public int version;
    public List<Folder> labels = new ArrayList<>();
    public List<Folder> folders = new ArrayList<>();
    private List<Message> listMessage = new ArrayList<>();
    public boolean has_attachments;
    public boolean unread;
    public boolean starred;

    public List<Message> getListMessage() {
        return listMessage;
    }

    public void setListMessage(List<Message> listMessage) {
        this.listMessage = listMessage;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

}
