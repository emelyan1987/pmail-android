package com.planckmail.data.db.beans;

import android.support.annotation.NonNull;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.planckmail.enums.AccountType;

import java.util.Comparator;

/**
 * Created by Taras Matolinets on 07.05.15.
 */
@DatabaseTable
public class AccountInfo{
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String OBJECT = "object";
    public static final String EMAIL = "email";
    public static final String ACCOUNT_ID = "accountId";
    public static final String NAME = "name";
    public static final String PROVIDER = "provider";
    public static final String CALENDAR_COLOR = "calendarColor";
    public static final String IS_EMAIL_ACCOUNT = "isEmailAccount";
    public static final String ACCOUNT_TYPE = "accountType";

    @DatabaseField(generatedId = true)
    public int id;
    @DatabaseField(columnName = ACCESS_TOKEN)
    public String accessToken;
    @DatabaseField(columnName = REFRESH_TOKEN)
    public String refreshToken;
    @DatabaseField(columnName = OBJECT)
    public String object;
    @DatabaseField(columnName = ACCOUNT_ID)
    public String accountId;
    @DatabaseField(columnName = EMAIL)
    public String email;
    @DatabaseField(columnName = NAME)
    public String name;
    @DatabaseField(columnName = PROVIDER)
    public String provider;
    @DatabaseField(columnName = CALENDAR_COLOR)
    public int calendarColor;
    @DatabaseField(columnName = IS_EMAIL_ACCOUNT)
    public boolean isEmailAccount;
    @DatabaseField(columnName = ACCOUNT_TYPE)
    public AccountType accountType;

    public String getRefreshToken() {
        return refreshToken;
    }

    public AccountInfo setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountInfo setAccountType(AccountType accountType) {
        this.accountType = accountType;
        return this;
    }

    public int getCalendarColor() {
        return calendarColor;
    }

    public AccountInfo setCalendarColor(int calendarColor) {
        this.calendarColor = calendarColor;
        return this;
    }

    public boolean isEmailAccount() {
        return isEmailAccount;
    }

    public AccountInfo setIsEmailAccount(boolean isEmailAccount) {
        this.isEmailAccount = isEmailAccount;
        return this;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
