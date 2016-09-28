package com.planckmail.listeners;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.widget.TextView;

import com.planckmail.R;

/**
 * Created by lionstar on 7/24/16.
 */
public class OnTabPageChangeListener implements ViewPager.OnPageChangeListener {

    private TabLayout mTabLayout;

    public OnTabPageChangeListener(TabLayout tabLayout) {
        mTabLayout = tabLayout;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        ViewGroup slidingTabStrip = (ViewGroup) mTabLayout.getChildAt(0);
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            TextView view = (TextView) ((ViewGroup)slidingTabStrip.getChildAt(i)).getChildAt(1);
            if(view != null) {

                if(i == position) {
                    view.setTypeface(null, Typeface.BOLD);
                    view.setTextColor(Color.parseColor("#30C4B4"));
                } else {
                    view.setTypeface(null, Typeface.NORMAL);
                    view.setTextColor(Color.parseColor("#9F9F9F"));
                }
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
