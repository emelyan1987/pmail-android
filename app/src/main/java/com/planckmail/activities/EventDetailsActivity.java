package com.planckmail.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.fragments.BaseFragment;
import com.planckmail.fragments.CalendarFragment;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.request.nylas.SendMessage;
import com.planckmail.web.response.nylas.Contact;
import com.planckmail.web.response.nylas.Event;
import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.TypedJsonString;
import com.planckmail.web.restClient.service.NylasService;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;

/**
 * Created by Taras Matolinets on 14.06.15.
 */
public class EventDetailsActivity extends BaseActivity implements View.OnClickListener, BaseFragment.OnBackPressed {

    private static final int REQUEST_CODE = 444;

    private ScrollView mScrollView;
    private TextView mTvTitle;
    private TextView mTvDate;
    private TextView mTvTime;
    private TextView mTvEventType;
    private TextView mTvOrganizerName;
    private TextView mTvEventDescription;
    private TextView mTvAlertTime;
    private TextView mUpdateEvent;
    private TextView mDeleteEvent;
    private TextView mStatusType;
    private LinearLayout mLlAttenderMain;
    private LinearLayout mLayoutUpdate;
    private ImageView mIvEventColor;
    private ImageView mIvOrganizerName;
    private Event mEvent;

    private List<AccountInfo> mAccountInfoList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_event_date);
        initViews();
        initListeners();
        mAccountInfoList = UserHelper.getEmailAccountList(true);
        setData();
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.eventDetailsToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mTvTitle = (TextView) findViewById(R.id.tvTitle);
        mTvDate = (TextView) findViewById(R.id.tvDate);
        mTvTime = (TextView) findViewById(R.id.tvTime);
        mTvEventType = (TextView) findViewById(R.id.tvEventLocation);
        mStatusType = (TextView) findViewById(R.id.tvStatusType);
        mUpdateEvent = (TextView) findViewById(R.id.tvUpdate);
        mDeleteEvent = (TextView) findViewById(R.id.tvDelete);
        mLlAttenderMain = (LinearLayout) findViewById(R.id.llAttendersMain);
        mTvOrganizerName = (TextView) findViewById(R.id.tvOrganizerName);
        mTvEventDescription = (TextView) findViewById(R.id.tvEventDescription);
        mTvAlertTime = (TextView) findViewById(R.id.tvAlertTime);
        mIvEventColor = (ImageView) findViewById(R.id.ivBackgroundEvent);
        mIvOrganizerName = (ImageView) findViewById(R.id.ivOrganize);
        mLayoutUpdate = (LinearLayout) findViewById(R.id.llUpdate);
        mScrollView = (ScrollView) findViewById(R.id.scrollEvent);
    }

    private void initListeners() {
        mTvAlertTime.setOnClickListener(this);
        mDeleteEvent.setOnClickListener(this);
        mUpdateEvent.setOnClickListener(this);
        mIvOrganizerName.setOnClickListener(this);
    }

    private void setData() {
        String json = getIntent().getStringExtra(BundleKeys.KEY_EVENT);

        mEvent = JsonUtilFactory.getJsonUtil().fromJson(json, Event.class);

        if (mEvent.read_only)
            mLayoutUpdate.setVisibility(View.GONE);

        mStatusType.setText(mEvent.getStatus());
        mTvTitle.setText(mEvent.title);

        if (mEvent.when.object.equals(Event.CALENDAR_DATE.DATE.toString()) || mEvent.when.object.equals(Event.CALENDAR_DATE.DATE_SNAP.toString())) {
            mTvTime.setText(R.string.allDayEvent);
        } else {
            String startTime = new DateTime(mEvent.when.start_time).toString("h:mm a");
            String endTime = new DateTime(mEvent.when.end_time).toString("h:mm a");

            mTvTime.setText(startTime + "-" + endTime);
        }

        String date = new DateTime(mEvent.when.start_time).toString("EE, MMMM, dd, yyyy");
        mTvDate.setText(date);

        showParticipants();

        if (!TextUtils.isEmpty(mEvent.description))
            mTvEventDescription.setText(mEvent.description);
        else
            mTvEventDescription.setVisibility(View.GONE);

        mIvEventColor.setBackgroundColor(mEvent.getColorEvent());

        if (!TextUtils.isEmpty(mEvent.location))
            mTvEventType.setText(mEvent.location);
        else
            mTvEventType.setVisibility(View.GONE);
        AccountInfo accountInfo = getAccount();

        if (accountInfo != null) {
            boolean isOwnerEnable = mEvent.owner != null && mEvent.owner.contains(accountInfo.getEmail());
            if (isOwnerEnable)
                mLayoutUpdate.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showParticipants() {
        if (mEvent.participants != null && !mEvent.participants.isEmpty()) {
            for (Participant p : mEvent.participants) {
                View view = View.inflate(this, R.layout.elem_attenders_event, null);
                TextView tvAttenderName = (TextView) view.findViewById(R.id.tvAttenderName);
                TextView tvAttenderEmails = (TextView) view.findViewById(R.id.tvAttenderEmail);
                TextView tvAttenderStatus = (TextView) view.findViewById(R.id.tvAttenderStatus);

                tvAttenderEmails.setText(p.email);
                tvAttenderStatus.setText(p.status);

                if (!TextUtils.isEmpty(p.name))
                    tvAttenderName.setText(p.name);
                else
                    tvAttenderName.setText(R.string.noName);

                mLlAttenderMain.addView(view);
            }
        } else
            mTvOrganizerName.setVisibility(View.GONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String text = data.getStringExtra(BundleKeys.TOAST_TEXT);
                Snackbar.make(mScrollView, text, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvUpdate:
                String eventJson = JsonUtilFactory.getJsonUtil().toJson(mEvent);
                Intent intent = new Intent(this, CreateUpdateEventActivity.class);
                intent.putExtra(BundleKeys.KEY_EVENT, eventJson);
                startActivityForResult(intent, REQUEST_CODE);
                break;
            case R.id.tvDelete:
                final AccountInfo accountInfo = getAccount();
                if (accountInfo != null) {
                    NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
                    nylasServer.deleteEvent(mEvent.id, new Callback<Object>() {
                        @Override
                        public void success(Object o, Response response) {

                            if (!mEvent.getParticipants().isEmpty())
                                sendEmail(accountInfo, R.string.meetingDeleted);

                            Intent intent = new Intent(CalendarFragment.MESSAGE_ACTION);
                            intent.putExtra(BundleKeys.TOAST_TEXT, getString(R.string.deleteEvent));
                            LocalBroadcastManager.getInstance(EventDetailsActivity.this).sendBroadcast(intent);

                            finish();
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Response r = error.getResponse();
                            if (r != null) {
                                TypedInput input = r.getBody();
                                try {
                                    String errorBody = UserHelper.fromStream(input.in());
                                    Toast.makeText(EventDetailsActivity.this, errorBody, Toast.LENGTH_SHORT).show();
                                    Log.e(PlanckMailApplication.TAG, "error: " + errorBody);
                                } catch (IOException e) {
                                    Log.e(PlanckMailApplication.TAG, "error: " + e);
                                }
                            }
                        }
                    });
                }
                break;
            case R.id.tvAlertTime:
                break;
            case R.id.ivOrganize:
                break;
        }
    }

    private void sendEmail(AccountInfo accountInfo, int text) {
        SendMessage sendMessage = buildMessage(text);

        String messageJson = JsonUtilFactory.getJsonUtil().toJson(sendMessage);

        TypedInput in = new TypedJsonString(messageJson);

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
        nylasServer.sendMessage(in, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.i(PlanckMailApplication.TAG, "response url " + response.getUrl());
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

    private SendMessage buildMessage(int text) {
        SendMessage sendMessage = new SendMessage();

        List<Contact> listContact = new ArrayList<>();

        for (Participant p : mEvent.getParticipants()) {
            Contact contact = new Contact();
            contact.setEmail(p.getEmail());
            contact.setName(p.getName());

            listContact.add(contact);
        }

        sendMessage.setTo(listContact);

        DateTime startDateTime = new DateTime(mEvent.when.getStart_time());
        String startTime = startDateTime.toString("EE, MMMM, dd, yyyy");

        DateTime startDate = new DateTime(mEvent.when.getEnd_time());
        String endTime = startDate.toString("EE, MMMM, dd, yyyy");

        String from = getResources().getString(R.string.from) + " " + startTime;
        String to = getResources().getString(R.string.to) + " " + endTime;
        String date = getResources().getString(R.string.date) + " " + mTvDate.getText();

        sendMessage.setSubject(getResources().getString(text) + " " + mEvent.getTitle() + " " + date + " " + from + " " + to);

        String notes = mEvent.getDescription();
        String location = getResources().getString(R.string.location) + " " + mEvent.getLocation();

        String body = location + "<br>" + notes;

        sendMessage.setBody(body);

        return sendMessage;
    }


    private AccountInfo getAccount() {
        // set account for getting contacts
        for (AccountInfo accountInfo : mAccountInfoList) {
            if (accountInfo.getAccountId().equalsIgnoreCase(mEvent.getAccount_id())) {
                return accountInfo;
            }
        }
        return null;
    }

    @Override
    public void onBackPress() {
        Intent intent = new Intent(CalendarFragment.CALENDAR_CACHING_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        finish();
    }

}
