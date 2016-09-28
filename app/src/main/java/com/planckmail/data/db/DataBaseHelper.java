package com.planckmail.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.beans.MessageDB;
import com.planckmail.data.db.beans.ParticipantDB;
import com.planckmail.data.db.beans.ThreadDB;
import com.planckmail.data.db.dao.ThreadDao;
import com.planckmail.data.db.manager.DataBaseManager;

import java.sql.SQLException;

/**
 * Created by Taras Matolinets on 26.02.15.
 */
public class DataBaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String DB_NAME = "planck_mail_db";
    private static final int DB_VERSION = 1;

    private Context mContext;
    private Dao<AccountInfo, Integer> listAccountInfo = null;
    private Dao<ThreadDB, Integer> listThreadDb = null;
    private Dao<ParticipantDB, Integer> listParticipantDb = null;
    private Dao<MessageDB, Integer> listMessageDb = null;
    private Dao<AccountInfo, Integer> listAccountInfoFile = null;

    public DataBaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            Log.i(PlanckMailApplication.TAG, "onCreate");

            TableUtils.createTable(connectionSource, AccountInfo.class);
            TableUtils.createTable(connectionSource, ThreadDB.class);
            TableUtils.createTable(connectionSource, ParticipantDB.class);
            TableUtils.createTable(connectionSource, MessageDB.class);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int i, int i2) {
        try {
            Log.i(PlanckMailApplication.TAG, "onUpgrade");

            TableUtils.dropTable(connectionSource, AccountInfo.class, true);
            TableUtils.dropTable(connectionSource, ThreadDB.class, true);
            TableUtils.dropTable(connectionSource, ParticipantDB.class, true);
            TableUtils.dropTable(connectionSource, MessageDB.class, true);

            onCreate(sqLiteDatabase, connectionSource);

        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    public void cleanTable(Class object)
    {
        try {
            TableUtils.clearTable(getConnectionSource(), object);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, "Can't clean table", e);
        }
    }

    public Dao<AccountInfo, Integer> getAccountInfoDao() {
        if (null == listAccountInfo) {
            try {
                listAccountInfo = getDao(AccountInfo.class);
            } catch (SQLException e) {
                Log.e(PlanckMailApplication.TAG, e.toString());
            }
        }
        return listAccountInfo;
    }

    public Dao<AccountInfo, Integer> getAllAccountsInfoDao() {
        if (null == listAccountInfoFile) {
            try {
                listAccountInfoFile = getDao(AccountInfo.class);
            } catch (SQLException e) {
                Log.e(PlanckMailApplication.TAG, e.toString());
            }
        }
        return listAccountInfoFile;
    }

    public Dao<ThreadDB, Integer> getThreadDB() {
        if (null == listThreadDb) {
            try {
                listThreadDb = new ThreadDao(getConnectionSource(),ThreadDB.class);
            } catch (SQLException e) {
                Log.e(PlanckMailApplication.TAG, e.toString());
            }
        }
        return listThreadDb;
    }

    public Dao<ParticipantDB, Integer> getParticipantDB() {
        if (null == listParticipantDb) {
            try {
                listParticipantDb = getDao(ParticipantDB.class);
            } catch (SQLException e) {
                Log.e(PlanckMailApplication.TAG, e.toString());
            }
        }
        return listParticipantDb;
    }

    public Dao<MessageDB, Integer> getMessageDB() {
        if (null == listMessageDb) {
            try {
                listMessageDb = getDao(MessageDB.class);
            } catch (SQLException e) {
                Log.e(PlanckMailApplication.TAG, e.toString());
            }
        }
        return listMessageDb;
    }

    public static  void cleanDb() {
        DataBaseManager.getInstanceDataManager().cleanTable(ThreadDB.class);
        DataBaseManager.getInstanceDataManager().cleanTable(MessageDB.class);
        DataBaseManager.getInstanceDataManager().cleanTable(ParticipantDB.class);
    }
}
