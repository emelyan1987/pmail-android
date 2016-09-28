package com.planckmail.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

/**
 * Created by Taras Matolinets on 03.05.15.
 */
public class PlanckMailSharePreferences {
    private static final String PLANK_MAIL_SHARED_PREFERENCES = "com.planck.mail.shared.preferences";

    private static Context mContext;

    public static SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(PLANK_MAIL_SHARED_PREFERENCES, Context.MODE_PRIVATE);
    }

    public static void setDataToSharePreferences(String key, Object value, SHARE_PREFERENCES_TYPE type) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();

        switch (type) {
            case STRING:
                String valueString = (String) value;
                editor.putString(key, valueString);
                break;

            case INTEGER:
                Integer valueInteger = (Integer) value;
                editor.putInt(key, valueInteger);
                break;

            case BOOLEAN:
                Boolean valueBoolean = (Boolean) value;
                editor.putBoolean(key, valueBoolean);
                break;

        }

        editor.apply();
    }

    public static void setContext(Context context) {
        mContext = context;
    }


    public enum SHARE_PREFERENCES_TYPE {
        STRING, INTEGER, BOOLEAN;
    }
}
