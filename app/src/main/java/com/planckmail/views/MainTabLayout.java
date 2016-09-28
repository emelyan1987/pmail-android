package com.planckmail.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

import com.planckmail.R;

public class MainTabLayout extends TabLayout {
    public MainTabLayout(Context context) {
        super(context);

    }


    public MainTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MainTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTabsFromPagerAdapter(@NonNull PagerAdapter adapter) {
        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/Helvetica.ttf");

        this.removeAllTabs();

        ViewGroup slidingTabStrip = (ViewGroup) getChildAt(0);

        for (int i = 0, count = adapter.getCount(); i < count; i++) {
            Tab tab = this.newTab();
            this.addTab(tab.setText(adapter.getPageTitle(i)));
            TextView view = (TextView) ((ViewGroup)slidingTabStrip.getChildAt(i)).getChildAt(1);

            if(i==0) {
                view.setTypeface(typeface, Typeface.BOLD);
                view.setTextColor(Color.parseColor("#30C4B4"));
            } else {
                view.setTypeface(typeface, Typeface.NORMAL);
                view.setTextColor(Color.parseColor("#9F9F9F"));
            }
        }
    }
}