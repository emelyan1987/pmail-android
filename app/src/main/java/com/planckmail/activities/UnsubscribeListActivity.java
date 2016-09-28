package com.planckmail.activities;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

import com.planckmail.R;
import com.planckmail.adapters.SpinnerSelectAccount;
import com.planckmail.adapters.UnsubscribePagerAdapter;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.fragments.ArchiveSpammerFragment;
import com.planckmail.fragments.BlockedFragment;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;

import java.util.List;

/**
 * Created by Terry on 3/13/2016.
 */
public class UnsubscribeListActivity extends BaseActivity implements OnItemSelectedListener, OnPageChangeListener {
    private ViewPager mViewPager;
    private UnsubscribePagerAdapter mAdapter;
    private List<AccountInfo> mListAccountInfo;
    private boolean mSelectAll;
    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unsubscribe);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarUnsubscribe);
        mSpinner = (Spinner) findViewById(R.id.spinner_account);

        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        mListAccountInfo = manager.getEmailAccountInfoList(true);

        SpinnerSelectAccount account = new SpinnerSelectAccount(this, mListAccountInfo);
        mSpinner.setAdapter(account);
        mSpinner.setSelection(0);
        mSpinner.setOnItemSelectedListener(this);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mViewPager = (ViewPager) findViewById(R.id.viewpagerUnsubscribe);
        mAdapter = new UnsubscribePagerAdapter(getFragmentManager());

        AccountInfo defaultAccountInfo = mListAccountInfo.get(0);
        String jsonAccountInfo = JsonUtilFactory.getJsonUtil().toJson(defaultAccountInfo);

        Bundle bundle = new Bundle();
        bundle.putString(BundleKeys.ACCOUNT, jsonAccountInfo);

        ArchiveSpammerFragment spammerFragment = new ArchiveSpammerFragment();
        spammerFragment.setArguments(bundle);

        BlockedFragment blockedFragment = new BlockedFragment();
        blockedFragment.setArguments(bundle);

        mAdapter.addFragment(spammerFragment, getString(R.string.archiveSpam));
        mAdapter.addFragment(blockedFragment, getString(R.string.blocked));

        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(this);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabsUnsubscribe);
        tabLayout.setupWithViewPager(mViewPager);
    }

    public void setSelectAllState(boolean state) {
        mSelectAll = state;
    }

    public boolean getSelectAllState() {
        return mSelectAll;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.actionSelect:
                Fragment fragment = mAdapter.getFragmentByPosition(mViewPager.getCurrentItem());

                if (fragment instanceof ISelectAction)
                    if (!mSelectAll) {
                        ((ISelectAction) fragment).selectAll();
                        mSelectAll = true;
                    } else {
                        ((ISelectAction) fragment).selectNone();
                        mSelectAll = false;
                    }
                invalidateOptionsMenu();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_unsubscribe, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.getItem(0);

        if (!mSelectAll)
            item.setTitle(getString(R.string.selectAll));
        else
            item.setTitle(getString(R.string.selectNone));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateInfo(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        int pos = mSpinner.getSelectedItemPosition();
        updateInfo(pos);
    }

    private void updateInfo(int position) {
        AccountInfo accountInfo = mListAccountInfo.get(position);
        Fragment fragment = mAdapter.getFragmentByPosition(mViewPager.getCurrentItem());

        if (fragment instanceof ISelectAction)
            ((ISelectAction) fragment).accountChanged(accountInfo);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public interface ISelectAction {
        void accountChanged(AccountInfo accountInfo);

        void selectAll();

        void selectNone();
    }
}
