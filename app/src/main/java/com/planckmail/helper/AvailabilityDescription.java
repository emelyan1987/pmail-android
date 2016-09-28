package com.planckmail.helper;

/**
 * Created by Terry on 12/13/2015.
 */
public class AvailabilityDescription {
    private String location;
    private String sender;
    private String participants;
    private long startTime;
    private long endTime;

    public String getLocation() {
        return location;
    }

    public AvailabilityDescription setLocation(String location) {
        this.location = location;
        return this;
    }

    public String getSender() {
        return sender;
    }

    public AvailabilityDescription setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getParticipants() {
        return participants;
    }

    public AvailabilityDescription setParticipants(String participants) {
        this.participants = participants;
        return this;
    }

    public long getStartTime() {
        return startTime;
    }

    public AvailabilityDescription setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    public long getEndTime() {
        return endTime;
    }

    public AvailabilityDescription setEndTime(long endTime) {
        this.endTime = endTime;
        return this;
    }
}
