package com.planckmail.enums;

/**
 * Created by Taras Matolinets on 22.09.15.
 */
public enum AccountType {
    GMAIL("gmail"), YAHOO("yahoo"), OUTLOOK("outlook"), DROP_BOX("dropBox"),GOOGLE_DRIVE("google_drive"),
    ONE_DRIVE("one_drive"), BOX("box"), MICROSOFT_EXCHANGE("microsoft_exchange");

    private final String account;

    AccountType(String tag) {
        this.account = tag;
    }

    @Override
    public String toString() {
        return account;
    }
}
