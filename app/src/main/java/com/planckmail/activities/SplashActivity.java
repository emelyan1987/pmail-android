package com.planckmail.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.planckmail.R;
import com.planckmail.data.PlanckMailSharePreferences;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;

import java.util.List;

/**
 * Created by Taras Matolinets on 25.08.15.
 */
public class SplashActivity extends BaseActivity {

    private static final int SPLASH_TIME_OUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        boolean isLoaded = PlanckMailSharePreferences.getSharedPreferences().getBoolean(BundleKeys.SPLASH_LOADED, false);
        if (!isLoaded)
            loadSplash();
        else {
             List<AccountInfo> list = UserHelper.getEmailAccountList(true);
            if (!list.isEmpty()) {
                Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                startActivity(intent);
                finish();
            } else
                loadSplash();
        }
    }

    private void loadSplash() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean isLoadedTutorial = PlanckMailSharePreferences.getSharedPreferences().getBoolean(BundleKeys.TUTORIAL_LOADED, false);

                Intent i;
                if (!isLoadedTutorial)
                    i = new Intent(SplashActivity.this, StartTutorialActivity.class);
                else
                    i = new Intent(SplashActivity.this, LoginActivity.class);

                startActivity(i);

                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
