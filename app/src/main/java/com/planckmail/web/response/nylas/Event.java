package com.planckmail.web.response.nylas;

import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.response.nylas.wrapper.Recurrence;
import com.planckmail.web.response.nylas.wrapper.When;

import java.util.List;

/**
 * Created by Taras Matolinets on 05.06.15.
 */
public class Event {
    public String object;
    public String id;
    public String calendar_id;
    public String account_id;
    public String message_id;
    public String description;
    public String location;
    public List<Participant> participants;
    public boolean read_only;
    public String title;
    public String event;
    public String owner;
    public When when;
    public boolean busy;
    public String status;
    public Recurrence recurrence;
    public int colorEvent;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCalendar_id() {
        return calendar_id;
    }

    public void setCalendar_id(String calendar_id) {
        this.calendar_id = calendar_id;
    }

    public String getAccount_id() {
        return account_id;
    }

    public void setAccount_id(String account_id) {
        this.account_id = account_id;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    public boolean isRead_only() {
        return read_only;
    }

    public void setRead_only(boolean read_only) {
        this.read_only = read_only;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public int getColorEvent() {
        return colorEvent;
    }

    public void setColorEvent(int colorEvent) {
        this.colorEvent = colorEvent;
    }

    public When getWhen() {
        return when;
    }

    public void setWhen(When when) {
        this.when = when;
    }

    public enum CALENDAR_DATE {
        DATE("date"), TIME("time"), TIME_SNAP("timespan"), DATE_SNAP("datesnap");
        private String date;

        CALENDAR_DATE(String date) {
            this.date = date;
        }

        public String toString() {
            return date;
        }
    }
}
