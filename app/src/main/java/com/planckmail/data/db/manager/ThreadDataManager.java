package com.planckmail.data.db.manager;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.ThreadDB;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by Taras Matolinets on 22.07.15.
 */
public class ThreadDataManager extends DataBaseManager {
    private Context mContext;

    public ThreadDataManager(Context ctx) {
        mContext = ctx;
    }

    public List<ThreadDB> getThreadList() {
        List<ThreadDB> threadList = null;
        try {
            threadList = mHelper.getThreadDB().queryForAll();
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return threadList;
    }

    public void deleteThreadList() {
        List<ThreadDB> threadList;
        try {
            Dao<ThreadDB, Integer> daoThreadList = mHelper.getThreadDB();
            threadList = daoThreadList.queryForAll();

            daoThreadList.delete(threadList);

        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public void deleteThread(ThreadDB thread) {
        try {
            Dao<ThreadDB, Integer> daoThreadList = mHelper.getThreadDB();
            daoThreadList.delete(thread);

        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }


    public ThreadDB getThreadById(String id) {

        List<ThreadDB> listThread = getThreadList();

        for (ThreadDB thread : listThread) {
            if (thread.getId().equalsIgnoreCase(id))
                return thread;
        }

        return null;
    }

    public void deleteThreadById(ThreadDB threadDB) {
        try {
            mHelper.getThreadDB().delete(threadDB);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public void createThread(ThreadDB threadDB) {
        try {
            mHelper.getThreadDB().create(threadDB);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public void updateThreadList(ThreadDB thread) {
        ThreadDB th = getThreadById(thread.getId());

        try {
            if (th == null)
                mHelper.getThreadDB().create(thread);
            else
                mHelper.getThreadDB().update(thread);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }
}
