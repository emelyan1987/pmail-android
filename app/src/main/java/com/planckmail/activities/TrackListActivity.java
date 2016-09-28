package com.planckmail.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.planckmail.R;
import com.planckmail.adapters.SpinnerSelectAccount;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.fragments.TrackingListFragment;
import com.planckmail.listeners.OnTabPageChangeListener;
import com.planckmail.views.MainTabLayout;
import com.planckmail.web.restClient.RestPlankMail;

import java.util.ArrayList;
import java.util.List;



public class TrackListActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private List<AccountInfo> mListAccountInfo;

    private ViewPagerAdapter mViewPageAdapter;

    private Toolbar mToolbar;
    private MainTabLayout mTabLayout;
    private ViewPager mViewPager;
    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_list);

        mToolbar = (Toolbar) findViewById(R.id.track_toolbar);
        mSpinner = (Spinner) findViewById(R.id.track_spinner_accounts);

        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        mListAccountInfo = manager.getEmailAccountInfoList(true);

        SpinnerSelectAccount account = new SpinnerSelectAccount(this, mListAccountInfo);
        mSpinner.setAdapter(account);
        mSpinner.setSelection(0);
        mSpinner.setOnItemSelectedListener(this);

        setSupportActionBar(mToolbar);

        setActionBar();

        mViewPager = (ViewPager) findViewById(R.id.track_viewpager);
        setupViewPager(mViewPager);

        mTabLayout = (MainTabLayout) findViewById(R.id.track_tablayout);
        mTabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new OnTabPageChangeListener(mTabLayout));

    }

    private void setActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.track));
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    private void setupViewPager(ViewPager viewPager) {
        mViewPageAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        AccountInfo accountInfo = mListAccountInfo.get(0);
        mViewPageAdapter.addFragment(new TrackingListFragment(accountInfo, RestPlankMail.TrackingListFilterTimeToday), "TODAY");
        mViewPageAdapter.addFragment(new TrackingListFragment(accountInfo, RestPlankMail.TrackingListFilterTimeLast7), "LAST 7 DAYS");
        mViewPageAdapter.addFragment(new TrackingListFragment(accountInfo, RestPlankMail.TrackingListFilterTimeLast31), "LAST 31 DAYS");
        viewPager.setAdapter(mViewPageAdapter);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        AccountInfo accountInfo = mListAccountInfo.get(position);
        TrackingListFragment fragment = (TrackingListFragment) mViewPageAdapter.getFragmentByPosition(mViewPager.getCurrentItem());

        fragment.changeAccount(accountInfo);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

        public Fragment getFragmentByPosition(int position) {
            return mFragmentList.get(position);
        }
    }
}
