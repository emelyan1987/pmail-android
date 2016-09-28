package com.planckmail.application;

import android.app.Application;
import android.os.Environment;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import com.planckmail.data.PlanckMailSharePreferences;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;

import net.danlew.android.joda.JodaTimeAndroid;

import java.io.File;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Taras Matolinets on 02.05.15.
 */
public class PlanckMailApplication extends Application {
    public static final String PLANCK_SERVER = "http://www.plancklabs.com";

    public static final String TAG = "com.plank.mail";

    public static final String PLANK_MAIL_FOLDER = File.separator + "com.plank.mail";
    public static final String PLANK_MAIL_PHOTOS = PLANK_MAIL_FOLDER + File.separator + "photos";
    public static final String PLANK_MAIL_FILES = PLANK_MAIL_FOLDER + File.separator + "files";
    public static final int ALARM_PENDING_CODE = 5;
    private boolean mLoadDB = true;
    private boolean mSendToken = true;

    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(getApplicationContext());

        Fabric.with(this, new Crashlytics());
        JodaTimeAndroid.init(this);
        DataBaseManager.init(this);
        JsonUtilFactory.initUtils(getApplicationContext());
        PlanckMailSharePreferences.setContext(getApplicationContext());

        PlanckMailSharePreferences.setDataToSharePreferences(BundleKeys.SAVE_DATA_IN_DB, true, PlanckMailSharePreferences.SHARE_PREFERENCES_TYPE.BOOLEAN);

        createFolder(PLANK_MAIL_PHOTOS);
        createFolder(PLANK_MAIL_FILES);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        JsonUtilFactory.releaseUtils();
    }

    private void createFolder(String folderPath) {
        File f = new File(Environment.getExternalStorageDirectory(), folderPath);
        if (!f.exists()) {
            f.mkdirs();
        }
    }


    public boolean getLoadDB() {
        return mLoadDB;
    }

    public void setLoadDB(boolean mLoadDB) {
        this.mLoadDB = mLoadDB;
    }


    public boolean isSentToken() {
        return mSendToken;
    }

    public void setSendToken(boolean mSendToken) {
        this.mSendToken = mSendToken;
    }

}
