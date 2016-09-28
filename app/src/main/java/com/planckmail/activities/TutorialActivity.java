package com.planckmail.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.adapters.TutorialIndicatorAdapter;
import com.viewpagerindicator.CirclePageIndicator;

/**
 * Created by Terry on 1/2/2016.
 */
public class TutorialActivity extends BaseActivity implements OnClickListener, OnPageChangeListener {

    private TextView mTextSkip;
    private CirclePageIndicator mIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutotial);
        initViews();
        setViewPager();
    }

    private void initViews() {
        mTextSkip = (TextView) findViewById(R.id.tvSkip);
        mTextSkip.setOnClickListener(this);
    }

    private void setViewPager() {
        ViewPager pagerTutorial = (ViewPager) findViewById(R.id.pagerTutorial);
        pagerTutorial.setAdapter(new TutorialIndicatorAdapter(this, getFragmentManager()));
        mIndicator = (CirclePageIndicator) findViewById(R.id.indicatorTutorial);
        mIndicator.setViewPager(pagerTutorial);
        mIndicator.setOnPageChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvSkip:
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mIndicator.setFillColor(R.color.gray_light);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
