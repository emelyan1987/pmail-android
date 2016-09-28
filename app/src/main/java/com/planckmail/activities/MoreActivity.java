package com.planckmail.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.View.OnClickListener;

import com.planckmail.R;

/**
 * Created by Terry on 3/13/2016.
 */
public class MoreActivity extends BaseActivity implements OnClickListener {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.more_activity);

        initViews();
    }

    private void initViews() {
        CardView tvSetting = (CardView) findViewById(R.id.cardSettings);
        CardView tvUnsubscribe = (CardView) findViewById(R.id.cardUnsubscribe);
        CardView tvTrack = (CardView) findViewById(R.id.cardTrack);

        tvSetting.setOnClickListener(this);
        tvUnsubscribe.setOnClickListener(this);
        tvTrack.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cardSettings: {
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
            }
                break;
            case R.id.cardUnsubscribe: {
                Intent intent = new Intent(this, UnsubscribeListActivity.class);
                startActivity(intent);
            }
                break;
            case R.id.cardTrack: {
                Intent intent = new Intent(this, TrackListActivity.class);
                startActivity(intent);
            }
                break;
        }
    }
}
