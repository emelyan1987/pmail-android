package com.planckmail.activities;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.planckmail.R;
import com.planckmail.adapters.DrawerExpandListAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.enums.AccountType;
import com.planckmail.enums.Folders;
import com.planckmail.fragments.AllAccountsFragment;
import com.planckmail.fragments.BoxDriveFileFragment;
import com.planckmail.fragments.CalendarFragment;
import com.planckmail.fragments.ContactDetailsFragment;
import com.planckmail.fragments.DropBoxFileFragment;
import com.planckmail.fragments.EmailAccountsFragment;
import com.planckmail.fragments.GoogleDriveFileFragment;
import com.planckmail.fragments.OneDriveFileFragment;
import com.planckmail.fragments.PeopleFragment;
import com.planckmail.dialogs.SwipeConfirmationDialog;
import com.planckmail.fragments.ThreadFragment;
import com.planckmail.helper.MailFragmentHelper;
import com.planckmail.helper.MenuHelper;
import com.planckmail.service.RegistrationIntentService;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Thread;
import com.planckmail.web.response.nylas.wrapper.Folder;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.service.NylasService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.planckmail.R.layout;
import static com.planckmail.R.string;


public class MenuActivity extends BaseActivity implements ExpandableListView.OnGroupClickListener,
        ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupCollapseListener, ExpandableListView.OnGroupExpandListener,
        View.OnClickListener {

    public static final String KEY_ALL_MAIL_ACCOUNT_ID = "key_all_mail_namespace_id";
    public static final String CACHING_ACTION = "com.plancklabs.local.caching";

    private static final int MAIL_COUNT = 2;
    private static final int DEFAULT_ANOTHER_SECTION_LOAD_MAILS = 100;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int OFFSET = 0;
    public static final int INBOX = 0;
    public static final int DRAFTS = 1;
    public static final int TRASH = 2;
    public static final int SENT_MAIL = 3;
    public static final int ALL_MAIL = 4;
    public static final int STARRED = 5;
    public static final int SPAM = 6;
    public static final int FOLDERS = 7;

    private int mCountRequestCall = 1;
    private boolean mFirstInit;

    private String mPeopleCache;
    private String mCacheCalendarDateList;
    private String mCalendarAllEventList;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private ExpandableListView mExpandableListView;
    private DrawerExpandListAdapter mExpandableListAdapter;
    private LinearLayout mLayoutDrawer;
    private android.widget.TextView tvGroup;
    private List<AccountInfo> mListAccountInfo;
    private ImageView mIvMail;
    private ImageView mIvContacts;
    private ImageView mIvCalendar;
    private ImageView mIvFiles;
    private TextView mTvUnreadMailCount;
    private RelativeLayout mRlMail;
    private RelativeLayout mRlContacts;
    private RelativeLayout mRlCalendar;
    private RelativeLayout mRlFiles;
    private View mViewFiles;
    private View mViewMail;
    private View mViewContacts;
    private View mViewCalendar;

    private HashMap<Integer, HashMap<Integer, String>> mCacheEmailImportant = new HashMap<>();
    private HashMap<String, String> mMessageCacheData = new HashMap<>();
    private HashMap<Integer, HashMap<Integer, String>> mCacheEmailLater = new HashMap<>();
    private HashMap<Integer, HashMap<Integer, String>> mCacheEmailFollowUp = new HashMap<>();
    private HashMap<Integer, HashMap<Integer, String>> mCacheEmailSocial = new HashMap<>();
    private HashMap<Integer, HashMap<Integer, String>> mCacheEmailFolders = new HashMap<>();
    private HashMap<String, List<Folder>> mHashAccountFolders = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);

        initViews();
        setActionBarToggle();
        initListeners();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mCacheActionReceiver, new IntentFilter(CACHING_ACTION));

        setSupportActionBar(mToolbar);

        setAdapter();
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        mListAccountInfo = manager.getEmailAccountInfoList(true);

        mFirstInit = true;
        startGSMService();
        getAccountFolders();
        //count and highlight unread emails
        countAllUnreadEmails();

        //default title
        setTitle(getString(string.menu_inbox));
        setTitleToolbar();
        //default: open inbox mail section
        mRlMail.performClick();
    }

    private void startGSMService() {
        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    private void getAccountFolders() {
        for (final AccountInfo a : mListAccountInfo) {
            AccountType type = a.accountType;

            String folderLabel;
            if (type.toString().equalsIgnoreCase(AccountType.GMAIL.toString()))
                folderLabel = "labels";
            else
                folderLabel = "folders";

            final NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, a.getAccessToken(), "");
            nylasServer.getFolders(folderLabel, new Callback<Object>() {

                @Override
                public void success(Object o, Response response) {
                    String json = (String) o;
                    List<Folder> list = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Folder.class);
                    mHashAccountFolders.put(a.accountId, MenuHelper.getFilteredFolders(list));
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
            });
        }
    }

    /**
     * receiver for save local data
     */
    private BroadcastReceiver mCacheActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            EnumMenuActivity result = (EnumMenuActivity) intent.getSerializableExtra(BundleKeys.ENUM_MENU);

            switch (result) {
                case MESSAGE:
                    String threadId = intent.getStringExtra(BundleKeys.THREAD);
                    String data = intent.getStringExtra(BundleKeys.CACHED_DATA);
                    setMessageCachedData(threadId, data);
                    break;
                case THREAD:
                    int childId = intent.getIntExtra(BundleKeys.CHILD_ID, -1);
                    int groupId = intent.getIntExtra(BundleKeys.GROUP_ID, -1);

                    ThreadFragment.TARGET_FOLDER cachedEmail = (ThreadFragment.TARGET_FOLDER) intent.getSerializableExtra(BundleKeys.CACHED_EMAIL_TYPE);

                    String cachedData = intent.getStringExtra(BundleKeys.CACHED_DATA);

                    switch (cachedEmail) {
                        case READ_LATER:
                            saveCacheEmailLater(groupId, childId, cachedData);
                            break;
                        case READ_NOW:
                            saveCacheEmailImportant(groupId, childId, cachedData);
                            break;
                        case FOLLOW_UP:
                            saveCacheEmailFollowUp(groupId, childId, cachedData);
                            break;
                        case SOCIAL:
                            saveCacheEmailSocial(groupId, childId, cachedData);
                            break;
                        case FOLDERS:
                            saveCacheEmailFolders(groupId, childId, cachedData);
                    }

                    break;
                case CALENDAR:
                    mCacheCalendarDateList = intent.getStringExtra(BundleKeys.CACHED_DATA);
                    mCalendarAllEventList = intent.getStringExtra(BundleKeys.CACHED_LIST_EVENTS);
                    break;
                case CONTACTS:
                    mPeopleCache = intent.getStringExtra(BundleKeys.CACHED_DATA);
                    break;
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mCacheActionReceiver);
    }

    public void setTitleToolbar() {
        if (mListAccountInfo.size() >= MAIL_COUNT) {
            mToolbar.setSubtitle(getString(string.all_accounts));
        } else if (!mListAccountInfo.isEmpty()) {
            AccountInfo account = mListAccountInfo.get(0);
            mToolbar.setSubtitle(account.getEmail());
        }
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFirstInit = false;
    }

    private void setAdapter() {
        mExpandableListAdapter = new DrawerExpandListAdapter(this);
        mExpandableListView.setAdapter(mExpandableListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent().getExtras() != null) {
            boolean loadFileSection = getIntent().getExtras().getBoolean(BundleKeys.LOAD_FILE_SECTION);
            if (loadFileSection)
                mRlFiles.performClick();

            getIntent().getExtras().remove(BundleKeys.LOAD_FILE_SECTION);
        }
    }

    private void setActionBarToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, string.app_name, string.app_name) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                syncState();
                countAllUnreadEmails();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
                syncState();
            }
        };
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(mLayoutDrawer);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void initListeners() {
        mExpandableListView.setOnGroupClickListener(this);
        mExpandableListView.setOnChildClickListener(this);
        mExpandableListView.setOnGroupCollapseListener(this);
        mExpandableListView.setOnGroupExpandListener(this);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mRlCalendar.setOnClickListener(this);
        mRlMail.setOnClickListener(this);
        mRlContacts.setOnClickListener(this);
        mRlFiles.setOnClickListener(this);
    }

    private void initViews() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mExpandableListView = (ExpandableListView) findViewById(R.id.expandList);
        mLayoutDrawer = (LinearLayout) findViewById(R.id.llDrawer);
        mIvContacts = (ImageView) findViewById(R.id.ivContacts);
        mViewContacts = findViewById(R.id.viewContacts);
        mViewFiles = findViewById(R.id.viewFiles);
        mIvFiles = (ImageView) findViewById(R.id.ivFiles);
        mIvMail = (ImageView) findViewById(R.id.ivMail);
        mViewMail = findViewById(R.id.viewMailInbox);
        mIvCalendar = (ImageView) findViewById(R.id.ivCalendar);
        mViewCalendar = findViewById(R.id.viewCalendar);
        mRlCalendar = (RelativeLayout) findViewById(R.id.rlCalendar);
        mRlMail = (RelativeLayout) findViewById(R.id.rlMail);
        mRlContacts = (RelativeLayout) findViewById(R.id.rlContacts);
        mRlFiles = (RelativeLayout) findViewById(R.id.rlFiles);
        mTvUnreadMailCount = (TextView) findViewById(R.id.tvUnreadMailCount);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mLayoutDrawer)) {
            mDrawerLayout.closeDrawers();
            return;
        }
        Fragment fragment = getFragmentManager().findFragmentById(R.id.mainContainer);

        if (fragment instanceof ContactDetailsFragment) {
            ((ContactDetailsFragment) fragment).onBackPress();
        } else if (fragment instanceof PeopleFragment) {
            ((PeopleFragment) fragment).onBackPress();
        } else if (fragment instanceof SwipeConfirmationDialog)
            ((SwipeConfirmationDialog) fragment).onBackPress();
        else if (fragment instanceof AllAccountsFragment)
            ((AllAccountsFragment) fragment).onBackPress();
        else if (fragment instanceof EmailAccountsFragment)
            ((EmailAccountsFragment) fragment).onBackPress();
        else if (fragment instanceof DropBoxFileFragment)
            ((DropBoxFileFragment) fragment).onBackPress();
        else if (fragment instanceof GoogleDriveFileFragment)
            ((GoogleDriveFileFragment) fragment).onBackPress();
        else if (fragment instanceof OneDriveFileFragment)
            ((OneDriveFileFragment) fragment).onBackPress();
        else if (fragment instanceof BoxDriveFileFragment)
            ((BoxDriveFileFragment) fragment).onBackPress();
        else
            super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout.isDrawerOpen(mLayoutDrawer))
                    mDrawerLayout.closeDrawer(mLayoutDrawer);
                else
                    mDrawerLayout.openDrawer(mLayoutDrawer);
                return true;

            case R.id.action_more:
                Intent intent = new Intent(this, MoreActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        mDrawerLayout.closeDrawer(mLayoutDrawer);
        v.setBackgroundColor(getResources().getColor(R.color.blueLight));
        //change background color selected item
        mExpandableListAdapter.changeBackgroundSelectedItem(groupPosition, childPosition);
        AccountInfo accountInfo = getAccountInfo(groupPosition);
        setToolbarTitle(groupPosition, childPosition, accountInfo);

        replaceInbox(groupPosition, childPosition);

        if (mListAccountInfo.size() >= MAIL_COUNT && groupPosition == 0 && childPosition == 0) {
            countAllUnreadEmails();
        } else if (!mExpandableListAdapter.getCountEmails().isEmpty() && childPosition == 0) {
            int unreadEmail = mExpandableListAdapter.getCountEmails().get(accountInfo.accountId);
            mTvUnreadMailCount.setVisibility(View.VISIBLE);
            mTvUnreadMailCount.setText(String.valueOf(unreadEmail));
        } else {
            mTvUnreadMailCount.setVisibility(View.GONE);
        }

        return false;
    }

    private void replaceFileAccount(AccountInfo accountInfo) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(BundleKeys.ACCOUNT_TYPE, accountInfo.accountType);
        switch (accountInfo.getAccountType()) {
            case DROP_BOX:
                replace(DropBoxFileFragment.class, R.id.mainContainer, bundle, true);
                break;
            case GOOGLE_DRIVE:
                replace(GoogleDriveFileFragment.class, R.id.mainContainer, bundle, true);
                break;
            case ONE_DRIVE:
                replace(OneDriveFileFragment.class, R.id.mainContainer, bundle, true);
                break;
            case BOX:
                replace(BoxDriveFileFragment.class, R.id.mainContainer, bundle, true);
                break;
            default:
                replace(EmailAccountsFragment.class, R.id.mainContainer, bundle, true);
        }
    }

    public void setToolbarTitle(int groupPosition, int childPosition, AccountInfo accountInfo) {
        if (mListAccountInfo.size() >= MAIL_COUNT && groupPosition == 0) {
            mToolbar.setSubtitle(getString(string.all_accounts));
        } else {
            mToolbar.setSubtitle(accountInfo.getEmail());
        }
        setTileToolbar(childPosition);
    }

    public void setTileToolbar(int childPosition) {
        String title = MenuHelper.setTileToolbar(this, childPosition);
        if (title != null)
            mToolbar.setTitle(title);
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        tvGroup = (TextView) v.findViewById(R.id.tvMail);
        tvGroup.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mail_black, 0, R.drawable.ic_expand_less_black, 0);

        AccountInfo accountInfo = getAccountInfo(groupPosition);

        if (mViewFiles.getVisibility() == View.VISIBLE) {
            mDrawerLayout.closeDrawer(mLayoutDrawer);
            if (groupPosition == 0 && mListAccountInfo.size() >= 2) {
                showFiles();
                setTitle(getString(R.string.all_accounts));
            } else {
                collapseGroups();
                replaceFileAccount(accountInfo);
            }
        } else
            setInnerAdapterData(accountInfo);

        return false;
    }

    public void setInnerAdapterData(AccountInfo accountInfo) {
        List<Folder> listFolder = mHashAccountFolders.get(accountInfo.accountId);
        mExpandableListAdapter.setMapFolders(listFolder);
    }

    private void replaceInbox(int groupId, int childPosition) {
        Bundle bundle = new Bundle();
        bundle.putInt(BundleKeys.GROUP_ID, groupId);
        bundle.putInt(BundleKeys.CHILD_ID, childPosition);

        replace(ThreadFragment.class, R.id.mainContainer, bundle, false);
    }

    public String getCacheEmailLater(int groupId, int childId) {
        String data = "";

        if (mCacheEmailLater.containsKey(groupId)) {
            HashMap<Integer, String> mapChild = mCacheEmailLater.get(groupId);
            data = mapChild.get(childId);
        }
        return data;
    }

    private void saveCacheEmailImportant(int groupId, int childId, String data) {
        if (!mCacheEmailImportant.containsKey(groupId)) {
            HashMap<Integer, String> childCacheData = new HashMap<>();
            childCacheData.put(childId, data);
            mCacheEmailImportant.put(groupId, childCacheData);
        } else {
            HashMap<Integer, String> childCacheData = mCacheEmailImportant.get(groupId);
            childCacheData.put(childId, data);
            mCacheEmailImportant.put(groupId, childCacheData);
        }
    }

    private void saveCacheEmailLater(int groupId, int childId, String data) {
        if (!mCacheEmailLater.containsKey(groupId)) {
            HashMap<Integer, String> childCacheData = new HashMap<>();
            childCacheData.put(childId, data);
            mCacheEmailLater.put(groupId, childCacheData);
        } else {
            HashMap<Integer, String> childCacheData = mCacheEmailLater.get(groupId);
            childCacheData.put(childId, data);
            mCacheEmailLater.put(groupId, childCacheData);
        }
    }

    private void saveCacheEmailFollowUp(int groupId, int childId, String data) {
        if (!mCacheEmailFollowUp.containsKey(groupId)) {
            HashMap<Integer, String> childCacheData = new HashMap<>();
            childCacheData.put(childId, data);
            mCacheEmailFollowUp.put(groupId, childCacheData);
        } else {
            HashMap<Integer, String> childCacheData = mCacheEmailFollowUp.get(groupId);
            childCacheData.put(childId, data);
            mCacheEmailFollowUp.put(groupId, childCacheData);
        }
    }

    private void saveCacheEmailSocial(int groupId, int childId, String data) {
        if (!mCacheEmailSocial.containsKey(groupId)) {
            HashMap<Integer, String> childCacheData = new HashMap<>();
            childCacheData.put(childId, data);
            mCacheEmailSocial.put(groupId, childCacheData);
        } else {
            HashMap<Integer, String> childCacheData = mCacheEmailSocial.get(groupId);
            childCacheData.put(childId, data);
            mCacheEmailSocial.put(groupId, childCacheData);
        }
    }

    public String getCacheImportantEmail(int groupId, int childId) {
        String data = "";

        if (mCacheEmailImportant.containsKey(groupId)) {
            HashMap<Integer, String> mapChild = mCacheEmailImportant.get(groupId);
            data = mapChild.get(childId);
        }
        return data;
    }

    private void saveCacheEmailFolders(int groupId, int childId, String data) {
        if (!mCacheEmailFolders.containsKey(groupId)) {
            HashMap<Integer, String> childCacheData = new HashMap<>();
            childCacheData.put(childId, data);
            mCacheEmailFolders.put(groupId, childCacheData);
        } else {
            HashMap<Integer, String> childCacheData = mCacheEmailFolders.get(groupId);
            childCacheData.put(childId, data);
            mCacheEmailFolders.put(groupId, childCacheData);
        }
    }

    public String getCacheFoldersEmail(int groupId, int childId) {
        String data = "";

        if (mCacheEmailFolders.containsKey(groupId)) {
            HashMap<Integer, String> mapChild = mCacheEmailFolders.get(groupId);
            data = mapChild.get(childId);
        }
        return data;
    }

    public String getMessageCachedData(String threadId) {
        String data = "";

        if (!mMessageCacheData.isEmpty() && mMessageCacheData.containsKey(threadId)) {
            data = mMessageCacheData.get(threadId);
        }

        return data;
    }

    public String getCacheEmailFollowUp(int groupId, int childId) {
        String data = "";

        if (mCacheEmailFollowUp.containsKey(groupId)) {
            HashMap<Integer, String> mapChild = mCacheEmailFollowUp.get(groupId);
            data = mapChild.get(childId);
        }
        return data;
    }

    public String getCacheEmailSocial(int groupId, int childId) {
        String data = "";

        if (mCacheEmailSocial.containsKey(groupId)) {
            HashMap<Integer, String> mapChild = mCacheEmailSocial.get(groupId);
            data = mapChild.get(childId);
        }
        return data;
    }

    private void setMessageCachedData(String threadId, String data) {
        if (!mMessageCacheData.containsKey(threadId)) {
            mMessageCacheData.put(threadId, data);
        }
    }

    private AccountInfo getAccountInfo(int id) {
        //when count mail >= 2.than we added in drawerExpandListAdapter all inbox section
        if (id != 0 && mListAccountInfo.size() >= 2)
            id = id - 1;

        return mListAccountInfo.get(id);
    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        if (tvGroup != null)
            tvGroup.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mail_black, 0, R.drawable.ic_expand_more_black, 0);
    }

    @Override
    public void onGroupExpand(int groupPosition) {
        for (int i = 0; i < mExpandableListAdapter.getGroupCount(); i++) {
            if (groupPosition != i || mViewFiles.getVisibility() == View.VISIBLE)
                mExpandableListView.collapseGroup(i);
        }

        if (tvGroup != null)
            tvGroup.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mail_black, 0, R.drawable.ic_expand_less_black, 0);
    }

    @Override
    public void onClick(View v) {
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);

        switch (v.getId()) {
            case R.id.rlMail:
                mExpandableListAdapter.loadAccounts(true, false);
                mListAccountInfo = manager.getEmailAccountInfoList(true);

                setMailIconEnable();
                Bundle bundle = new Bundle();
                int defValue = 0;
                bundle.putInt(BundleKeys.GROUP_ID, defValue);
                bundle.putInt(BundleKeys.CHILD_ID, defValue);

                replace(ThreadFragment.class, R.id.mainContainer, bundle, false);
                break;
            case R.id.rlContacts:
                setContactIconEnable();
                Bundle bundle2 = new Bundle();
                bundle2.putString(BundleKeys.CACHED_DATA, mPeopleCache);

                replace(PeopleFragment.class, R.id.mainContainer, bundle2, true);
                break;
            case R.id.rlCalendar:
                setCalendarIconEnable();
                Bundle bundle1 = new Bundle();
                bundle1.putString(BundleKeys.CACHED_DATA, mCacheCalendarDateList);
                bundle1.putString(BundleKeys.CACHED_LIST_EVENTS, mCalendarAllEventList);

                replace(CalendarFragment.class, R.id.mainContainer, bundle1, true);
                break;
            case R.id.rlFiles:
                mExpandableListAdapter.loadAccounts(true, true);
                mListAccountInfo = manager.getAllAccountInfoList();
                collapseGroups();
                setFileIconEnable();

                if (mListAccountInfo.size() >= 2) {
                    showFiles();
                    setTitle(getString(R.string.all_accounts));
                } else {
                    int defaultAccountInfo = 0;
                    AccountInfo accountInfo = getAccountInfo(defaultAccountInfo);
                    replaceFileAccount(accountInfo);
                }

                break;
        }
        mDrawerLayout.closeDrawer(mLayoutDrawer);
    }

    private void collapseGroups() {
        for (int i = 0; i < mExpandableListAdapter.getGroupCount(); i++) {
            mExpandableListView.collapseGroup(i);
        }
    }

    private void showFiles() {
        Bundle bundle3 = new Bundle();
        bundle3.putInt(BundleKeys.VIEW_ID, R.id.mainContainer);
        replace(AllAccountsFragment.class, R.id.mainContainer, bundle3, true);
    }

    public void setContactIconEnable() {
        mIvMail.setBackgroundResource(R.drawable.ic_mail_grey);
        mIvContacts.setBackgroundResource(R.drawable.ic_contact_green);
        mIvCalendar.setBackgroundResource(R.drawable.ic_event_grey);
        mIvFiles.setBackgroundResource(R.drawable.ic_file_grey);

        mViewFiles.setVisibility(View.GONE);
        mViewCalendar.setVisibility(View.GONE);
        mViewMail.setVisibility(View.GONE);
        mViewContacts.setVisibility(View.VISIBLE);
    }

    public void setMailIconEnable() {
        mIvMail.setBackgroundResource(R.drawable.ic_mail_green);
        mIvContacts.setBackgroundResource(R.drawable.ic_contacts_grey);
        mIvCalendar.setBackgroundResource(R.drawable.ic_event_grey);
        mIvFiles.setBackgroundResource(R.drawable.ic_file_grey);

        mViewCalendar.setVisibility(View.INVISIBLE);
        mViewMail.setVisibility(View.VISIBLE);
        mViewContacts.setVisibility(View.INVISIBLE);
        mViewFiles.setVisibility(View.INVISIBLE);
    }


    public void setCalendarIconEnable() {
        mIvMail.setBackgroundResource(R.drawable.ic_mail_grey);
        mIvContacts.setBackgroundResource(R.drawable.ic_contacts_grey);
        mIvCalendar.setBackgroundResource(R.drawable.ic_event_green);
        mIvFiles.setBackgroundResource(R.drawable.ic_file_grey);

        mViewCalendar.setVisibility(View.VISIBLE);
        mViewMail.setVisibility(View.INVISIBLE);
        mViewContacts.setVisibility(View.INVISIBLE);
        mViewFiles.setVisibility(View.INVISIBLE);
    }

    public void setFileIconEnable() {
        mIvMail.setBackgroundResource(R.drawable.ic_mail_grey);
        mIvContacts.setBackgroundResource(R.drawable.ic_contacts_grey);
        mIvCalendar.setBackgroundResource(R.drawable.ic_event_grey);
        mIvFiles.setBackgroundResource(R.drawable.ic_file_green);

        mViewFiles.setVisibility(View.VISIBLE);
        mViewCalendar.setVisibility(View.INVISIBLE);
        mViewMail.setVisibility(View.INVISIBLE);
        mViewContacts.setVisibility(View.INVISIBLE);
    }


    public void countAllUnreadEmails() {
        mCountRequestCall = 0;

        final LinkedHashMap<String, Integer> mapContUnreadEmails = new LinkedHashMap<>();

        for (int i = 0; i < mListAccountInfo.size(); i++) {
            final AccountInfo accountInfo = mListAccountInfo.get(i);

            HashMap<String, String> mapParams = new HashMap<>();
            if (accountInfo.getEmail().contains(MailFragmentHelper.GMAIL_APP))
                mapParams.put(Folders.IN.toString(), Folders.READ_NOW.toString());
            else
                mapParams.put(Folders.IN.toString(), Folders.INBOX.toString());


            final NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
            nylasServer.getThreadsInbox(mapParams, DEFAULT_ANOTHER_SECTION_LOAD_MAILS, OFFSET, new Callback<Object>() {
                @Override
                public void success(Object o, Response response) {
                    Log.i(PlanckMailApplication.TAG, "response url " + response.getUrl());
                    mCountRequestCall++;

                    String json = (String) o;

                    if (json != null) {
                        List<Thread> list = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Thread.class);
                        int countElem = countUnreadThreads(list);

                        mapContUnreadEmails.put(accountInfo.getAccountId(), countElem);

                        if (mCountRequestCall == mListAccountInfo.size()) {
                            int count = 0;

                            List<Integer> listIntValues = new ArrayList<>();

                            for (String key : mapContUnreadEmails.keySet())
                                listIntValues.add(mapContUnreadEmails.get(key));

                            for (Integer i : listIntValues) {
                                count += i;
                            }

                            int defaultPosition = 0;

                            add(mapContUnreadEmails, defaultPosition, KEY_ALL_MAIL_ACCOUNT_ID, count);
                            mExpandableListAdapter.setInboxCount(mapContUnreadEmails, 0);

                            countUnreadThreads(mapContUnreadEmails);
                        }
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    mExpandableListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void countUnreadThreads(LinkedHashMap<String, Integer> mapContUnreadEmails) {
        int count = 0;
        if (!mapContUnreadEmails.isEmpty()) {
            for (AccountInfo accountInfo : mListAccountInfo) {
                int unreadEmail = mapContUnreadEmails.get(accountInfo.accountId);
                count = count + unreadEmail;
            }

            if (count > 0) {
                mTvUnreadMailCount.setVisibility(View.VISIBLE);
                mTvUnreadMailCount.setText(String.valueOf(count));
            } else {
                mTvUnreadMailCount.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(PlanckMailApplication.TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }


    @SuppressLint("Assert")
    private <K, V> void add(LinkedHashMap<K, V> map, int index, K key, V value) {
        assert (map != null);
        assert !map.containsKey(key);
        assert (index >= 0) && (index < map.size());

        int i = 0;
        List<Map.Entry<K, V>> rest = new ArrayList<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (i++ >= index) {
                rest.add(entry);
            }
        }
        map.put(key, value);
        for (int j = 0; j < rest.size(); j++) {
            Map.Entry<K, V> entry = rest.get(j);
            map.remove(entry.getKey());
            map.put(entry.getKey(), entry.getValue());
        }
    }

    private int countUnreadThreads(List<Thread> list) {
        ArrayList<Thread> unreadThread = new ArrayList<>();

        for (Thread thread : list) {
            if (thread.unread)
                unreadThread.add(thread);
        }
        return unreadThread.size();
    }

    public enum EnumMenuActivity {
        THREAD, MESSAGE, CALENDAR, EVENT, CONTACTS
    }

}
