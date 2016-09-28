package com.planckmail.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.planckmail.R;
import com.planckmail.adapters.RecycleMessageAdapter;
import com.planckmail.adapters.RecycleThreadAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.beans.MessageDB;
import com.planckmail.data.db.beans.ParticipantDB;
import com.planckmail.data.db.beans.ThreadDB;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.data.db.manager.ThreadDataManager;
import com.planckmail.dialogs.SwipeDialog;
import com.planckmail.enums.Folders;
import com.planckmail.fragments.BaseFragment;
import com.planckmail.dialogs.SwipeConfirmationDialog;
import com.planckmail.fragments.ThreadFragment;
import com.planckmail.helper.InternetConnection;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.request.nylas.UpdateTag;
import com.planckmail.web.response.nylas.Message;
import com.planckmail.web.response.nylas.Thread;
import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.TypedJsonString;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.service.NylasService;
import com.planckmail.web.restClient.service.PlanckService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;

/**
 * Created by Taras Matolinets on 19.05.15.
 */
public class MessageActivity extends BaseActivity implements BaseFragment.OnBackPressed, View.OnClickListener, SwipeDialog.IPickedCustomDate, SwipeConfirmationDialog.OnConfirmButtonClick {

    private static final int REQUEST_CODE = 222;
    private static final String GMAIL = "gmail";

    private RecycleMessageAdapter mAdapter;
    private RecyclerView mRecycleMessages;
    private ProgressBar mProgress;
    private TextView mTvNoInternetConnection;
    private CoordinatorLayout mLayout;
    private RecyclerView.AdapterDataObserver mObserver;
    private List<AccountInfo> mAllAccountInfoList;
    private ArrayList<Message> mMessageList;
    public FrameLayout flReply;
    public FrameLayout flReplyAll;
    public FrameLayout flCompose;

    public FrameLayout flExpand;
    public FrameLayout flSummery;
    public FrameLayout flSnooze;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_message);
        initViews();
        setListener();
        setAdapter();
        mAllAccountInfoList = UserHelper.getEmailAccountList(true);

        if (InternetConnection.isNetworkConnected(this)) {
            getMails();
        } else {
            AsyncShowMessageFromDB task = new AsyncShowMessageFromDB();
            task.execute();
        }
    }

    private void setListener() {
        flCompose.setOnClickListener(this);
        flReply.setOnClickListener(this);
        flReplyAll.setOnClickListener(this);
        flSnooze.setOnClickListener(this);
        flExpand.setOnClickListener(this);
        flSummery.setOnClickListener(this);
    }

    private void setAdapter() {
        ArrayList<Message> list = new ArrayList<>();

        mAdapter = new RecycleMessageAdapter(this, list);
        mRecycleMessages.setAdapter(mAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecycleMessages.setLayoutManager(layoutManager);
        mRecycleMessages.setHasFixedSize(true);

        mRecycleMessages.scrollToPosition(list.size() + 1);
        implementObserver();
    }

    private void implementObserver() {
        mObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mRecycleMessages.smoothScrollToPosition(0);
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdapter.registerAdapterDataObserver(mObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.unregisterAdapterDataObserver(mObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.unregisterReceiver();
    }

    private void getMails() {
        String threadJson = getIntent().getStringExtra(BundleKeys.THREAD);
        Thread thread = JsonUtilFactory.getJsonUtil().fromJson(threadJson, Thread.class);

        String accessToken = getIntent().getStringExtra(BundleKeys.ACCESS_TOKEN);

        final NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accessToken, "");
        nylasServer.getMessages(thread.id, new Callback<Object>() {

                    @Override
                    public void success(Object o, Response response) {
                        mTvNoInternetConnection.setVisibility(View.GONE);
                        String json = (String) o;

                        Log.i(PlanckMailApplication.TAG, "response url " + response.getUrl());

                        mMessageList = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Message.class);

                        mProgress.setVisibility(View.GONE);

                        mAdapter.setListData(mMessageList);
                        mAdapter.notifyDataSetChanged();

                        mRecycleMessages.scrollToPosition(mMessageList.size() + 1);

                        storeLocalCaching(json, mMessageList);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                }
        );
    }

    public void storeLocalCaching(String json, List<Message> messageList) {
        Intent intent = new Intent(MenuActivity.CACHING_ACTION);
        intent.putExtra(BundleKeys.CACHED_DATA, json);
        intent.putExtra(BundleKeys.THREAD, messageList.get(0).thread_id);
        intent.putExtra(BundleKeys.ENUM_MENU, MenuActivity.EnumMenuActivity.MESSAGE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String text = data.getStringExtra(BundleKeys.TOAST_TEXT);
                Snackbar.make(mLayout, text, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        final Message message = mMessageList.get(mMessageList.size() - 1);

        switch (view.getId()) {
            case R.id.flForward:
                startComposeActivity(2, message);
                break;
            case R.id.flReply:
                startComposeActivity(0, message);
                break;
            case R.id.flReplyAll:
                startComposeActivity(1, message);
                break;
            case R.id.flExpand:
                mAdapter.setExpandAllViews(true);
                mAdapter.setShowSummarizeText(false);
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.flSummery:
                mAdapter.setExpandAllViews(true);
                mAdapter.setShowSummarizeText(true);
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.flSnooze:
                String threadJson = getIntent().getStringExtra(BundleKeys.THREAD);
                Thread thread = JsonUtilFactory.getJsonUtil().fromJson(threadJson, Thread.class);

                Bundle bundle = new Bundle();
                bundle.putString(BundleKeys.ID, thread.id);
                bundle.putBoolean(BundleKeys.IS_THREAD, false);

                SwipeDialog dialog = new SwipeDialog();
                dialog.setListener(this);
                dialog.setArguments(bundle);
                dialog.show(getFragmentManager(), null);
                break;
        }
    }

    private void startComposeActivity(int type, Message message) {
        String accessToken = getIntent().getStringExtra(BundleKeys.ACCESS_TOKEN);

        String json = JsonUtilFactory.getJsonUtil().toJson(mMessageList);
        Intent intent = new Intent(this, ComposeActivity.class);
        intent.putExtra(BundleKeys.MESSAGES, json);
        intent.putExtra(BundleKeys.MESSAGE_TYPE, type);
        intent.putExtra(BundleKeys.ACCESS_TOKEN, accessToken);

        int groupId = getIntent().getIntExtra(BundleKeys.GROUP_ID, -1);
        if (groupId != -1)
            intent.putExtra(BundleKeys.GROUP_ID, groupId);

        String singleJsonMessage = JsonUtilFactory.getJsonUtil().toJson(message);
        intent.putExtra(BundleKeys.SINGLE_MESSAGE, singleJsonMessage);

        startActivity(intent);
    }

    @Override
    public void pickedDate(SwipeLayout swipeLayout, RecycleThreadAdapter.SWIPE_TYPE swipeType, long millis) {
        String threadJson = getIntent().getStringExtra(BundleKeys.THREAD);
        Bundle bundle = new Bundle();
        bundle.putLong(BundleKeys.TIME, millis);
        bundle.putBoolean(BundleKeys.IS_MESSAGE, true);
        bundle.putString(BundleKeys.THREAD, threadJson);

        SwipeConfirmationDialog fragment = new SwipeConfirmationDialog();
        fragment.setArguments(bundle);
        fragment.setListener(this);
        fragment.show(getFragmentManager(), fragment.getClass().getName());
    }

    @Override
    public void pickedDate(String threadId, long millis) {
    }

    @Override
    public void confirmSwipeActionClick(RecycleThreadAdapter.SWIPE_TYPE type, SwipeLayout swipe, Thread thread, long millis, int position, boolean remindState) {

    }

    @Override
    public void confirmSwipeSnoozeClick(Thread thread, long millis) {
        AccountInfo accountInfo = getAccount(mMessageList.get(0));
        setSnoozeFolder(thread.id, accountInfo, millis);
    }

    private AccountInfo getAccount(Message message) {
        // set account for getting contacts
        for (AccountInfo accountInfo : mAllAccountInfoList) {
            if (accountInfo.getAccountId().equalsIgnoreCase(message.account_id)) {
                return accountInfo;
            }
        }
        return null;
    }

    private void setSnoozeFolder(String id, AccountInfo accountInfo, long millis) {
        RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
        PlanckService service = client.getPlankService();
        service.add_thread_to_snooze(accountInfo.email, id, "abc", accountInfo.accessToken, millis, new Callback<String>() {
            @Override
            public void success(String s, Response response) {
                Log.i(PlanckMailApplication.TAG, "response snooze " + s);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null) {
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
            }
        });
    }

    private class AsyncShowMessageFromDB extends AsyncTask<Void, ThreadDB, ThreadDB> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected ThreadDB doInBackground(Void... voids) {
            String threadJson = getIntent().getStringExtra(BundleKeys.THREAD);
            Thread thread = JsonUtilFactory.getJsonUtil().fromJson(threadJson, Thread.class);

            ThreadDataManager threadManager = (ThreadDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.THREAD_MANAGER);
            return threadManager.getThreadById(thread.id);
        }

        @Override
        protected void onPostExecute(ThreadDB threadDB) {
            super.onPostExecute(threadDB);
            mProgress.setVisibility(View.GONE);

            if (threadDB == null)
                mTvNoInternetConnection.setVisibility(View.VISIBLE);
            else {
                Collection<MessageDB> list = threadDB.getMessageList();
                List<Message> listMessage = new ArrayList<>();

                for (MessageDB m : list) {
                    Message message = createMessage(m);
                    listMessage.add(message);
                }
                String json = JsonUtilFactory.getJsonUtil().toJson(listMessage);

                List<Message> messageList = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Message.class);

                mProgress.setVisibility(View.GONE);

                mAdapter.setListData(messageList);
                mAdapter.notifyDataSetChanged();

                mRecycleMessages.scrollToPosition(messageList.size() + 1);
            }
        }

    }

    public Message createMessage(MessageDB m) {
        Message message = new Message();

        message.body = m.body;
        message.date = m.date;
        message.id = m.id;
        message.thread_id = m.thread_id;
        message.object = m.object;
        message.unread = m.unread;
        message.snippet = m.snippet;
        message.subject = m.subject;

        message.replyTo = createParticipantsList(m.replyTo);
        message.from = createParticipantsList(m.from);
        message.bcc = createParticipantsList(m.bcc);
        message.cc = createParticipantsList(m.cc);
        message.to = createParticipantsList(m.to);

        return message;
    }

    public List<Participant> createParticipantsList(Collection<ParticipantDB> listParticipantDb) {
        List<Participant> listParticipant = new ArrayList<>();
        for (ParticipantDB p : listParticipantDb) {
            Participant participant = new Participant();

            participant.email = p.email;
            participant.name = p.name;

            listParticipant.add(participant);
        }
        return listParticipant;
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.messageToolbar);
        setSupportActionBar(toolbar);
        String subTitle = getIntent().getExtras().getString(BundleKeys.SUB_TITLE);

        String title = getIntent().getExtras().getString(BundleKeys.TITLE);

        if (!TextUtils.isEmpty(subTitle))
            toolbar.setSubtitle(subTitle);

        setTitle(title);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mRecycleMessages = (RecyclerView) findViewById(R.id.recycleMessage);
        mProgress = (ProgressBar) findViewById(R.id.prLoadMessage);
        mTvNoInternetConnection = (TextView) findViewById(R.id.tvNoInternetConnection);
        mLayout = (CoordinatorLayout) findViewById(R.id.coordinateLayout);
        flReply = (FrameLayout) findViewById(R.id.flReply);
        flReplyAll = (FrameLayout) findViewById(R.id.flReplyAll);
        flCompose = (FrameLayout) findViewById(R.id.flForward);
        flExpand = (FrameLayout) findViewById(R.id.flExpand);
        flSummery = (FrameLayout) findViewById(R.id.flSummery);
        flSnooze = (FrameLayout) findViewById(R.id.flSnooze);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.delete:
                String tagJson = updateTag(Folders.TRASH);
                moveMessage(R.string.deleteMessage, tagJson);
                break;

            case R.id.archive:
                String tagArchived = updateTag(Folders.ALL_MAIL);
                moveMessage(R.string.archivedMessage, tagArchived);
                break;
            case R.id.move:
                final View menuItemView = findViewById(item.getItemId());
                PopupMenu popupMenu = new PopupMenu(this, menuItemView, Gravity.RIGHT);
                popupMenu.inflate(R.menu.menu_move_mail);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.itemInbox:
                                String tagInbox = updateTag(Folders.INBOX);
                                moveMessage(R.string.moveInbox, tagInbox);
                                break;
                            case R.id.itemStarred:
                                String tagStarred = updateTag(Folders.STARRED);
                                moveMessage(R.string.moveStarred, tagStarred);
                                break;
                            case R.id.itemSpam:
                                String tagSpam = updateTag(Folders.SPAM);
                                moveMessage(R.string.moveSpam, tagSpam);
                                break;
                        }
                        return false;
                    }
                });
                popupMenu.show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void moveMessage(final int message, String tagJson) {
        String threadJson = getIntent().getStringExtra(BundleKeys.THREAD);
        Thread thread = JsonUtilFactory.getJsonUtil().fromJson(threadJson, Thread.class);

        String accessToken = getIntent().getStringExtra(BundleKeys.ACCESS_TOKEN);

        final NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accessToken, "");

        final TypedInput in = new TypedJsonString(tagJson);
        nylasServer.updateRemoveThread(thread.id, in, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.d(PlanckMailApplication.TAG, response.getUrl());

                Intent intent = new Intent(ThreadFragment.MESSAGE_ACTION);
                intent.putExtra(BundleKeys.TOAST_TEXT, getString(message));

                LocalBroadcastManager.getInstance(MessageActivity.this).sendBroadcast(intent);
                getFragmentManager().popBackStackImmediate();
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private String updateTag(Folders currentTag) {
        //TODO: use folders for move thread to specific folder
        UpdateTag tag = new UpdateTag();
        tag.add_tags.add(currentTag.toString());

        int childId = getIntent().getIntExtra(BundleKeys.CHILD_ID, -1);

        switch (childId) {
            case MenuActivity.INBOX:
                tag.remove_tags.add(Folders.INBOX.toString());
                break;
        }
        return JsonUtilFactory.getJsonUtil().toJson(tag);
    }

    @Override
    public void onBackPress() {
        if (mAdapter.getShowSummarizeText()) {
            mAdapter.setShowSummarizeText(false);
            mAdapter.setExpandAllViews(false);
            mAdapter.notifyDataSetChanged();
        } else
            finish();
    }
}
