package com.planckmail.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.onegravity.rteditor.RTEditText;
import com.onegravity.rteditor.RTManager;
import com.onegravity.rteditor.api.RTApi;
import com.onegravity.rteditor.api.RTMediaFactoryImpl;
import com.onegravity.rteditor.api.RTProxyImpl;
import com.planckmail.R;
import com.planckmail.adapters.AutoCompleteContactAdapter;
import com.planckmail.adapters.RecycleThreadAdapter;
import com.planckmail.adapters.SpinnerAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.PlanckMailSharePreferences;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.model.TrackedItem;
import com.planckmail.dialogs.DraftDialogFragment;
import com.planckmail.dialogs.SwipeDialog;
import com.planckmail.enums.Folders;
import com.planckmail.fragments.AllAccountsFragment;
import com.planckmail.fragments.BoxDriveFileFragment;
import com.planckmail.fragments.DropBoxFileFragment;
import com.planckmail.fragments.GoogleDriveFileFragment;
import com.planckmail.fragments.OneDriveFileFragment;
import com.planckmail.dialogs.SwipeConfirmationDialog;
import com.planckmail.helper.AvailabilityDescription;
import com.planckmail.helper.InternetConnection;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.views.ContactsCompletionView;
import com.planckmail.web.request.nylas.SendDraft;
import com.planckmail.web.request.nylas.SendMessage;
import com.planckmail.web.response.nylas.AvailabilityTime;
import com.planckmail.web.response.nylas.Contact;
import com.planckmail.web.response.nylas.Draft;
import com.planckmail.web.response.nylas.Message;
import com.planckmail.web.response.nylas.Thread;
import com.planckmail.web.response.nylas.wrapper.File;
import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.response.nylas.wrapper.Tag;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.TypedJsonString;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.service.NylasService;
import com.planckmail.web.restClient.service.PlanckService;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import io.fabric.sdk.android.services.concurrency.AsyncTask;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedInput;

/**
 * Created by Taras Matolinets on 24.05.15.
 */
public class ComposeActivity extends BaseActivity implements AdapterView.OnItemSelectedListener, View.OnTouchListener, View.OnClickListener, TextWatcher, SwipeDialog.IPickedCustomDate, SwipeConfirmationDialog.OnConfirmButtonClick {

    public static final String CACHING_ACTION = "com.plancklabs.local.save.file";
    private static final String DIALOG_FRAGMENT = "draft_fragment";
    public static final String IMAGE_FILE_FORMAT = ".png";
    private static final String MB = "mb";
    private static final String KB = "kb";
    private static final int RESULT_LOAD_IMAGE_CAMERA = 223;
    private static final int RESULT_LOAD_IMAGE = 123;
    public static final int RESULT_CREATE_AVAILABILITY = 444;
    public static final int DRAFT_ID = 1;
    private static final int LIMIT_LOAD_MAILS = 600;
    public static final int RESULT_EVENT_CREATED = 101;
    public static final double SIZE_DIVIDER = 1024.0;
    private static final int RESULT_FILE_FROM_ACCOUNT_ADD = 555;
    public static final int REPLY = 0;
    public static final int REPLY_ALL = 1;
    public static final int FORWARD = 2;
    public static final int MILLISECONDS = 1000;

    private boolean mIsNotifyClicked;
    private long mMillisFoNotifyMe;
    private int mNextAvailabilityFrame;
    private String mBodyMessage;
    private Spinner mSpinnerEmail;
    private List<AccountInfo> mAccountInfoList;
    private ContactsCompletionView mCcAutoComplete;
    private ContactsCompletionView mBccAutoComplete;
    private ContactsCompletionView mToAutoComplete;
    private EditText mEtSubject;
    private LinearLayout mLlBccCompose;
    private LinearLayout mLlMailFile;
    private AutoCompleteContactAdapter mAdapterAutoCompiler;
    private TextView tvCcCompose;
    private ProgressBar mProgressTo;
    private ProgressBar mProgressBcc;
    private ProgressBar mProgressCc;
    private RTEditText mEtComposeMessage;
    private TextView mTvFullMessage;
    private Message mMessage;
    private TextView mTvFileName;
    private LinearLayout mLayoutSendAvailability;
    private TextView mTvMeeting;
    private TextView mTvLength;
    private TextView mTvLocation;
    private TextView mTvTimeZone;
    private TextView mNoneTime;
    private TextView mTvDeleteAvailability;
    private View mViewCreateAvailability;
    private CardView mCardAvailability;
    private CardView mCardNoAvailableTime;
    private Draft mDraft;
    private CoordinatorLayout mLayout;

    private ArrayList<java.io.File> mArrayFileUpload = new ArrayList<>();
    private ArrayList<Contact> mMainListContacts = new ArrayList<>();
    private ArrayList<File> mArrayFileResponse = new ArrayList<>();
    private ArrayList<String> mArrayFileIds = new ArrayList<>();
    private HashMap<String, String> mMapFileCachedData = new HashMap<>();
    private View mViewFile;
    private AvailabilityDescription mDescription;

    private ProgressBar mProgressBar;

    private String mTrackId;
    private boolean mTrackingMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mCacheActionReceiver, new IntentFilter(CACHING_ACTION));

        initViews();
        setListeners();
        initActionBar();
        RTApi rtApi = new RTApi(this, new RTProxyImpl(this), new RTMediaFactoryImpl(this, true));
        RTManager rTManager = new RTManager(rtApi, savedInstanceState);

        rTManager.registerEditor(mEtComposeMessage, true);

        mAccountInfoList = UserHelper.getEmailAccountList(true);
        fillData();
    }

    private void initViews() {
        mSpinnerEmail = (Spinner) findViewById(R.id.spMail);
        mCcAutoComplete = (ContactsCompletionView) findViewById(R.id.acCc);
        mBccAutoComplete = (ContactsCompletionView) findViewById(R.id.acBcc);
        mToAutoComplete = (ContactsCompletionView) findViewById(R.id.acTo);
        mEtSubject = (EditText) findViewById(R.id.etSubject);
        mEtComposeMessage = (RTEditText) findViewById(R.id.etComposeMessage);
        mLlBccCompose = (LinearLayout) findViewById(R.id.llBccCompose);
        mTvFileName = (TextView) findViewById(R.id.tvFiles);
        mLlMailFile = (LinearLayout) findViewById(R.id.llMailFile);
        tvCcCompose = (TextView) findViewById(R.id.tvCcCompose);
        mProgressTo = (ProgressBar) findViewById(R.id.prAcTo);
        mProgressCc = (ProgressBar) findViewById(R.id.prCcTo);
        mProgressBcc = (ProgressBar) findViewById(R.id.prBccTo);
        mTvFullMessage = (TextView) findViewById(R.id.tvFullMessage);
        mLayout = (CoordinatorLayout) findViewById(R.id.coordinateLayout);
        mViewCreateAvailability = findViewById(R.id.viewCreateAvailability);
        mViewFile = findViewById(R.id.viewFile);
        mCardAvailability = (CardView) findViewById(R.id.cardViewAvailability);
        mCardNoAvailableTime = (CardView) findViewById(R.id.cardNotAvailability);
        mLayoutSendAvailability = (LinearLayout) findViewById(R.id.llAvailability);
        mTvDeleteAvailability = (TextView) findViewById(R.id.tvDeleteAvailability);
        mTvMeeting = (TextView) findViewById(R.id.tvMeeting);
        mTvLength = (TextView) findViewById(R.id.tvLength);
        mTvLocation = (TextView) findViewById(R.id.tvLocation);
        mTvTimeZone = (TextView) findViewById(R.id.tvTimeZone);
        mNoneTime = (TextView) findViewById(R.id.tvNoneTime);

        mProgressBar = (ProgressBar) findViewById(R.id.compose_pb);
    }

    private void setListeners() {
        mCcAutoComplete.setOnTouchListener(this);
        mTvFullMessage.setOnClickListener(this);
        mSpinnerEmail.setOnItemSelectedListener(this);
        mToAutoComplete.addTextChangedListener(this);
        mCcAutoComplete.addTextChangedListener(this);
        mBccAutoComplete.addTextChangedListener(this);
        mTvDeleteAvailability.setOnClickListener(this);
        mNoneTime.setOnClickListener(this);
    }

    private void saveCachedData(String accountId, String json) {
        mMapFileCachedData.put(accountId, json);
    }

    private String getJsonData(String accountId) {
        String data = "";
        if (mMapFileCachedData.containsKey(accountId))
            data = mMapFileCachedData.get(accountId);
        return data;
    }

    private void fillData() {
        ArrayList<String> list = new ArrayList<>();

        for (AccountInfo accountInfo : mAccountInfoList)
            list.add(accountInfo.email);

        SpinnerAdapter adapter = new SpinnerAdapter(this, list, R.layout.elem_spinner);
        mSpinnerEmail.setAdapter(adapter);

        mAdapterAutoCompiler = new AutoCompleteContactAdapter(this);

        mToAutoComplete.setAdapter(mAdapterAutoCompiler);
        mCcAutoComplete.setAdapter(mAdapterAutoCompiler);
        mBccAutoComplete.setAdapter(mAdapterAutoCompiler);

        final int childId = getIntent().getIntExtra(BundleKeys.CHILD_ID, 0);

        if (childId != DRAFT_ID) {
            String jsonSingleMessage = getIntent().getStringExtra(BundleKeys.SINGLE_MESSAGE);
            mMessage = JsonUtilFactory.getJsonUtil().fromJson(jsonSingleMessage, Message.class);
            setDataFromIntent();
        } else
            getDraft();
    }

    private void setAvailableTimeInterval(List<List<Long>> intervalsList) {
        //clean all views for avoid duplication
        mLayoutSendAvailability.removeAllViews();

        for (int i = 0; i < intervalsList.size(); i++) {
            List<Long> listItem = intervalsList.get(i);
            final View view = View.inflate(this, R.layout.elem_availability, null);

            int marginView = getResources().getDimensionPixelSize(R.dimen.edge_normal);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(marginView, marginView, marginView, marginView);

            TextView tvDayAvailability = (TextView) view.findViewById(R.id.tvAvailabilityDate);
            TextView tvStartAvailability = (TextView) view.findViewById(R.id.tvAvailabilityStart);
            TextView tvEndAvailability = (TextView) view.findViewById(R.id.tvAvailabilityEnd);
            CardView cardElemAvailability = (CardView) view.findViewById(R.id.cardViewElementAvailability);

            cardElemAvailability.setOnClickListener(this);

            long startAvailableTime = listItem.get(0);
            long endAvailableTime = listItem.get(1);

            DateTime dateStart = new DateTime(startAvailableTime * MILLISECONDS);
            DateTime dateEnd = new DateTime(endAvailableTime * MILLISECONDS);

            mDescription.setStartTime(dateStart.getMillis() * MILLISECONDS);
            mDescription.setEndTime(dateEnd.getMillis() * MILLISECONDS);

            DateTimeFormatter formatterDate = DateTimeFormat.forPattern("E MM/dd");
            String dateTime = formatterDate.print(dateStart);

            DateTimeFormatter formatterDateStart = DateTimeFormat.forPattern("hh:mm a");
            String timeStart = formatterDateStart.print(dateStart);

            DateTimeFormatter formatterDateEnd = DateTimeFormat.forPattern("hh:mm a");
            String timeEnd = formatterDateEnd.print(dateEnd);

            tvDayAvailability.setText(dateTime);
            tvStartAvailability.setText(timeStart);
            tvEndAvailability.setText(timeEnd);

            mLayoutSendAvailability.addView(view, layoutParams);
        }
    }

    private BroadcastReceiver mCacheActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String jsonList = intent.getStringExtra(BundleKeys.CACHED_DATA);
            int position = mSpinnerEmail.getSelectedItemPosition();
            AccountInfo accountInfo = mAccountInfoList.get(position);
            saveCachedData(accountInfo.getAccessToken(), jsonList);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mCacheActionReceiver);
    }

    private void getDraft() {
        final ArrayList<String> listDrafts = getIntent().getStringArrayListExtra(BundleKeys.DRAFT_ID);
        int position = mSpinnerEmail.getSelectedItemPosition();
        AccountInfo accountInfo = mAccountInfoList.get(position);

        int defaultObject = 0;

        final NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
        if (!listDrafts.isEmpty())
            nylasServer.getDraft(listDrafts.get(defaultObject), new Callback<Object>() {
                @Override
                public void success(Object o, Response response) {
                    String json = (String) o;

                    Log.i(PlanckMailApplication.TAG, "response url " + response.getUrl());

                    mDraft = JsonUtilFactory.getJsonUtil().fromJson(json, Draft.class);
                    setDataFromDraft(mDraft);
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "error: draft " + r.getReason());
                }
            });
    }

    private void setDataFromDraft(Draft draft) {
        if (draft.participants != null)
            for (Participant p : draft.participants)
                setContactFrom(p);

        mTvFileName.setVisibility(View.GONE);
        mEtSubject.setText(draft.subject);
        mEtComposeMessage.setText(Html.fromHtml(draft.body), TextView.BufferType.SPANNABLE);
        mToAutoComplete.setSelection(mToAutoComplete.getText().length());
    }

    public void showFile(File f) {
        if (mViewFile.getVisibility() == View.GONE) {
            mViewFile.setVisibility(View.VISIBLE);
            mTvFileName.setVisibility(View.VISIBLE);
        }

        View viewShowFile = View.inflate(this, R.layout.elem_file_message, null);

        TextView tvFileSize = (TextView) viewShowFile.findViewById(R.id.tvFileSize);
        TextView tvFileName = (TextView) viewShowFile.findViewById(R.id.tvFileName);
        TextView tvFileRemove = (TextView) viewShowFile.findViewById(R.id.tvFileRemove);

        ImageView ivFile = (ImageView) viewShowFile.findViewById(R.id.ivFile);
        ivFile.setImageResource(R.drawable.ic_attachment_black_message);

        tvFileRemove.setTag(f);
        tvFileRemove.setTag(R.string.tagRemoveView, viewShowFile);
        tvFileRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = (File) v.getTag();
                View removeView = (View) v.getTag(R.string.tagRemoveView);

                mLlMailFile.removeView(removeView);
                int childCount = mLlMailFile.getChildCount();
                int noViews = 0;
                if (childCount == noViews) {
                    mTvFileName.setVisibility(View.GONE);
                    mViewFile.setVisibility(View.GONE);
                }
                if (mMessage != null) {
                    mMessage.files.remove(file);
                }
            }
        });
        tvFileRemove.setVisibility(View.VISIBLE);
        setResponseFileSize(f, tvFileSize, tvFileName);
        mLlMailFile.addView(viewShowFile);
    }

    private void setResponseFileSize(File f, TextView tvFileSize, TextView tvFileName) {
        if (TextUtils.isEmpty(f.filename))
            tvFileName.setText(getText(R.string.noName));
        else
            tvFileName.setText(f.filename);

        int size = (int) (f.size / SIZE_DIVIDER);

        if (size >= SIZE_DIVIDER) {
            size = (int) ((f.size / SIZE_DIVIDER) / SIZE_DIVIDER);
            tvFileSize.setText(String.valueOf("(" + size + " " + MB + ")"));
        } else
            tvFileSize.setText(String.valueOf("(" + size + " " + KB + ")"));
    }

    private void setDataFromIntent() {
        int type = getIntent().getIntExtra(BundleKeys.MESSAGE_TYPE, -1);
        int position = getIntent().getIntExtra(BundleKeys.GROUP_ID, 0);

        String composeTo = getIntent().getStringExtra(BundleKeys.COMPOSE_TO);

        if (composeTo != null)
            mToAutoComplete.setText(composeTo);

        position = getPositionAction(type, position);
        mSpinnerEmail.setSelection(position);

        setMessageContent();
        mToAutoComplete.setSelection(mToAutoComplete.getText().length());
    }

    private int getPositionAction(int type, int position) {
        switch (type) {
            case REPLY:
                reply();
                break;
            case REPLY_ALL:
                replyAll();
                break;
            case FORWARD:
                position = forward(position);
                break;
        }
        return position;
    }

    private void setMessageContent() {
        if (mMessage != null) {
            mEtComposeMessage.setRichTextEditing(true, mMessage.body);
            mEtSubject.setText(mMessage.subject);

            if (mMessage.files.isEmpty()) {
                mTvFileName.setVisibility(View.GONE);
                mViewFile.setVisibility(View.GONE);
            } else {
                mViewFile.setVisibility(View.VISIBLE);
                mTvFileName.setVisibility(View.VISIBLE);
            }

            boolean isCreateMessage = getIntent().getBooleanExtra(BundleKeys.CREATE_MESSAGE, false);

            if (isCreateMessage || mMessage.files.isEmpty()) {
                mTvFullMessage.setVisibility(View.GONE);
            }
        }
    }

    private int forward(int position) {
        for (File f : mMessage.files) {
            showFile(f);
        }
        mSpinnerEmail.setEnabled(false);

        if (!mMessage.files.isEmpty()) {
            int i;
            for (i = 0; i < mAccountInfoList.size(); i++) {
                if (mMessage.account_id.equalsIgnoreCase(mAccountInfoList.get(i).accountId))
                    break;
            }
            position = i;
        }
        return position;
    }

    private void reply() {
        //get first email receiver
        addContact(mMessage.from.get(0));
        mSpinnerEmail.setEnabled(false);
    }

    private void replyAll() {
        for (Participant p : mMessage.from)
            setContactFrom(p);
        for (Participant p : mMessage.to)
            setContactFrom(p);
        mSpinnerEmail.setEnabled(false);
    }

    private void setContactFrom(Participant p) {
        Contact contact = new Contact();
        contact.setEmail(p.getEmail());
        contact.setName(p.getName());
        mToAutoComplete.addObject(contact);
    }

    private void addContact(Participant p) {
        Contact contact = new Contact();
        contact.setEmail(p.getEmail());
        contact.setName(p.getName());
        mToAutoComplete.addObject(contact);
    }

    public DataSetObserver observer = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            mProgressTo.setVisibility(View.GONE);
            mProgressCc.setVisibility(View.GONE);
            mProgressBcc.setVisibility(View.GONE);
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mProgressTo.setVisibility(View.GONE);
            mProgressCc.setVisibility(View.GONE);
            mProgressBcc.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mAdapterAutoCompiler.registerDataSetObserver(observer);

        if (mMainListContacts.isEmpty())
            getContacts();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapterAutoCompiler.unregisterDataSetObserver(observer);
    }

    private void initActionBar() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.compose);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose_message, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.flMainCompose);

        if (fragment instanceof DropBoxFileFragment)
            ((DropBoxFileFragment) fragment).onBackPress();
        else if (fragment instanceof OneDriveFileFragment)
            ((OneDriveFileFragment) fragment).onBackPress();
        else if (fragment instanceof AllAccountsFragment)
            ((AllAccountsFragment) fragment).onBackPress();
        else if (fragment instanceof GoogleDriveFileFragment)
            ((GoogleDriveFileFragment) fragment).onBackPress();
        else if (fragment instanceof BoxDriveFileFragment)
            ((BoxDriveFileFragment) fragment).onBackPress();
        else
            saveDraft();
    }

    public void saveDraft() {
        Bundle bundle = new Bundle();
        String jsonMessage;
        int position = mSpinnerEmail.getSelectedItemPosition();
        AccountInfo accountInfo = mAccountInfoList.get(position);

        if (mDraft == null) {
            SendDraft draft = createDraft();
            jsonMessage = JsonUtilFactory.getJsonUtil().toJson(draft);
        } else {
            updateDraft();
            jsonMessage = JsonUtilFactory.getJsonUtil().toJson(mDraft);
            bundle.putBoolean(BundleKeys.DRAFT_UPDATE, true);
        }

        bundle.putString(BundleKeys.DRAFT, jsonMessage);
        bundle.putString(BundleKeys.ACCESS_TOKEN, accountInfo.getAccessToken());

        showDialog(bundle);
    }

    public void showDialog(Bundle bundle) {
        DraftDialogFragment dialog = new DraftDialogFragment();
        dialog.setActivity(this);
        dialog.setArguments(bundle);

        dialog.show(getFragmentManager(), DIALOG_FRAGMENT);
    }

    private void updateDraft() {
        mDraft.setTo(mToAutoComplete.getObjects());
        mDraft.setCc(mCcAutoComplete.getObjects());
        mDraft.setBcc(mBccAutoComplete.getObjects());
        mDraft.setSubject(mEtSubject.getText().toString());
        mDraft.setBody(mEtComposeMessage.getText().toString().replace("\n", "<br>"));

        for (File file : mArrayFileResponse) {
            if (!mDraft.getFile_ids().contains(file.getId()))
                mDraft.file_ids.add(file.id);
        }

        for (String fileId : mArrayFileIds) {
            if (!mDraft.getFile_ids().contains(fileId))
                mDraft.file_ids.add(fileId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.send:
                if (mCardAvailability.getVisibility() == View.VISIBLE)
                    sendAvailabilityEvent();

                int position = mSpinnerEmail.getSelectedItemPosition();
                AccountInfo accountInfo = mAccountInfoList.get(position);

                if (send(accountInfo)) return false;
                break;
            case R.id.createInvitation:
                createInvitation(1);
                break;
            case R.id.sendAvailability:
                mNextAvailabilityFrame = 0;
                startAvailability();
                break;
            case R.id.addFiles:
                UserHelper.hideKeyboard(ComposeActivity.this, mLayout);
                showAddFilesDialog();
                break;
            case R.id.notifyMe:
                Bundle bundle = new Bundle();
                bundle.putBoolean(BundleKeys.IS_THREAD, false);

                SwipeDialog dialog = new SwipeDialog();
                dialog.setListener(this);
                dialog.setArguments(bundle);

                dialog.show(getFragmentManager(), null);
                break;
            case R.id.trackMail:
                createTrack();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createTrack() {
        mTrackingMe = !mTrackingMe;

        if(mTrackingMe && mTrackId==null) {
            PlanckService service = new RestPlankMail(RestPlankMail.BASE_URL4).getPlankService();

            mProgressBar.setVisibility(View.VISIBLE);
            service.createTrack(new Callback<String>() {

                @Override
                public void success(String s, Response response) {
                    mProgressBar.setVisibility(View.GONE);
                    try {
                        JSONObject jsonResult = new JSONObject(s);

                        if(jsonResult!=null && jsonResult.get("success")==true) {
                            mTrackId = jsonResult.getString("track_id");

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mProgressBar.setVisibility(View.GONE);
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null) {
                        Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                    mProgressBar.setVisibility(View.GONE);
                }
            });
        }

    }

    private void updateTrack(String trackId, Message message) {

        PlanckService service = new RestPlankMail(RestPlankMail.BASE_URL4).getPlankService();

        String ownerEmail = message.from.get(0).getEmail();
        StringBuilder targetEmails = new StringBuilder();

        for(int i=0; i<message.to.size(); i++) {
            if(i>0) targetEmails.append(",");

            Participant p = message.to.get(i);
            targetEmails.append(p.getEmail());
        }
        for(int i=0; i<message.cc.size(); i++) {
            if(i>0) targetEmails.append(",");

            Participant p = message.cc.get(i);
            targetEmails.append(p.getEmail());
        }
        for(int i=0; i<message.bcc.size(); i++) {
            if(i>0) targetEmails.append(",");

            Participant p = message.bcc.get(i);
            targetEmails.append(p.getEmail());
        }

        mProgressBar.setVisibility(View.VISIBLE);
        service.updateTrack(trackId, message.thread_id, message.id, message.subject, ownerEmail, targetEmails.toString(), new Callback<String>() {

            @Override
            public void success(String s, Response response) {
                mProgressBar.setVisibility(View.GONE);
                try {
                    JSONObject jsonResult = new JSONObject(s);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null) {
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
                mProgressBar.setVisibility(View.GONE);
            }
        });
    }

    public void showAddFilesDialog() {
        View v = findViewById(R.id.addFiles);
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.menu_compose_add_files);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.actionChoseFromAccounts:
                        Bundle bundle = new Bundle();
                        bundle.putInt(BundleKeys.VIEW_ID, R.id.flMainCompose);
                        replace(AllAccountsFragment.class, R.id.flMainCompose, bundle, true);
                        break;
                    case R.id.actionChoseFromPhotos:
                        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(i, RESULT_LOAD_IMAGE);
                        break;
                    case R.id.actionTakePhoto:
                        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, RESULT_LOAD_IMAGE_CAMERA);
                        break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void sendAvailabilityEvent() {
        for (int i = 0; i < mLayoutSendAvailability.getChildCount(); i++) {
            View view = mLayoutSendAvailability.getChildAt(i);
            Object tag = view.getTag(R.string.tag_selected_availability);

            if (tag != null && (boolean) tag) {
                AvailabilityDescription description = (AvailabilityDescription) view.getTag(R.string.tag_availability_description);
                RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
                PlanckService service = client.getPlankService();
                service.createEvent(description.getSender(), description.getStartTime(), description.getEndTime(), description.getLocation(), description.getParticipants(), new Callback<String>() {
                    @Override
                    public void success(String s, Response response) {
                        Log.i(PlanckMailApplication.TAG, "sent availability status " + s);
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

        }
    }

    public void setDropBoxLink(String link, String name) {
        String dropBoxLink = "<a href =" + link + ">" + name + "</a>";

        mBodyMessage = Html.toHtml(mEtComposeMessage.getText()) + " " + dropBoxLink;
        mEtComposeMessage.setText(Html.fromHtml(mBodyMessage));
    }

    public void createInvitation(int eventType) {
        Intent intent = new Intent(ComposeActivity.this, EventTypeActivity.class);
        intent.putExtra(BundleKeys.KEY_EVENT_TYPE, eventType);
        startActivityForResult(intent, RESULT_EVENT_CREATED);
    }

    public void startAvailability() {
        ArrayList<String> listPeople = getEmailList();
        String jsoContacts = JsonUtilFactory.getJsonUtil().toJson(mMainListContacts);

        int position = mSpinnerEmail.getSelectedItemPosition();
        AccountInfo accountInfo = mAccountInfoList.get(position);

        Intent intent = new Intent(this, CreateAvailabilityActivity.class);
        intent.putExtra(BundleKeys.THREAD_SUBJECT, mEtSubject.getText().toString());
        intent.putExtra(BundleKeys.EMAIL, listPeople);
        intent.putExtra(BundleKeys.CONTACT, jsoContacts);
        intent.putExtra(BundleKeys.ACCESS_TOKEN, accountInfo.accessToken);
        startActivityForResult(intent, RESULT_CREATE_AVAILABILITY);
    }

    @NonNull
    private ArrayList<String> getEmailList() {
        ArrayList<String> listPeople = new ArrayList<>();
        List<Contact> listContactsTo = mToAutoComplete.getObjects();
        List<Contact> listContactsCc = mCcAutoComplete.getObjects();

        for (Contact contact : listContactsTo) {
            listPeople.add(contact.email);
        }

        for (Contact contact : listContactsCc) {
            listPeople.add(contact.email);
        }
        return listPeople;
    }

    public boolean send(AccountInfo accountInfo) {
        if (InternetConnection.isNetworkConnected(this)) {
            if (!checkEmailValid())
                return true;

            UserHelper.hideKeyboard(this, mLayout);
            prepareSendEmail(accountInfo);

        } else {
            sendMessageOfflineMode(accountInfo);
        }
        return false;
    }

    /**
     * When you want to send message in offline mode the message will be send when automatically when internet will appear
     *
     * @param accountInfo account for sending message
     */
    public void sendMessageOfflineMode(AccountInfo accountInfo) {
        SendMessage sendMessage = buildMessage();
        String messageJson = JsonUtilFactory.getJsonUtil().toJson(sendMessage);

        PlanckMailSharePreferences.setDataToSharePreferences(BundleKeys.MESSAGE, messageJson, PlanckMailSharePreferences.SHARE_PREFERENCES_TYPE.STRING);
        PlanckMailSharePreferences.setDataToSharePreferences(BundleKeys.ACCESS_TOKEN, accountInfo.getAccessToken(), PlanckMailSharePreferences.SHARE_PREFERENCES_TYPE.STRING);

        finish();
    }

    public void prepareSendEmail(AccountInfo accountInfo) {
        SendMessage sendMessage = buildMessage();
        String messageJson = JsonUtilFactory.getJsonUtil().toJson(sendMessage);

        final TypedInput in = new TypedJsonString(messageJson);

        Snackbar snackbar = Snackbar.make(mLayout, getString(R.string.sendEmail), Snackbar.LENGTH_SHORT);
        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.lightGreen));
        snackbar.show();

        sendEmail(accountInfo, in);
    }

    private void uploadFile(final ImageView ivFile, final ProgressBar progressBar, final java.io.File file, final TextView tvFileSize) {
        int position = mSpinnerEmail.getSelectedItemPosition();
        AccountInfo accountInfo = mAccountInfoList.get(position);

        MultipartTypedOutput multipartTypedOutput = new MultipartTypedOutput();
        multipartTypedOutput.addPart("image[]", new TypedFile("image/jpg", file));

        progressBar.setVisibility(View.VISIBLE);
        ivFile.setVisibility(View.GONE);

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
        nylasServer.uploadFile(multipartTypedOutput, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String json = (String) o;
                ArrayList<File> list = JsonUtilFactory.getJsonUtil().fromJsonArray(json, File.class);

                mArrayFileResponse.addAll(list);
                progressBar.setVisibility(View.GONE);

                if (file.exists()) {
                    setFileImage(file, ivFile);
                    setFileSize(file, tvFileSize);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                progressBar.setVisibility(View.GONE);
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.close_x);
                ivFile.setImageBitmap(icon);
                ivFile.setVisibility(View.VISIBLE);
                tvFileSize.setTextColor(Color.RED);
                tvFileSize.setText(R.string.imageFailed);
                Log.d(PlanckMailApplication.TAG, "error load file " + error.getResponse());
            }
        });

    }

    public void setFileImage(java.io.File file, ImageView ivFile) {
        Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
        Bitmap newBitmap = Bitmap.createScaledBitmap(image, 120, 120, false);
        ivFile.setVisibility(View.VISIBLE);
        ivFile.setImageBitmap(newBitmap);
    }

    public void setFileSize(java.io.File file, TextView tvFileSize) {
        int size = (int) (file.length() / SIZE_DIVIDER);
        if (size >= SIZE_DIVIDER) {
            size = (int) ((file.length() / SIZE_DIVIDER) / SIZE_DIVIDER);
            tvFileSize.setText(String.valueOf("(" + size + " " + MB + ")"));
        } else
            tvFileSize.setText(String.valueOf("(" + size + " " + KB + ")"));
    }

    public void sendEmail(final AccountInfo accountInfo, TypedInput in) {
        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
        nylasServer.sendMessage(in, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String json = (String) o;

                Message message = JsonUtilFactory.getJsonUtil().fromJson(json, Message.class);
                if (mIsNotifyClicked) {
                    notifyMe(message, mMillisFoNotifyMe, accountInfo);
                }

                if(mTrackingMe && mTrackId!=null) {
                    updateTrack(mTrackId, message);
                }
                Intent intent = new Intent();
                intent.putExtra(BundleKeys.TOAST_TEXT, getResources().getString(R.string.messageSent));
                setResult(Activity.RESULT_OK, intent);
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null) {
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
                Snackbar.make(mLayout, getString(R.string.sendEmailError), Snackbar.LENGTH_SHORT).show();
            }
        });
    }



    private void notifyMe(final Message message, final long millis, final AccountInfo accountInfo) {
        RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
        PlanckService service = client.getPlankService();
        service.add_thread_to_notify(accountInfo.email, message.thread_id, message.id, 1, message.subject, millis, new Callback<String>() {
            @Override
            public void success(String s, Response response) {
                Log.i(PlanckMailApplication.TAG, "response notify me " + s);
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

    private void getContacts() {
        int position = mSpinnerEmail.getSelectedItemPosition();
        AccountInfo accountInfo = mAccountInfoList.get(position);

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
        nylasServer.getAllContacts(LIMIT_LOAD_MAILS, new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        String json = (String) o;
                        Log.i(PlanckMailApplication.TAG, "url " + response.getUrl());
                        mMainListContacts = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Contact.class);
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

    private boolean checkEmailValid() {
        boolean isToValid;
        boolean isCcValid;
        boolean isBccValid;

        isToValid = isToValid();
        isCcValid = isCcValid(true);
        isBccValid = isBccValid(true);

        return isToValid && isCcValid && isBccValid;
    }

    private boolean isToValid() {
        boolean isToValid;
        isToValid = isValid(mToAutoComplete.getObjects());

        if (!isToValid) {
            showError(mToAutoComplete);
        }
        return isToValid;
    }

    private boolean isBccValid(boolean isBccValid) {
        if (!mBccAutoComplete.getObjects().isEmpty()) {
            isBccValid = isValid(mBccAutoComplete.getObjects());

            if (!isBccValid)
                showError(mBccAutoComplete);
        }
        return isBccValid;
    }

    private boolean isCcValid(boolean isCcValid) {
        if (!mCcAutoComplete.getObjects().isEmpty()) {
            isCcValid = isValid(mCcAutoComplete.getObjects());

            if (!isCcValid)
                showError(mCcAutoComplete);
        }
        return isCcValid;
    }

    private boolean isValid(List<Contact> list) {
        boolean isValid = true;

        for (Contact contact : list) {
            if (!UserHelper.isValidEmail(contact.getEmail()))
                isValid = false;
        }
        return isValid;
    }

    private void showError(MultiAutoCompleteTextView view) {
        view.setError(getResources().getString(R.string.notEmailValid));
    }

    private SendMessage buildMessage() {
        SendMessage sendMessage = new SendMessage();
        doAction(sendMessage);

        for (File file : mArrayFileResponse)
            sendMessage.file_ids.add(file.id);

        for (String fileId : mArrayFileIds)
            sendMessage.file_ids.add(fileId);

        Tag tag = new Tag();
        tag.name = Folders.SENT.toString();

        sendMessage.setTo(mToAutoComplete.getObjects());
        sendMessage.setCc(mCcAutoComplete.getObjects());
        sendMessage.setBcc(mBccAutoComplete.getObjects());
        sendMessage.setSubject(mEtSubject.getText().toString());

        mBodyMessage = mEtComposeMessage.getText().toString();

        if(mTrackingMe && mTrackId!=null) {
            String openUrl = RestPlankMail.BASE_URL4 + "/read?log=true&track_id=" + mTrackId;
            mBodyMessage += "<img src=\""+openUrl+"\" style=\"visibility: hidden;\" width=\"1\" height=\"1\" border=\"0\" />";

            String linkUrl = RestPlankMail.BASE_URL4 + "/link?log=true&track_id=" + mTrackId;
            mBodyMessage = mBodyMessage.replace(PlanckMailApplication.PLANCK_SERVER, linkUrl);
        }
        sendMessage.setBody(mBodyMessage);

        return sendMessage;
    }

    private void doAction(SendMessage sendMessage) {
        int type = getIntent().getIntExtra(BundleKeys.MESSAGE_TYPE, -1);
        switch (type) {
            case REPLY:
            case REPLY_ALL:
                sendMessage.setReply_to_message_id(mMessage.id);
                break;
            case FORWARD:
                if (!mMessage.files.isEmpty()) {
                    for (File file : mMessage.files)
                        sendMessage.file_ids.add(file.id);
                }
                break;
        }
    }

    private SendDraft createDraft() {
        SendDraft draft = new SendDraft();

        Tag tag = new Tag();
        tag.name = Folders.SENT.toString();
        draft.setTo(mToAutoComplete.getObjects());
        draft.setCc(mCcAutoComplete.getObjects());
        draft.setBcc(mBccAutoComplete.getObjects());
        draft.setSubject(mEtSubject.getText().toString());

        for (File file : mArrayFileResponse)
            draft.file_ids.add(file.id);

        for (String fileId : mArrayFileIds)
            draft.file_ids.add(fileId);

        String body = (mEtComposeMessage.getText().toString().replace("\n", "<br>"));
        draft.setBody(body);

        return draft;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        getContacts();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.acCc:
                tvCcCompose.setText(R.string.cc);
                mLlBccCompose.setVisibility(View.VISIBLE);
                break;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvFullMessage:
                mEtComposeMessage.setText(mEtComposeMessage.getText().toString() + "<br>" + "<br>" + "<br>"
                        + Html.fromHtml(mMessage.body) + "<br>" + "<br>" + "<br>" + getResources().getString(R.string.send_by));

                mTvFullMessage.setVisibility(View.GONE);
                break;
            case R.id.tvDeleteAvailability:
                mCardAvailability.setVisibility(View.GONE);
                mViewCreateAvailability.setVisibility(View.GONE);
                break;
            case R.id.tvNoneTime:
                int nextFrame = 7;
                int defaultDuration = 1800;

                mNextAvailabilityFrame = mNextAvailabilityFrame + nextFrame;

                MutableDateTime mutableTime = new MutableDateTime();
                mutableTime.addDays(mNextAvailabilityFrame);
                mNoneTime.setClickable(false);
                setAvailableTimeInterval(mutableTime.toDateTime(), defaultDuration);
                break;
            case R.id.cardViewElementAvailability:
                for (int i = 0; i < mLayoutSendAvailability.getChildCount(); i++) {
                    View view = mLayoutSendAvailability.getChildAt(i);

                    if (v.getId() != view.getId()) {
                        v.setTag(R.string.tag_selected_availability, false);
                        view.setBackgroundColor(Color.WHITE);
                    } else {
                        v.setBackgroundColor(getResources().getColor(R.color.gray_light));
                        v.setTag(R.string.tag_selected_availability, true);
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_EVENT_CREATED:
                    String eventType = getEvent(data);
                    setEventInformation(data, eventType);
                    break;
                case RESULT_FILE_FROM_ACCOUNT_ADD:
                    Bundle extras = data.getExtras();
                    AsyncSaveFile task = new AsyncSaveFile(extras);
                    task.execute();
                    break;
                case RESULT_CREATE_AVAILABILITY:
                    String subject = data.getStringExtra(BundleKeys.THREAD_SUBJECT);
                    String length = data.getStringExtra(BundleKeys.DURATION);
                    final String location = data.getStringExtra(BundleKeys.LOCATION);
                    int seconds = data.getIntExtra(BundleKeys.SECONDS_AVAILABILITY, 0);

                    if (!TextUtils.isEmpty(subject)) {
                        mEtSubject.setText(subject);
                    }
                    ArrayList<String> listEmail = data.getStringArrayListExtra(BundleKeys.EMAIL);

                    final StringBuilder emailBuilder = new StringBuilder();
                    for (int i = 0; i < listEmail.size(); i++) {
                        String email = listEmail.get(i);
                        emailBuilder.append(email);

                        if (i != listEmail.size() - 1)
                            emailBuilder.append("; ");

                        Contact contact = new Contact();
                        contact.setEmail(email);
                        mCcAutoComplete.addObject(contact);
                    }
                    getAvailabilityTime(length, seconds, location, emailBuilder);

                    break;
                case RESULT_LOAD_IMAGE:
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    if (cursor != null) {
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);

                        String picturePath = cursor.getString(columnIndex);
                        cursor.close();

                        Bundle bundle = new Bundle();
                        bundle.putBoolean(BundleKeys.PICTURE_CAMERA, false);
                        bundle.putString(BundleKeys.PICTURE_PATH, picturePath);

                        AsyncSaveFile task1 = new AsyncSaveFile(bundle);
                        task1.execute();
                    }
                    break;
                case RESULT_LOAD_IMAGE_CAMERA:
                    Bundle bundle = data.getExtras();
                    bundle.putBoolean(BundleKeys.PICTURE_CAMERA, true);
                    AsyncSaveFile task2 = new AsyncSaveFile(bundle);
                    task2.execute();
                    break;
            }
        }
    }

    private void getAvailabilityTime(String length, int time, final String location, final StringBuilder emailBuilder) {

        mTvLocation.setText(getString(R.string.locationForMeeting, location));
        mTvLength.setText(getString(R.string.length, length));
        mTvMeeting.setText(getString(R.string.meeting, emailBuilder.toString()));

        int position = mSpinnerEmail.getSelectedItemPosition();
        final AccountInfo accountInfo = mAccountInfoList.get(position);

        mDescription = new AvailabilityDescription();
        mDescription.setLocation(location);
        mDescription.setSender(accountInfo.email);
        mDescription.setParticipants(emailBuilder.toString());
        DateTime dateTime = new DateTime();
        setAvailableTimeInterval(dateTime, time);
    }

    private void setAvailableTimeInterval(final DateTime time, int length) {
        RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
        PlanckService service = client.getPlankService();

        int position = mSpinnerEmail.getSelectedItemPosition();
        final AccountInfo accountInfo = mAccountInfoList.get(position);

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yyyy");
        String dateTime = formatter.print(time);
        int timeFrame = 7;
        String timeZone = TimeZone.getDefault().getID();

        service.getAvailability(timeZone, accountInfo.email, dateTime, timeFrame, length, new Callback<String>() {
            @Override
            public void success(String s, Response response) {
                mNoneTime.setClickable(true);
                UserHelper.hideKeyboard(ComposeActivity.this, mLayout);

                mCardAvailability.setVisibility(View.VISIBLE);
                mViewCreateAvailability.setVisibility(View.VISIBLE);

                AvailabilityTime availabilityTime = JsonUtilFactory.getJsonUtil().fromJson(s, AvailabilityTime.class);
                List<List<Long>> intervalsList = availabilityTime.intervals;

                if (intervalsList != null) {
                    setAvailableTimeInterval(intervalsList);
                    mCardNoAvailableTime.setVisibility(View.GONE);
                } else
                    mCardNoAvailableTime.setVisibility(View.VISIBLE);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    @NonNull
    private String getEvent(Intent data) {
        int type = data.getIntExtra(BundleKeys.KEY_EVENT_TYPE, -1);
        String eventType;

        if (type == 0)
            eventType = getResources().getString(R.string.eventAvailable);
        else
            eventType = getResources().getString(R.string.eventInvitation);
        return eventType;
    }

    private void setEventInformation(Intent data, String eventType) {
        String subject = data.getStringExtra(BundleKeys.KEY_SUBJECT_CREATE_EVENT);
        String date = data.getStringExtra(BundleKeys.KEY_DATE_CREATE_EVENT);
        String time = data.getStringExtra(BundleKeys.KEY_TIME_CREATE_EVENT);
        String location = data.getStringExtra(BundleKeys.KEY_LOCATION_CREATE_EVENT) == null ? "" : data.getStringExtra(BundleKeys.KEY_LOCATION_CREATE_EVENT);
        String currentText = mEtComposeMessage.getText().toString();
        String when = getResources().getString(R.string.when);

        mEtSubject.setText(subject);

        mEtComposeMessage.setText(eventType + when + " " + time + "\n" + date + "\n" + location + "\n\n\n" + currentText);
    }

    @Override
    public void pickedDate(SwipeLayout swipeLayout, RecycleThreadAdapter.SWIPE_TYPE swipeType, long millis) {
    }

    @Override
    public void pickedDate(String threadId, long millis) {
        Bundle bundle = new Bundle();
        bundle.putLong(BundleKeys.TIME, millis);
        bundle.putBoolean(BundleKeys.IS_NOTIFY, true);

        SwipeConfirmationDialog fragment = new SwipeConfirmationDialog();
        fragment.setArguments(bundle);
        fragment.setListener(this);
        fragment.show(getFragmentManager(), fragment.getClass().getName());
    }

    @Override
    public void confirmSwipeActionClick(RecycleThreadAdapter.SWIPE_TYPE type, SwipeLayout swipe, Thread thread, long millis, int position, boolean stateRemind) {
        mIsNotifyClicked = true;
        mMillisFoNotifyMe = millis;
    }

    @Override
    public void confirmSwipeSnoozeClick(Thread thread, long millis) {

    }

    private class AsyncSaveFile extends AsyncTask<Void, Void, java.io.File> {
        private final Bundle mBundle;

        public AsyncSaveFile(Bundle bundle) {
            mBundle = bundle;
        }

        @Override
        protected java.io.File doInBackground(Void... voids) {
            java.io.File mFile;
            boolean state = mBundle.getBoolean(BundleKeys.PICTURE_CAMERA);
            if (state) {
                mFile = loadCameraImage(mBundle);
            } else {
                mFile = loadGalleryImage(mBundle);
            }

            return mFile;
        }

        @Override
        protected void onPostExecute(java.io.File file) {
            super.onPostExecute(file);
            mArrayFileUpload.add(file);
            showTakenFile(file);
        }
    }

    private java.io.File loadCameraImage(Bundle extras) {
        Bitmap imageBitmap = (Bitmap) extras.get("data");
        return saveBitmap(imageBitmap);
    }

    private java.io.File loadGalleryImage(Bundle extras) {
        String picturePath = extras.getString(BundleKeys.PICTURE_PATH, "");
        java.io.File file = new java.io.File(picturePath);
        Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());

        return saveBitmap(image);
    }

    private void showTakenFile(java.io.File file) {
        View viewShowFile = View.inflate(this, R.layout.elem_file_message, null);
        //   TextView tvFileSize = (TextView) viewShowFile.findViewById(R.id.tvFileSize);
        TextView tvFileName = (TextView) viewShowFile.findViewById(R.id.tvFileName);
        TextView tvFileRemove = (TextView) viewShowFile.findViewById(R.id.tvFileRemove);
        ImageView ivFile = (ImageView) viewShowFile.findViewById(R.id.ivFile);
        ProgressBar progressBar = (ProgressBar) viewShowFile.findViewById(R.id.prLoadImage);

        tvFileRemove.setTag(R.string.tagFileDelete, file);
        tvFileRemove.setTag(R.string.tagRemoveView, viewShowFile);
        tvFileRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View removeView = (View) v.getTag(R.string.tagRemoveView);
                java.io.File file = (java.io.File) v.getTag(R.string.tagFileDelete);

                mLlMailFile.removeView(removeView);
                mArrayFileUpload.remove(file);
            }
        });
        tvFileRemove.setVisibility(View.VISIBLE);

        mLlMailFile.addView(viewShowFile);

        uploadFile(ivFile, progressBar, file, tvFileName);
    }

    private java.io.File saveBitmap(Bitmap bmp) {
        DateTime time = new DateTime();
        String timePhotoTaken = time.toString("dd:hh:ss");

        java.io.File file = new java.io.File(Environment.getExternalStorageDirectory(), PlanckMailApplication.PLANK_MAIL_PHOTOS + java.io.File.separator + timePhotoTaken + IMAGE_FILE_FORMAT);

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 70, bos);
            byte[] bitmapData = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapData);

            fos.flush();
            fos.close();

        } catch (Exception e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return file;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        List<Contact> filteredList = new ArrayList<>();
        String modifyString = s.toString().replaceAll("[,\\s]", "");

        if (modifyString.length() > 0) {
            for (Contact contact : mMainListContacts) {
                int contactLimit = 5;
                if (contact.getEmail() != null && contact.getEmail().contains(modifyString) && filteredList.size() < contactLimit)
                    filteredList.add(contact);
            }
        }
        mAdapterAutoCompiler.setListData(filteredList);
        mAdapterAutoCompiler.getFilter().filter(modifyString);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
