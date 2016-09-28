package com.planckmail.web.response.nylas.wrapper;

/**
 * Created by Taras Matolinets on 05.06.15.
 */
public class When {
    public String object;
    public long end_time;
    public long start_time;
    public String date;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getStart_time() {
        return start_time;
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(long end_time) {
        this.end_time = end_time;
    }
}
