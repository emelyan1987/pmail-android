package com.planckmail.data.db.manager;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.MessageDB;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by Taras Matolinets on 24.07.15.
 */
public class MessageDataManager extends DataBaseManager {
    private final Context mContext;

    public MessageDataManager(Context ctx) {
        mContext = ctx;
    }

    public void createMessage(MessageDB m) {
        try {
            mHelper.getMessageDB().create(m);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public List<MessageDB> getMessageList() {
        List<MessageDB> listMessage = null;
        try {
            Dao<MessageDB, Integer> daoAccountInfoList = mHelper.getMessageDB();
            listMessage = daoAccountInfoList.queryForAll();

        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return listMessage;
    }

    public void updateMessage(MessageDB messageDB) {
        try {
            Dao<MessageDB, Integer> daoAccountInfoList = mHelper.getMessageDB();
            daoAccountInfoList.update(messageDB);

        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public void deleteMessageList() {
        List<MessageDB> listMessage;
        try {
            Dao<MessageDB, Integer> daoMessageList = mHelper.getMessageDB();
            listMessage = daoMessageList.queryForAll();

            daoMessageList.delete(listMessage);

        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public void deleteMessage(MessageDB messageDB) {
        try {
            Dao<MessageDB, Integer> daoMessageList = mHelper.getMessageDB();
            daoMessageList.delete(messageDB);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }
}
