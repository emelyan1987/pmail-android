package com.planckmail.enums;

/**
 * Created by Taras Matolinets on 12.05.15.
 */
public enum Folders {

    IN("in"),INBOX("inbox"),ALL_MAIL("all"),DRAFTS("drafts"),SENT("sent"),SPAM("spam"),SOCIAL("Social"),
    STARRED("starred"),UNREAD("unread"), TRASH("trash"),ATTACHMENT("attachment"),UNSEEN("unseen"), READ_NOW("Read Now"),READ_LATER("Read Later"),FOLLOW_UP("FollowUp"),SENT_MAIL("Sent Mail");

    private final String folder;
    Folders(String tag) {
        this.folder = tag;
    }

    @Override
    public String toString() {
        return folder;
    }
}
