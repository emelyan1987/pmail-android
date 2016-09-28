package com.planckmail.data.db.dao;

import android.util.Log;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.ThreadDB;

import java.sql.SQLException;

/**
 * Created by Taras Matolinets on 31.07.15.
 */
public class ThreadDao extends BaseDaoImpl<ThreadDB, Integer> {

    public ThreadDao(ConnectionSource connectionSource, Class<ThreadDB> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    @Override
    public int delete(ThreadDB data) throws SQLException {
        DeleteBuilder deleteBuilder = deleteBuilder();
        try {
            deleteBuilder.where().eq(ThreadDB.ACCOUNT_ID, data.getAccount_id());
            deleteBuilder.delete();
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return super.delete(data);
    }
}
