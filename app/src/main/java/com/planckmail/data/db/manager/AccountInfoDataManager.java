package com.planckmail.data.db.manager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.enums.AccountType;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Taras Matolinets on 29.03.15.
 */
public class AccountInfoDataManager extends DataBaseManager {
    public static final String IS_EMAIL_ACCOUNT = "isEmailAccount";
    private Context mContext;

    public AccountInfoDataManager(Context ctx) {
        mContext = ctx;
    }

    public List<AccountInfo> getEmailAccountInfoList(boolean isEmail) {
        List<AccountInfo> accountInfoList = null;
        try {
            QueryBuilder<AccountInfo, Integer> queryBuilder = mHelper.getAllAccountsInfoDao().queryBuilder();
            Where<AccountInfo, Integer> where = queryBuilder.where();
            accountInfoList = where.eq(IS_EMAIL_ACCOUNT, isEmail).query();
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return accountInfoList;
    }

    public AccountInfo getEmailAccountInfoByEmail(String email) {
        List<AccountInfo> accountInfoList = getEmailAccountInfoList(true);

        Iterator<AccountInfo> iter = accountInfoList.iterator();
        while(iter.hasNext()) {
            AccountInfo info = iter.next();

            if(info.getEmail().equalsIgnoreCase(email))
                return info;
        }

        return null;
    }

    public boolean ifAccountExist(String name) {
        try {
            QueryBuilder<AccountInfo, Integer> queryBuilder = mHelper.getAllAccountsInfoDao().queryBuilder();
            Where<AccountInfo, Integer> where = queryBuilder.where();
            where.eq(AccountInfo.EMAIL, name);
            where.and();
            where.eq(AccountInfo.IS_EMAIL_ACCOUNT, false);
            List<AccountInfo> accountInfoList = where.query();

            if (accountInfoList.isEmpty())
                return false;
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return true;
    }

    public List<AccountInfo> getAllAccountInfoList() {
        List<AccountInfo> accountInfoList = null;
        try {
            accountInfoList = mHelper.getAccountInfoDao().queryForAll();
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return accountInfoList;
    }

    public void deleteAccountInfosList() {
        List<AccountInfo> accountInfoList;
        try {
            Dao<AccountInfo, Integer> daoAccountInfoList = mHelper.getAccountInfoDao();
            accountInfoList = daoAccountInfoList.queryForAll();

            daoAccountInfoList.delete(accountInfoList);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public void deleteAccountInfo(AccountInfo account) {
        try {
            Dao<AccountInfo, Integer> daoAccountInfoList = mHelper.getAccountInfoDao();
            daoAccountInfoList.delete(account);

        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public void deleteAccountInfoById(int id) {
        try {
            mHelper.getAccountInfoDao().deleteById(id);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public AccountInfo getAccountInfoByPosition(int id) {
        AccountInfo accountInfo = null;
        try {
            List<AccountInfo> AccountInfoList = mHelper.getAccountInfoDao().queryForAll();

            accountInfo = AccountInfoList.get(id);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return accountInfo;
    }

    public AccountInfo getAccountInfoById(String id) {
        try {
            QueryBuilder<AccountInfo, Integer> queryBuilder = mHelper.getAccountInfoDao().queryBuilder();
            return queryBuilder.where().eq(AccountInfo.ACCOUNT_ID, id).queryForFirst();
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return null;
    }

    public AccountInfo getAccountInfoByAccessToken(String token) {
        try {
            QueryBuilder<AccountInfo, Integer> queryBuilder = mHelper.getAccountInfoDao().queryBuilder();
            return queryBuilder.where().eq(AccountInfo.ACCESS_TOKEN, token).queryForFirst();
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return null;
    }

    public AccountInfo getAccountInfoByType(AccountType type) {
        try {
            QueryBuilder<AccountInfo, Integer> queryBuilder = mHelper.getAccountInfoDao().queryBuilder();
            return queryBuilder.where().eq(AccountInfo.ACCOUNT_TYPE, type).queryForFirst();
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return null;
    }

    public boolean createOrUpdateAccountInfo(AccountInfo accountInfo) {
        try {
            mHelper.getAccountInfoDao().createOrUpdate(accountInfo);

        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return false;
    }

    public void createAccountInfo(AccountInfo accountInfo) {
        try {
            mHelper.getAccountInfoDao().create(accountInfo);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public void updateAccountInfoList(AccountInfo AccountInfo) {
        try {
            mHelper.getAccountInfoDao().update(AccountInfo);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }
}
