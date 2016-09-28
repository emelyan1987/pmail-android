package com.planckmail.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.planckmail.R;
import com.planckmail.data.PlanckMailSharePreferences;
import com.planckmail.utils.BundleKeys;

/**
 * Created by Terry on 1/2/2016.
 */
public class StartTutorialActivity extends BaseActivity implements OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_tutorial);
        PlanckMailSharePreferences.setDataToSharePreferences(BundleKeys.TUTORIAL_LOADED, true, PlanckMailSharePreferences.SHARE_PREFERENCES_TYPE.BOOLEAN);

        Button buttonGetStarted = (Button) findViewById(R.id.btGetStarted);
        buttonGetStarted.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btGetStarted:
                Intent intent = new Intent(this, TutorialActivity.class);
                startActivity(intent);
                break;
        }
    }
}
