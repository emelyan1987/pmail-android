package com.planckmail.fragments;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.ComposeActivity;
import com.planckmail.activities.MenuActivity;
import com.planckmail.activities.MessageActivity;
import com.planckmail.adapters.RecycleThreadAdapter;
import com.planckmail.adapters.SimpleSectionedRecyclerViewAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.DataBaseHelper;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.beans.ThreadDB;
import com.planckmail.enums.Folders;
import com.planckmail.helper.InternetConnection;
import com.planckmail.helper.ItemDecorationMail;
import com.planckmail.helper.MailFragmentHelper;
import com.planckmail.helper.UserHelper;
import com.planckmail.listeners.EndlessRecyclerOnScrollListener;
import com.planckmail.tasks.AsyncLoadThreadDB;
import com.planckmail.tasks.AsyncLoadThreadDB.ILoadedThread;
import com.planckmail.tasks.AsyncSaveEmailInDb;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Thread;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.service.NylasService;
import com.planckmail.web.restClient.service.PlanckService;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.support.v4.widget.SwipeRefreshLayout.*;
import static com.planckmail.adapters.RecycleThreadAdapter.*;

/**
 * Created by Taras Matolinets on 07.05.15.
 */
public class ThreadFragment extends BaseFragment implements OnRefreshListener, OnClickListener,
        OnQueryTextListener, ILoadedThread, OnElementClickListener {
    public static final String NEW_MESSAGE = "plank.mail.show.new.message";

    private static final int MAIL_COUNT = 2;
    public static final int DEFAULT_ANOTHER_SECTION_LOAD_MAILS = 100;
    public static final int DEFAULT_COUNT_LOAD_EMAILS = 10;
    public static final int REQUEST_CODE = 111;
    public static final String MESSAGE_ACTION = "com.plancklabs.message.action";
    public static final int ALL_ACCOUNT_GROUP_KEY = -1;
    public static final int DRAFT = 1;
    public static final int LIMIT = 30;
    private static int COUNT_LIMIT_MAIL = 100;
    private static int OFFSET;

    private boolean mFistInit = true;
    private boolean mDisableScrollInOffline;
    private int mLastAccountLoaded = 0;

    private RecyclerView mRecycleInbox;
    private RecycleThreadAdapter mBaseAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressBar mProgressLoading;
    private List<AccountInfo> mAccountInfoList;
    private List<Thread> mAllAccountsThreadList = new ArrayList<>();
    private LinearLayoutManager mLayoutManager;
    private EndlessRecyclerOnScrollListener mRecycleScrollListener;
    private FloatingActionButton mActionButton;
    private TextView mTvNoMails;
    private CoordinatorLayout mLayout;
    private RelativeLayout mLayoutFilter;
    private RelativeLayout mRlImportant;
    private RelativeLayout mRlReadLater;
    private RelativeLayout mRlFollowUp;
    private RelativeLayout mRlSocial;
    private TextView mTvSocial;
    private TextView mTvImportant;
    private TextView mTvFollowUp;
    private TextView mTvReadLater;
    private View mViewImportant;
    private View mViewReadLater;
    private View mViewFollowUp;
    private View mViewSocial;
    private TARGET_FOLDER mTargetFolder = TARGET_FOLDER.READ_NOW;
    private SimpleSectionedRecyclerViewAdapter mSectionedAdapter;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_inbox, container, false);

        setActionBar();
        setTitle();
        mAccountInfoList = UserHelper.getEmailAccountList(true);
        registerReceivers();
        initViews(view);
        initListeners();
        setAdapterData();
        setRecycleListSettings();
        mSwipeRefreshLayout.setColorSchemeColors(R.color.primaryGreen);
        //default focus on important emails
        mRlImportant.performClick();

        return view;
    }

    public void setActionBar() {
        ActionBar mActionBar = ((MenuActivity) getActivity()).getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayShowCustomEnabled(false);
        }
    }

    public void setTitle() {
        int groupId = getArguments().getInt(BundleKeys.GROUP_ID);
        if (groupId == 0) {
            ((MenuActivity) getActivity()).setTitleToolbar();
            getActivity().setTitle(R.string.menu_inbox);
        }
    }

    public void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(NEW_MESSAGE);
        getActivity().registerReceiver(messageReceiver, filter);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageActionReceiver, new IntentFilter(MESSAGE_ACTION));
    }

    public void setRecycleListSettings() {
        mRecycleInbox.setLayoutManager(mLayoutManager);
        int size = getResources().getDimensionPixelSize(R.dimen.edge_most);
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);
        boolean isSecondLayer = getArguments().getBoolean(BundleKeys.IS_SECOND_LAYER_LIST);

        if (childId != 0 || isSecondLayer) {
            size = 0;
            mLayoutFilter.setVisibility(GONE);
        }
        mRecycleInbox.addItemDecoration(new ItemDecorationMail((size)));
        mRecycleInbox.setHasFixedSize(true);

        addDivider(R.dimen.edge_tiny);
    }

    private void initViews(View view) {
        mLayoutManager = new LinearLayoutManager(getActivity());
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        mRecycleInbox = (RecyclerView) view.findViewById(R.id.recycleInbox);
        mProgressLoading = (ProgressBar) view.findViewById(R.id.prLoading);
        mTvNoMails = (TextView) view.findViewById(R.id.tvNoMails);
        mTvImportant = (TextView) view.findViewById(R.id.tvImportant);
        mTvReadLater = (TextView) view.findViewById(R.id.tvReadLater);
        mTvFollowUp = (TextView) view.findViewById(R.id.tvFollowUp);
        mTvSocial = (TextView) view.findViewById(R.id.tvSocial);
        mActionButton = (FloatingActionButton) view.findViewById(R.id.abCreateEmail);
        mLayout = (CoordinatorLayout) view.findViewById(R.id.coordinateLayout);
        mLayoutFilter = (RelativeLayout) view.findViewById(R.id.viewFilterEmail);
        mRlImportant = (RelativeLayout) mLayoutFilter.findViewById(R.id.rlImportant);
        mRlReadLater = (RelativeLayout) mLayoutFilter.findViewById(R.id.rlReadLater);
        mRlFollowUp = (RelativeLayout) mLayoutFilter.findViewById(R.id.rlFollowUp);
        mRlSocial = (RelativeLayout) mLayoutFilter.findViewById(R.id.rlSocial);
        mViewFollowUp = mLayoutFilter.findViewById(R.id.viewFollowUp);
        mViewImportant = mLayoutFilter.findViewById(R.id.viewImportant);
        mViewReadLater = mLayoutFilter.findViewById(R.id.viewReadLater);
        mViewSocial = mLayoutFilter.findViewById(R.id.viewSocial);
    }

    private void initListeners() {
        addScrollListener();
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mActionButton.setOnClickListener(this);
        mRlImportant.setOnClickListener(this);
        mRlReadLater.setOnClickListener(this);
        mRlSocial.setOnClickListener(this);
        mRlFollowUp.setOnClickListener(this);
    }

    public void addScrollListener() {
        mRecycleInbox.setOnScrollListener(mRecycleScrollListener = new EndlessRecyclerOnScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int current_page) {
                if (!mDisableScrollInOffline) {
                    mBaseAdapter.stopLoading(false);
                    int groupId = getArguments().getInt(BundleKeys.GROUP_ID);

                    boolean notFollowUpFolder = mTargetFolder != TARGET_FOLDER.FOLLOW_UP;
                    boolean isAllAccountSection = groupId != 0 && mAccountInfoList.size() >= MAIL_COUNT;
                    boolean isSeparateSection = groupId == 0 && mAccountInfoList.size() < MAIL_COUNT;

                    if (notFollowUpFolder && isAllAccountSection || isSeparateSection && notFollowUpFolder) {
                        hideProgress(false, 1);
                        fillDataList();
                    } else {
                        mBaseAdapter.stopLoading(true);
                    }
                }
            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onHide() {
                mLayoutFilter.animate().translationY(-mLayoutFilter.getHeight()).setInterpolator(new AccelerateInterpolator(2));
                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) mActionButton.getLayoutParams();
                int fabBottomMargin = lp.bottomMargin;
                mActionButton.animate().translationY(mActionButton.getHeight() + fabBottomMargin).setInterpolator(new AccelerateInterpolator(2)).start();
            }

            @Override
            public void onShow() {
                int childId = getArguments().getInt(BundleKeys.CHILD_ID);
                //show filtered email just in inbox section
                if (childId == 0)
                    mLayoutFilter.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2));

                mActionButton.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
            }
        });
    }

    public void setAdapterData() {
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);
        setCache();

        mBaseAdapter = new RecycleThreadAdapter(getActivity(), mAllAccountsThreadList, this);
        mSectionedAdapter = new SimpleSectionedRecyclerViewAdapter(getActivity(), R.layout.elem_follow_up_section, R.id.section_text, mBaseAdapter);
        mRecycleInbox.setAdapter(mSectionedAdapter);
        mBaseAdapter.setChildId(childId);
    }

    public void setCache() {
        String readNowEmail = getArguments().getString(BundleKeys.CACHED_READ_NOW);
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);
        int groupId = getGroupIdLoadCache();

        //check cache in activity
        if (TextUtils.isEmpty(readNowEmail)) {
            readNowEmail = ((MenuActivity) getActivity()).getCacheImportantEmail(groupId, childId);
        }
        if (TextUtils.isEmpty(readNowEmail) && mFistInit) {
            //show progress when data isn't cached yet
            mFistInit = false;
            mProgressLoading.setVisibility(VISIBLE);
        }

        AsyncCachedTask task = new AsyncCachedTask();
        task.execute(readNowEmail);
    }

    @Override
    public void onElementClicked(int position) {
        List<Thread> list = mBaseAdapter.getListDate();

        //get new position in section list
//        if (mTargetFolder == TARGET_FOLDER.FOLLOW_UP)
//            position = mSectionedAdapter.sectionedPositionToPosition(position);

        final Thread thread = list.get(position);
        AccountInfo accountInfo = null;

        for (AccountInfo account : mAccountInfoList) {
            if (account.getAccountId().equalsIgnoreCase(thread.account_id)) {
                accountInfo = account;
            }
        }

        Bundle bundle = getBundle(accountInfo, thread);
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);

        if (childId != DRAFT) {
            Intent intent = new Intent(getActivity(), MessageActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            startComposeActivity(thread, childId);
        }
    }

    public TARGET_FOLDER getTargetFolder() {
        return mTargetFolder;
    }

    private class AsyncCachedTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String readNowEmail = strings[0];
            if (!TextUtils.isEmpty(readNowEmail))
                mAllAccountsThreadList = JsonUtilFactory.getJsonUtil().fromJsonArray(readNowEmail, Thread.class);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            int childId = getArguments().getInt(BundleKeys.CHILD_ID);

            mBaseAdapter = new RecycleThreadAdapter(getActivity(), mAllAccountsThreadList, ThreadFragment.this);
            mSectionedAdapter = new SimpleSectionedRecyclerViewAdapter(getActivity(), R.layout.elem_follow_up_section, R.id.section_text, mBaseAdapter);
            mRecycleInbox.setAdapter(mSectionedAdapter);
            mBaseAdapter.setChildId(childId);
        }
    }

    public View getBaseView() {
        return getView();
    }


    public SimpleSectionedRecyclerViewAdapter getSectionPosition() {
        return mSectionedAdapter;
    }

    /**
     * show notification when we archive ,delete or do another action with email
     */
    private BroadcastReceiver mMessageActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra(BundleKeys.TOAST_TEXT);
            Snackbar.make(mLayout, text, Snackbar.LENGTH_SHORT).show();
        }
    };
    /**
     * show new inbox notification
     */
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            clearThreadsList();
            fillDataList();
        }
    };

    public void startComposeActivity(Thread thread, int childId) {
        Intent intentMessage = new Intent(getActivity(), ComposeActivity.class);
        intentMessage.putExtra(BundleKeys.CHILD_ID, childId);
        intentMessage.putStringArrayListExtra(BundleKeys.DRAFT_ID, thread.draft_ids);
        startActivityForResult(intentMessage, REQUEST_CODE);
    }

    private void loadEmailFromDB() {
        if (!InternetConnection.isNetworkConnected(getActivity())) {
            mSwipeRefreshLayout.setEnabled(false);
            mDisableScrollInOffline = true;

            clearThreadsList();
            mProgressLoading.setVisibility(VISIBLE);
            mTvNoMails.setVisibility(GONE);

            AsyncLoadThreadDB task = new AsyncLoadThreadDB(this);
            task.execute();
        }
    }

    private void fillDataList() {
        int groupId = getArguments().getInt(BundleKeys.GROUP_ID);

        if (mAccountInfoList.size() >= MAIL_COUNT && groupId == 0) {
            hideProgress(false, 1);

            OFFSET = 0;
            COUNT_LIMIT_MAIL = DEFAULT_ANOTHER_SECTION_LOAD_MAILS;

            for (AccountInfo a : mAccountInfoList) {
                loadSeparateMail(a, true);
            }
        } else {
            setCountMailLimit();
            if (mBaseAdapter.getStopLoading())
                OFFSET = 0;

            hideProgress(true, 1);

            int item = getItem(groupId);
            AccountInfo accountInfo = mAccountInfoList.get(item);
            loadSeparateMail(accountInfo, false);
        }
    }

    private int getItem(int groupId) {
        int item;
        //we added all inbox folder so offset will increase for one item
        if (groupId != 0)
            item = groupId - 1;
        else
            item = groupId;
        return item;
    }

    private void setCountMailLimit() {
        if (mTargetFolder == TARGET_FOLDER.FOLLOW_UP)
            COUNT_LIMIT_MAIL = DEFAULT_ANOTHER_SECTION_LOAD_MAILS;
        else
            COUNT_LIMIT_MAIL = LIMIT;
    }

    private void loadSeparateMail(AccountInfo accountInfo, boolean state) {
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);

        String folderName = getArguments().getString(BundleKeys.FOLDER_NAME);
        HashMap<String, String> mapParams;
        boolean isSecondLayer = getArguments().getBoolean(BundleKeys.IS_SECOND_LAYER_LIST);

        if (isSecondLayer) {
            mapParams = new HashMap<>();
            mapParams.put(Folders.IN.toString(), folderName);
        } else
            mapParams = MailFragmentHelper.getMapTags(mTargetFolder, childId, accountInfo);

        inboxMail(accountInfo, mapParams, state);
    }

    public void updateAdapter(List<Thread> listUnread) {
        mBaseAdapter.updateData(listUnread);
        mSectionedAdapter.notifyDataSetChanged();
    }

    private Bundle getBundle(AccountInfo accountInfo, Thread thread) {
        String title = getActivity().getTitle().toString();

        int groupId = getArguments().getInt(BundleKeys.GROUP_ID);
        String jsonThread = JsonUtilFactory.getJsonUtil().toJson(thread);

        Bundle bundle = new Bundle();
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);

        bundle.putString(BundleKeys.TITLE, title);
        bundle.putString(BundleKeys.SUB_TITLE, accountInfo.email);
        bundle.putString(BundleKeys.THREAD, jsonThread);
        bundle.putString(BundleKeys.ACCESS_TOKEN, accountInfo.accessToken);
        bundle.putInt(BundleKeys.CHILD_ID, childId);

        if (groupId != 0) {
            groupId = groupId - 1;
        }

        bundle.putInt(BundleKeys.GROUP_ID, groupId);
        return bundle;
    }

    private void addDivider(int divider) {
        mRecycleInbox.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .sizeResId(divider)
                .colorResId(R.color.gray_divider)
                .build());
    }

    public void clearThreadsList() {
        mAllAccountsThreadList.clear();
        mBaseAdapter.updateData(mAllAccountsThreadList);
        mSectionedAdapter.notifyDataSetChanged();
    }

    private void insertElements(List<Thread> list) {
        if (mSwipeRefreshLayout.isRefreshing()) {
            refreshFullList(list);
        } else if (mAllAccountsThreadList.isEmpty()) {
            addList(list);
        } else if (!list.isEmpty()) {
            addItem(list);
        }
        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setEnabled(true);

        matchFollowUpSectionsToList();
    }

    private void refreshFullList(List<Thread> list) {
        mAllAccountsThreadList = list;
        mBaseAdapter.updateData(mAllAccountsThreadList);
        mSectionedAdapter.notifyDataSetChanged();
    }

    private void addItem(List<Thread> list) {
        int offset = 1;
        int count = mAllAccountsThreadList.size() + offset;
        mAllAccountsThreadList.addAll(list);

        for (int i = count; i < mAllAccountsThreadList.size(); i++) {
            mSectionedAdapter.notifyItemInserted(i);
            mSectionedAdapter.notifyItemRangeInserted(i, mAllAccountsThreadList.size());
        }
    }

    private void addList(List<Thread> list) {
        mAllAccountsThreadList.addAll(list);
        mBaseAdapter.updateData(mAllAccountsThreadList);
        mSectionedAdapter.notifyDataSetChanged();
    }

    private void matchFollowUpSectionsToList() {
        if (mTargetFolder == TARGET_FOLDER.FOLLOW_UP && !mAllAccountsThreadList.isEmpty()) {
            SimpleSectionedRecyclerViewAdapter.Section[] section = MailFragmentHelper.getSchedule(getActivity(), mAllAccountsThreadList);
            mSectionedAdapter.setSections(section);
        }
    }

    private void hideProgress(boolean state, int offset) {
        View view = mLayoutManager.getChildAt(mLayoutManager.getChildCount() - offset);
        if (view != null) {
            LinearLayout llMail = (LinearLayout) view.findViewById(R.id.llProgressLoadMail);

            if (llMail != null) {
                ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
                if (state) {
                    progressBar.setVisibility(GONE);
                } else {
                    progressBar.setVisibility(VISIBLE);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.abCreateEmail:
                startEmailActivity();
                break;
            case R.id.rlImportant:
                mBaseAdapter.showUnsubscribeButton(false);
                mLastAccountLoaded = 0;
                highLightImportantSection();
                //in offline mode just load emails from DB
                if (!InternetConnection.isNetworkConnected(getActivity())) {
                    loadEmailFromDB();
                    return;
                }
                loadImportantEmail();
                break;
            case R.id.rlReadLater:
                mBaseAdapter.showUnsubscribeButton(true);
                mLastAccountLoaded = 0;
                highLightReadLaterSection();
                loadReadLaterEmails();
                break;
            case R.id.rlFollowUp:
                mBaseAdapter.showUnsubscribeButton(false);
                mLastAccountLoaded = 0;
                highLightFollowUpSection();
                followUpEmails();
                break;
            case R.id.rlSocial:
                mBaseAdapter.showUnsubscribeButton(false);
                mLastAccountLoaded = 0;
                highLightSocialEmails();
                loadSocialEmail();
                break;
        }
    }

    private void highLightSocialEmails() {
        mTvNoMails.setVisibility(GONE);
        mViewImportant.setVisibility(GONE);
        mViewReadLater.setVisibility(GONE);
        mViewFollowUp.setVisibility(GONE);
        mViewSocial.setVisibility(VISIBLE);

        mTvImportant.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));
        mTvSocial.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGreen));
        mTvReadLater.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));
        mTvFollowUp.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));

        mTvImportant.setTypeface(null, Typeface.NORMAL);
        mTvSocial.setTypeface(null, Typeface.BOLD);
        mTvReadLater.setTypeface(null, Typeface.NORMAL);
        mTvFollowUp.setTypeface(null, Typeface.NORMAL);
    }

    public void highLightFollowUpSection() {
        mTvNoMails.setVisibility(GONE);
        mViewImportant.setVisibility(GONE);
        mViewReadLater.setVisibility(GONE);
        mViewFollowUp.setVisibility(VISIBLE);
        mViewSocial.setVisibility(GONE);


        mTvImportant.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));
        mTvSocial.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));
        mTvReadLater.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));
        mTvFollowUp.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGreen));

        mTvImportant.setTypeface(null, Typeface.NORMAL);
        mTvSocial.setTypeface(null, Typeface.NORMAL);
        mTvReadLater.setTypeface(null, Typeface.NORMAL);
        mTvFollowUp.setTypeface(null, Typeface.BOLD);
    }

    public void highLightReadLaterSection() {
        mTvNoMails.setVisibility(GONE);
        mViewImportant.setVisibility(GONE);
        mViewReadLater.setVisibility(VISIBLE);
        mViewFollowUp.setVisibility(GONE);
        mViewSocial.setVisibility(GONE);

        mTvImportant.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));
        mTvSocial.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));
        mTvReadLater.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGreen));
        mTvFollowUp.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));

        mTvImportant.setTypeface(null, Typeface.NORMAL);
        mTvSocial.setTypeface(null, Typeface.NORMAL);
        mTvReadLater.setTypeface(null, Typeface.BOLD);
        mTvFollowUp.setTypeface(null, Typeface.NORMAL);
    }

    public void highLightImportantSection() {
        mTvNoMails.setVisibility(GONE);
        mViewImportant.setVisibility(VISIBLE);
        mViewReadLater.setVisibility(GONE);
        mViewFollowUp.setVisibility(GONE);
        mViewSocial.setVisibility(GONE);

        mTvImportant.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGreen));
        mTvSocial.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));
        mTvReadLater.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));
        mTvFollowUp.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightGray));

        mTvImportant.setTypeface(null, Typeface.BOLD);
        mTvSocial.setTypeface(null, Typeface.NORMAL);
        mTvReadLater.setTypeface(null, Typeface.NORMAL);
        mTvFollowUp.setTypeface(null, Typeface.NORMAL);

    }

    private void loadSocialEmail() {
        //Cache data is empty than we clean list
        if (isNoInternetConnection()) return;

        //Cache data is empty than we clean list
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);
        int groupId = getGroupIdLoadCache();

        boolean isSocial = false;
        if (mTargetFolder != TARGET_FOLDER.SOCIAL) {
            mTargetFolder = TARGET_FOLDER.SOCIAL;
            isSocial = true;
        }

        String dataSocialEmail = ((MenuActivity) getActivity()).getCacheEmailSocial(groupId, childId);
        loadCacheData(isSocial, dataSocialEmail);
    }

    private void followUpEmails() {
        //Cache data is empty than we clean list
        if (isNoInternetConnection()) return;

        //Cache data is empty than we clean list
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);
        int groupId = getGroupIdLoadCache();

        boolean isFollowUp = false;
        if (mTargetFolder != TARGET_FOLDER.FOLLOW_UP) {
            mTargetFolder = TARGET_FOLDER.FOLLOW_UP;
            isFollowUp = true;
        }

        String dataReadFollowUpEmail = ((MenuActivity) getActivity()).getCacheEmailFollowUp(groupId, childId);
        loadCacheData(isFollowUp, dataReadFollowUpEmail);
    }

    public void loadReadLaterEmails() {
        //Cache data is empty than we clean list
        if (isNoInternetConnection()) return;

        int childId = getArguments().getInt(BundleKeys.CHILD_ID);
        int groupId = getGroupIdLoadCache();

        boolean isReadLater = false;
        if (mTargetFolder != TARGET_FOLDER.READ_LATER) {
            mTargetFolder = TARGET_FOLDER.READ_LATER;
            isReadLater = true;
        }

        String dataReadLaterEmail = ((MenuActivity) getActivity()).getCacheEmailLater(groupId, childId);
        loadCacheData(isReadLater, dataReadLaterEmail);
    }

    public boolean isNoInternetConnection() {
        //don't load read later emails when in inbox section no important mails
        if (!InternetConnection.isNetworkConnected(getActivity())) {
            mTvNoMails.setVisibility(VISIBLE);
            clearThreadsList();
            return true;
        }
        return false;
    }

    public void loadCacheData(boolean flag, final String dataReadLaterEmail) {
        AsyncLoadThreadCache task = new AsyncLoadThreadCache(flag);
        task.execute(dataReadLaterEmail);
    }

    private class AsyncLoadThreadCache extends AsyncTask<String, Void, List<Thread>> {
        private boolean flag;

        public AsyncLoadThreadCache(boolean flag) {
            this.flag = flag;
        }

        @Override
        protected List<Thread> doInBackground(String... strings) {
            String dataReadLaterEmail = strings[0];
            List<Thread> list = new ArrayList<>();
            if (!TextUtils.isEmpty(dataReadLaterEmail)) {
                list = JsonUtilFactory.getJsonUtil().fromJsonArray(dataReadLaterEmail, Thread.class);
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<Thread> list) {
            super.onPostExecute(list);
            if (flag && mTargetFolder != TARGET_FOLDER.FOLLOW_UP) {
                mSectionedAdapter.deleteAllSections();
                clearThreadsList();
            } else {
                clearThreadsList();
            }

            if (list.isEmpty()) {
                mProgressLoading.setVisibility(VISIBLE);
                fillDataList();
            } else if (flag) {
                mProgressLoading.setVisibility(GONE);
                insertElements(list);
            }
        }
    }

    private class AsyncLoadThreadCacheImportant extends AsyncTask<String, Void, List<Thread>> {

        @Override
        protected List<Thread> doInBackground(String... strings) {
            String dataReadLaterEmail = strings[0];
            List<Thread> list = new ArrayList<>();
            if (!TextUtils.isEmpty(dataReadLaterEmail)) {
                list = JsonUtilFactory.getJsonUtil().fromJsonArray(dataReadLaterEmail, Thread.class);
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<Thread> list) {
            super.onPostExecute(list);
            boolean isReadNow = false;
            boolean isSecondLayerList = getArguments().getBoolean(BundleKeys.IS_SECOND_LAYER_LIST);

            if (mTargetFolder != TARGET_FOLDER.READ_NOW && !isSecondLayerList) {
                mTargetFolder = TARGET_FOLDER.READ_NOW;
                isReadNow = true;
            } else if (isSecondLayerList) {
                mTargetFolder = TARGET_FOLDER.FOLDERS;
                isReadNow = true;
            }

            if (isReadNow) {
                clearThreadsList();
                mSectionedAdapter.deleteAllSections();
            }
            if (list.isEmpty()) {
                mSectionedAdapter.deleteAllSections();
                mProgressLoading.setVisibility(VISIBLE);
                fillDataList();
            } else if (isReadNow) {
                insertElements(list);
            }
        }
    }

    public int getGroupIdLoadCache() {
        int groupId = getArguments().getInt(BundleKeys.GROUP_ID);
        if (mAccountInfoList.size() >= MAIL_COUNT && groupId == 0)
            groupId = ALL_ACCOUNT_GROUP_KEY;
        return groupId;
    }

    public void loadImportantEmail() {
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);
        int groupId = getGroupIdLoadCache();

        //check cache in activity
        boolean isSecondLayerList = getArguments().getBoolean(BundleKeys.IS_SECOND_LAYER_LIST);
        String data;

        if (isSecondLayerList)
            data = ((MenuActivity) getActivity()).getCacheFoldersEmail(groupId, childId);
        else
            data = ((MenuActivity) getActivity()).getCacheImportantEmail(groupId, childId);

        AsyncLoadThreadCacheImportant task = new AsyncLoadThreadCacheImportant();
        task.execute(data);
    }

    public void startEmailActivity() {
        Intent intentMessage = new Intent(getActivity(), ComposeActivity.class);
        intentMessage.putExtra(BundleKeys.CREATE_MESSAGE, true);
        startActivityForResult(intentMessage, REQUEST_CODE);
    }

    public void inboxMail(final AccountInfo accountInfo, HashMap<String, String> mapParams, final boolean isAllMailsLoad) {
        hideView();
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);

        if (childId == MenuActivity.STARRED)
            mapParams.put("starred", "true");

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
        nylasServer.getThreadsInbox(mapParams, COUNT_LIMIT_MAIL, OFFSET, new Callback<Object>() {
            @Override
            public void failure(RetrofitError restError) {
                if (isAllMailsLoad)
                    errorAllMailsResponse(restError);
                else
                    failureMailResponse(restError);
            }

            @Override
            public void success(Object o, Response response) {
                String json = (String) o;
                if (!json.equalsIgnoreCase("[]"))
                    saveDataToCache(json);

                mDisableScrollInOffline = false;
                mTvNoMails.setVisibility(GONE);
                mBaseAdapter.stopLoading(true);

                if (isAllMailsLoad)
                    successAllMailResponse(json, response);
                else if (json != null)
                    successMailResponse(json, response);
            }
        });
    }

    /**
     * show filter email view on just inbox email folder
     */
    public void hideView() {
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);
        boolean isSecondLayer = getArguments().getBoolean(BundleKeys.IS_SECOND_LAYER_LIST);

        if (childId != 0 || isSecondLayer) {
            mLayoutFilter.setVisibility(GONE);
        }
    }

    public void errorAllMailsResponse(RetrofitError restError) {
        mBaseAdapter.stopLoading(true);
        Response r = restError.getResponse();
        if (r != null)
            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());

        mSectionedAdapter.notifyDataSetChanged();
        mProgressLoading.setVisibility(GONE);
        mRecycleScrollListener.reset(0, true);
    }

    public void successMailResponse(final String json, final Response response) {
        mRecycleScrollListener.reset(0, true);

        Log.i(PlanckMailApplication.TAG, "response url " + response.getUrl());

        if (isVisible()) {
            TaskMailResponse taskMailResponse = new TaskMailResponse();
            taskMailResponse.execute(json);
        }
    }

    private class TaskMailResponse extends AsyncTask<String, Void, List<Thread>> {

        @Override
        protected List<Thread> doInBackground(String... strings) {
            String json = strings[0];
            return JsonUtilFactory.getJsonUtil().fromJsonArray(json, Thread.class);
        }

        @Override
        protected void onPostExecute(List<Thread> list) {
            super.onPostExecute(list);
            insertElements(list);

            hideProgress(true, 1);

            PlanckMailApplication app = (PlanckMailApplication) getActivity().getApplication();
            boolean isLoadDb = app.getLoadDB();

            int groupId = getArguments().getInt(BundleKeys.GROUP_ID);

            if (groupId == 0 && isLoadDb) {
                loadEmailWithMessage();
            }

            OFFSET += COUNT_LIMIT_MAIL;
            hideMainProgress();
        }
    }

    public void successAllMailResponse(final String json, final Response response) {
        mRecycleScrollListener.reset(0, true);

        Log.i(PlanckMailApplication.TAG, "response url " + response.getUrl());

        AsyncTaskAllMailResponse taskAllMailResponse = new AsyncTaskAllMailResponse();
        taskAllMailResponse.execute(json);
    }

    private class AsyncTaskAllMailResponse extends AsyncTask<String, Void, List<Thread>> {

        @Override
        protected List<Thread> doInBackground(String... strings) {
            String json = strings[0];
            return JsonUtilFactory.getJsonUtil().fromJsonArray(json, Thread.class);
        }

        @Override
        protected void onPostExecute(List<Thread> list) {
            super.onPostExecute(list);
            mAllAccountsThreadList.addAll(list);
            mLastAccountLoaded++;

            if (mLastAccountLoaded == mAccountInfoList.size() && getActivity() != null) {
                mBaseAdapter.updateData(mAllAccountsThreadList);
                mSectionedAdapter.notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
                mSwipeRefreshLayout.setEnabled(true);

                PlanckMailApplication app = (PlanckMailApplication) getActivity().getApplication();
                boolean isLoadDb = app.getLoadDB();

                if (isLoadDb) {
                    loadEmailWithMessage();
                }
                hideProgress(true, 0);

                matchFollowUpSectionsToList();
                hideMainProgress();
            }
        }
    }

    public void hideMainProgress() {
        if (mAllAccountsThreadList.isEmpty()) {
            mTvNoMails.setVisibility(VISIBLE);
        }
        mProgressLoading.setVisibility(GONE);
    }

    public void failureMailResponse(RetrofitError restError) {
        mBaseAdapter.stopLoading(true);
        Response r = restError.getResponse();
        if (r != null)
            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());

        mSectionedAdapter.notifyDataSetChanged();
        mProgressLoading.setVisibility(GONE);
    }

    public void saveDataToCache(String json) {
        int childId = getArguments().getInt(BundleKeys.CHILD_ID);
        Intent intent = new Intent(MenuActivity.CACHING_ACTION);
        intent.putExtra(BundleKeys.CHILD_ID, childId);

        addCacheType(intent);
        setGroupId(intent);

        intent.putExtra(BundleKeys.CACHED_DATA, json);
        intent.putExtra(BundleKeys.ENUM_MENU, MenuActivity.EnumMenuActivity.THREAD);

        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    public void addCacheType(Intent intent) {
        switch (mTargetFolder) {
            case READ_LATER:
                intent.putExtra(BundleKeys.CACHED_EMAIL_TYPE, TARGET_FOLDER.READ_LATER);
                break;
            case READ_NOW:
                intent.putExtra(BundleKeys.CACHED_EMAIL_TYPE, TARGET_FOLDER.READ_NOW);
                break;
            case FOLLOW_UP:
                intent.putExtra(BundleKeys.CACHED_EMAIL_TYPE, TARGET_FOLDER.FOLLOW_UP);
                break;
            case FOLDERS:
                intent.putExtra(BundleKeys.CACHED_EMAIL_TYPE, TARGET_FOLDER.FOLDERS);
                break;
            case SOCIAL:
                intent.putExtra(BundleKeys.CACHED_EMAIL_TYPE, TARGET_FOLDER.SOCIAL);
                break;
        }
    }

    public void setGroupId(Intent intent) {
        int groupId = getArguments().getInt(BundleKeys.GROUP_ID);

        if (mAccountInfoList.size() >= MAIL_COUNT && groupId == 0) {
            intent.putExtra(BundleKeys.GROUP_ID, ALL_ACCOUNT_GROUP_KEY);
        } else
            intent.putExtra(BundleKeys.GROUP_ID, groupId);
    }

    @Override
    public void loadedListThreads(List<Thread> list) {
        mProgressLoading.setVisibility(GONE);
        Log.d(PlanckMailApplication.TAG, "saved thread objects in db" + String.valueOf(list.size()));

        if (list.isEmpty()) {
            mTvNoMails.setVisibility(VISIBLE);
            mBaseAdapter.updateData(list);
            mSectionedAdapter.notifyDataSetChanged();
        } else {
            insertElements(list);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(messageReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageActionReceiver);
    }

    @Override
    public void onRefresh() {
        mRecycleScrollListener.reset(0, true);
        mBaseAdapter.stopLoading(true);
        mSwipeRefreshLayout.setRefreshing(true);

        mAllAccountsThreadList.clear();
        fillDataList();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_inbox, menu);
        final MenuItem mSearchMenu = menu.findItem(R.id.action_search);

        SearchManager manager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        final SearchView search = (SearchView) menu.findItem(R.id.action_search).getActionView();
        closeButtonListener(mSearchMenu, search);
        search.setSearchableInfo(manager.getSearchableInfo(getActivity().getComponentName()));
        search.setOnQueryTextListener(this);
    }

    public void closeButtonListener(final MenuItem mSearchMenu, final SearchView search) {
        // Get the search close button image view
        ImageView closeButton = (ImageView) search.findViewById(R.id.search_close_btn);
        // Set on click listener
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Find EditText view
                EditText et = (EditText) search.findViewById(R.id.search_src_text);
                //Clear the text from EditText view
                et.setText("");
                //Clear query
                search.setQuery("", false);
                //Collapse the action view
                search.onActionViewCollapsed();
                //Collapse the search widget
                mSearchMenu.collapseActionView();
                fillDataList();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String text = data.getStringExtra(BundleKeys.TOAST_TEXT);

                Snackbar snackbar = Snackbar.make(mLayout, text, Snackbar.LENGTH_SHORT);
                View snackBarView = snackbar.getView();
                snackBarView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.lightGreen));
                snackbar.show();
            }
        }
    }

    private void loadEmailWithMessage() {
        PlanckMailApplication app = (PlanckMailApplication) getActivity().getApplication();
        app.setLoadDB(false);

        //clean db for avoid data duplication
        DataBaseHelper.cleanDb();
        final ArrayList<ThreadDB> threadDbCollection = new ArrayList<>();
        final int[] counter = new int[1];
        for (AccountInfo accountInfo : mAccountInfoList) {
            RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
            PlanckService service = client.getPlankService();

            service.getThreadsList(accountInfo.accessToken, DEFAULT_COUNT_LOAD_EMAILS, new Callback<String>() {
                        @Override
                        public void success(String listThreads, Response response) {
                            counter[0]++;
                            List<ThreadDB> list = JsonUtilFactory.getJsonUtil().fromJsonArray(listThreads, ThreadDB.class);

                            if (list != null)
                                threadDbCollection.addAll(list);

                            if (counter[0] == mAccountInfoList.size() && isVisible()) {

                                AsyncSaveEmailInDb task = new AsyncSaveEmailInDb(threadDbCollection);
                                task.execute();
                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            //do nothing
                        }
                    }
            );
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                showFilterPopupMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        for (int i = 0; i < mAccountInfoList.size(); i++) {
            AccountInfo accountInfo = mAccountInfoList.get(i);

            NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
            nylasServer.getAllMailsInbox(newText, new Callback<Object>() {
                @Override
                public void success(Object o, Response response) {
                    Log.i(PlanckMailApplication.TAG, "response url " + response.getUrl());

                    String json = (String) o;
                    ArrayList<Thread> list = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Thread.class);

                    clearThreadsList();
                    mAllAccountsThreadList.addAll(list);

                    if (list.isEmpty()) {
                        mTvNoMails.setVisibility(VISIBLE);
                        mTvNoMails.setText(R.string.noResults);

                    } else {
                        mTvNoMails.setVisibility(GONE);
                        mTvNoMails.setText(R.string.no_mails);
                    }
                    updateAdapter(mAllAccountsThreadList);
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
            });
        }
        return true;
    }

    private void showFilterPopupMenu() {
        View view = ((MenuActivity) getActivity()).getToolbar();
        PopupMenu popupMenu = new PopupMenu(getActivity(), view, Gravity.RIGHT);
        popupMenu.inflate(R.menu.menu_compose_popup_filter);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.filterUnread:
                        filterUnreadThreads();
                        break;
                    case R.id.filterFiles:
                        filterFilesThread();
                        break;
                    case R.id.filterClear:
                        updateAdapter(mAllAccountsThreadList);
                        break;
                }
                return false;
            }

            public void filterUnreadThreads() {
                List<Thread> listUnread = new ArrayList<>();

                for (Thread thread : mAllAccountsThreadList) {
                    if (thread.unread)
                        listUnread.add(thread);
                }
                updateAdapter(listUnread);
            }

            public void filterFilesThread() {
                List<Thread> listFiles = new ArrayList<>();

                for (Thread thread : mAllAccountsThreadList) {
                    if (thread.has_attachments)
                        listFiles.add(thread);
                }
                updateAdapter(listFiles);
            }
        });
        popupMenu.show();
    }

    public enum TARGET_FOLDER {
        READ_NOW, READ_LATER, FOLLOW_UP, SOCIAL, FOLDERS
    }

    public enum ScheduleTime {
        TODAY("today"), THIS_EVENING("This evening"), TOMORROW("Tomorrow"), THIS_WEEKEND("This week"), NEXT_WEEK("Next week"),
        NEXT_TWO_WEEKS("Next two weeks"), NEXT_THREE_WEEKS("Next tree weeks"), NEXT_MONTH("In next month"), ANOTHER_DATE("Another date");

        private String schedule;

        ScheduleTime(String schedule) {
            this.schedule = schedule;
        }

        @Override
        public String toString() {
            return schedule;
        }
    }
}


