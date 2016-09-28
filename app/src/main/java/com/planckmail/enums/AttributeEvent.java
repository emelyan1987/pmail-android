package com.planckmail.enums;

/**
 * Created by Taras Matolinets on 10.07.15.
 */
public enum AttributeEvent {
    CREATE("create"), MODIFY("modify"), DELETE("delete");

    private final String event;

    AttributeEvent(String event) {
        this.event = event;
    }

    @Override
    public String toString() {
        return event;
    }

}
