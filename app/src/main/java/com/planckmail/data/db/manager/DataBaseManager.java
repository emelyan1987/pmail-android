package com.planckmail.data.db.manager;

import android.content.Context;

import com.planckmail.data.db.DataBaseHelper;


/**
 * Created by Taras Matolinets on 27.02.15.
 */
public class DataBaseManager {

    static private DataBaseManager instanceDataManager;
    private Context mContext;

    private static AccountInfoDataManager instanceMailAccountData;
    private static ThreadDataManager instanceThreadManager;
    private static ParticipantDataManager instanceParticipantManager;
    private static MessageDataManager instanceMessageManager;

    protected static DataBaseHelper mHelper;

    public DataBaseManager() {
    }

    public DataBaseManager(Context ctx) {
        mContext = ctx;
        mHelper = new DataBaseHelper(ctx);
    }

    static public void init(Context ctx) {
        if (null == instanceDataManager) {
            instanceDataManager = new DataBaseManager(ctx);
        }
    }

    public DataBaseManager getCurrentManager(DataManager manager) {
        switch (manager) {
            case MAIL_ACCOUNT_MANAGER:
                return instanceMailAccountData = getAccountManager(mContext);

            case THREAD_MANAGER:
                return instanceThreadManager = getThreadManager(mContext);

            case PARTICIPANT_MANAGER:
                return instanceParticipantManager = getParticipantManager(mContext);
            case MESSAGER_MANAGER:
                return instanceMessageManager = getMessageManager(mContext);

            default:
                return instanceDataManager;
        }
    }

    private AccountInfoDataManager getAccountManager(Context ctx) {
        if (null == instanceMailAccountData) {
            return instanceMailAccountData = new AccountInfoDataManager(ctx);
        } else
            return instanceMailAccountData;
    }

    private ThreadDataManager getThreadManager(Context ctx) {
        if (null == instanceThreadManager) {
            return instanceThreadManager = new ThreadDataManager(ctx);
        } else
            return instanceThreadManager;
    }

     private ParticipantDataManager getParticipantManager(Context ctx) {
        if (null == instanceParticipantManager) {
            return instanceParticipantManager = new ParticipantDataManager(ctx);
        } else
            return instanceParticipantManager;
    }

    private MessageDataManager getMessageManager(Context ctx) {
        if (null == instanceMessageManager) {
            return instanceMessageManager = new MessageDataManager(ctx);
        } else
            return instanceMessageManager;
    }

    public void cleanTable(Class object) {
        mHelper.cleanTable(object);
    }

    static public DataBaseManager getInstanceDataManager() {
        return instanceDataManager;
    }

    private DataBaseHelper getHelper() {
        return mHelper;
    }

    public enum DataManager {
        MAIL_ACCOUNT_MANAGER, THREAD_MANAGER, PARTICIPANT_MANAGER, MESSAGER_MANAGER
    }

}
