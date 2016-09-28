package com.planckmail.activities;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.planckmail.R;
import com.planckmail.adapters.AutoCompleteContactAdapter;
import com.planckmail.adapters.SpinnerAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.views.ContactsCompletionView;
import com.planckmail.web.request.nylas.SendMessage;
import com.planckmail.web.response.nylas.Contact;
import com.planckmail.web.response.nylas.Event;
import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.response.nylas.wrapper.Recurrence;
import com.planckmail.web.response.nylas.wrapper.When;
import com.planckmail.web.restClient.api.AuthNylasApi;

import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.TypedJsonString;
import com.planckmail.web.restClient.service.NylasService;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;


/**
 * Created by Taras Matolinets on 18.06.15.
 */
public class CreateUpdateEventActivity extends BaseActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener,
        AdapterView.OnItemSelectedListener, TextWatcher {

    public static final String DATE_PICKER_TAG = "datepicker";
    public static final String TIME_PICKER_TAG = "timepicker";
    public static final int LIMIT_LOAD_MAILS = 600;
    public static final int DAILY = 1;
    public static final int EVERY_WEEK_DAY = 2;
    public static final int EVERY_MN_WE_FR = 3;
    public static final int EVERY_TU_TH = 4;
    private static final int WEEKLY = 5;
    private static final int MONTHLY = 6;
    private boolean isToClicked;
    private boolean isEnableCreateEvent;

    private int mStatusPosition = -1;
    private int mRecurrencePosition = -1;
    private TextView mTvDate;
    private TextView mFromTime;
    private TextView mToTime;
    private EditText mEtSubject;
    private EditText mEtLocation;
    private ContactsCompletionView mEtInvitees;
    private EditText mEtNotes;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;
    private AutoCompleteContactAdapter mAdapter;
    private ProgressBar mProgressInvitees;
    private Spinner mSpinnerEmail;
    private Spinner mSpinnerCalendar;
    private Spinner mSpinnerRecurrence;
    private Spinner mSpinnerStatus;
    private LinearLayout mLayoutCalendar;
    private LinearLayout mLayoutEmail;
    private LinearLayout mLayoutRecurrence;
    private LinearLayout mllTime;
    private TextView mTo;
    private TextView mFrom;
    private Event mEvent;
    private ActionBar mActionBar;

    private List<AccountInfo> mAccountInfoList;
    public ArrayList<com.planckmail.web.response.nylas.Calendar> mListCalendars = new ArrayList<>();
    private ArrayList<Contact> mMainListContacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_event_activity);

        initActionBar();
        initViews();
        setListeners();
        mAccountInfoList = UserHelper.getEmailAccountList(true);
        setDate();
    }

    private void initViews() {
        mTvDate = (TextView) findViewById(R.id.tvDate);
        mToTime = (TextView) findViewById(R.id.tvToTime);
        mFromTime = (TextView) findViewById(R.id.tvFromTime);
        mTo = (TextView) findViewById(R.id.tvTo);
        mFrom = (TextView) findViewById(R.id.tvFrom);

        mEtSubject = (EditText) findViewById(R.id.etSubject);
        mEtLocation = (EditText) findViewById(R.id.etLocation);
        mEtInvitees = (ContactsCompletionView) findViewById(R.id.etInvitees);
        mEtNotes = (EditText) findViewById(R.id.etNotes);
        mProgressInvitees = (ProgressBar) findViewById(R.id.prInvitees);
        mSpinnerEmail = (Spinner) findViewById(R.id.spEmail);
        mSpinnerCalendar = (Spinner) findViewById(R.id.spCalendar);
        mSpinnerRecurrence = (Spinner) findViewById(R.id.spRecurrence);
        mSpinnerStatus = (Spinner) findViewById(R.id.spStatus);
        mLayoutCalendar = (LinearLayout) findViewById(R.id.llCalendar);
        mLayoutEmail = (LinearLayout) findViewById(R.id.llEmail);
        mLayoutRecurrence = (LinearLayout) findViewById(R.id.llRecurrence);
        mllTime = (LinearLayout) findViewById(R.id.llTime);
    }

    private void setListeners() {
        mFromTime.setOnClickListener(this);
        mToTime.setOnClickListener(this);
        mFrom.setOnClickListener(this);
        mTo.setOnClickListener(this);
        mTvDate.setOnClickListener(this);

        mEtInvitees.addTextChangedListener(new CustomTextWatcher(mEtInvitees));
        mEtSubject.addTextChangedListener(new CustomTextWatcher(mEtSubject));
        mEtLocation.addTextChangedListener(new CustomTextWatcher(mEtLocation));
        mEtNotes.addTextChangedListener(new CustomTextWatcher(mEtNotes));
        mEtInvitees.addTextChangedListener(this);

        mSpinnerRecurrence.setOnItemSelectedListener(this);
        mSpinnerStatus.setOnItemSelectedListener(this);
        mSpinnerEmail.setOnItemSelectedListener(this);
    }

    private void initActionBar() {
        if (getSupportActionBar() != null)
            mActionBar = getSupportActionBar();
    }

    private void setDate() {
        mAdapter = new AutoCompleteContactAdapter(this);
        mEtInvitees.setAdapter(mAdapter);

        String jsonEvent = getIntent().getStringExtra(BundleKeys.KEY_EVENT);
        String[] array = getResources().getStringArray(R.array.array_recurrent_rules);

        SpinnerAdapter adapter = new SpinnerAdapter(this, new ArrayList<>(Arrays.asList(array)), R.layout.elem_spinner);
        mSpinnerRecurrence.setAdapter(adapter);

        String[] statusArray = getResources().getStringArray(R.array.array_event_status);

        SpinnerAdapter adapterStatus = new SpinnerAdapter(this, new ArrayList<>(Arrays.asList(statusArray)), R.layout.elem_spinner);
        mSpinnerStatus.setAdapter(adapterStatus);

        if (jsonEvent != null) {
            mActionBar.setTitle(R.string.updateEventTitle);
            updateEvent(jsonEvent);
        } else {
            Calendar calendar = Calendar.getInstance();

            datePickerDialog = DatePickerDialog.newInstance(this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), true);
            timePickerDialog = TimePickerDialog.newInstance(this, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false, false);

            mActionBar.setTitle(R.string.createEventTitle);
            setDataInNewEvent();
        }
    }

    private void setDataInNewEvent() {
        ArrayList<String> nameAccountList = new ArrayList<>();
        for (AccountInfo a : mAccountInfoList)
            nameAccountList.add(a.getEmail());

        SpinnerAdapter adapterEmail = new SpinnerAdapter(this, nameAccountList, R.layout.elem_spinner);
        mSpinnerEmail.setAdapter(adapterEmail);

        DateTime dateTime = new DateTime();
        String date = dateTime.toString("EE, MMMM, dd, yyyy");
        mTvDate.setTag(dateTime);
        mTvDate.setText(date);
    }

    private void updateEvent(String jsonEvent) {
        mLayoutEmail.setVisibility(View.GONE);
        mLayoutCalendar.setVisibility(View.GONE);

        mEvent = JsonUtilFactory.getJsonUtil().fromJson(jsonEvent, Event.class);
        getContacts(false);

        setSpinnerStatusData();

        boolean isDateType = mEvent.when.object.equals(Event.CALENDAR_DATE.DATE.toString());
        boolean isDateSnapType = mEvent.when.object.equals(Event.CALENDAR_DATE.DATE_SNAP.toString());

        if (isDateType || isDateSnapType)
            mllTime.setVisibility(View.GONE);

        if (mEvent.location != null)
            mEtLocation.setText(mEvent.location);

        if (mEvent.description != null)
            mEtNotes.setText(mEvent.description);

        DateTime startTime = new DateTime(mEvent.getWhen().start_time);
        DateTime endTime = new DateTime(mEvent.getWhen().end_time);

        datePickerDialog = DatePickerDialog.newInstance(this, startTime.getYear(), startTime.getMonthOfYear() - 1, startTime.getDayOfMonth(), true);
        timePickerDialog = TimePickerDialog.newInstance(this, startTime.getHourOfDay(), startTime.getMinuteOfHour(), false, false);

        mFromTime.setTag(startTime);
        mToTime.setTag(endTime);

        mFromTime.setText(startTime.toString("hh:mm a"));
        mToTime.setText(endTime.toString("hh:mm a"));

        mTvDate.setTag(startTime);
        mTvDate.setText(startTime.toString("EE, MMMM, dd, yyyy"));


        for (Participant p : mEvent.getParticipants()) {
            Contact contact = new Contact();
            contact.setName(p.getName());
            contact.setEmail(p.getEmail());

            mEtInvitees.addObject(contact);
        }

        setRecurrence();
        mRecurrencePosition = mSpinnerRecurrence.getSelectedItemPosition();
        mStatusPosition = mSpinnerStatus.getSelectedItemPosition();
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

    private void setRecurrence() {
        if (mEvent.recurrence != null) {
            String rule = mEvent.recurrence.rrule.get(0);
            if (rule.equalsIgnoreCase(RecurrenceRule.DAILY.getString())) {
                mSpinnerRecurrence.setSelection(1);
            } else if (rule.equalsIgnoreCase(RecurrenceRule.EVERY_WEEK_DAY.getString())) {
                mSpinnerRecurrence.setSelection(2);
            } else if (rule.equalsIgnoreCase(RecurrenceRule.EVERY_MO_WE_FR.getString())) {
                mSpinnerRecurrence.setSelection(3);
            } else if (rule.equalsIgnoreCase(RecurrenceRule.EVERY_TU_TH.getString())) {
                mSpinnerRecurrence.setSelection(4);
            } else if (rule.startsWith(RecurrenceRule.WEEKLY.getString())) {
                mSpinnerRecurrence.setSelection(5);
            } else if (rule.equalsIgnoreCase(RecurrenceRule.MONTLY.getString())) {
                mSpinnerRecurrence.setSelection(6);
            }
        } else
            mLayoutRecurrence.setVisibility(View.GONE);
    }

    private void setSpinnerStatusData() {
        String[] statusArray = getResources().getStringArray(R.array.array_event_status);

        mEtSubject.setText(mEvent.title);

        for (int i = 0; i < statusArray.length; i++) {
            if (mEvent.status.equalsIgnoreCase(statusArray[i])) {
                mSpinnerStatus.setSelection(i);
            }
        }
    }

    private String getInvitees(List<Participant> list) {
        StringBuilder builder = new StringBuilder();

        for (Participant p : list) {
            builder.append(p.email);
            builder.append("\n");
        }
        return builder.toString();
    }

    /**
     * @param state true create event, false update event
     */
    private void getContacts(boolean state) {
        AccountInfo accountInfo;

        if (state) {
            int position = mSpinnerEmail.getSelectedItemPosition();
            accountInfo = mAccountInfoList.get(position);
        } else {
            accountInfo = getAccount();
        }

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

    public DataSetObserver observer = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            mProgressInvitees.setVisibility(View.GONE);
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mProgressInvitees.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mAdapter.registerDataSetObserver(observer);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvDate:
                datePickerDialog.show(getSupportFragmentManager(), DATE_PICKER_TAG);
                break;
            case R.id.tvToTime:
            case R.id.tvTo:
                if (v.getTag() != null) {
                    DateTime timeTo = (DateTime) v.getTag();
                    timePickerDialog.setStartTime(timeTo.getHourOfDay(), timeTo.getMinuteOfHour());
                }
                timePickerDialog.show(getSupportFragmentManager(), TIME_PICKER_TAG);
                isToClicked = true;
                break;
            case R.id.tvFromTime:
            case R.id.tvFrom:
                if (v.getTag() != null) {
                    DateTime timeFrom = (DateTime) v.getTag();
                    timePickerDialog.setStartTime(timeFrom.getHourOfDay(), timeFrom.getMinuteOfHour());
                }
                timePickerDialog.show(getSupportFragmentManager(), TIME_PICKER_TAG);
                isToClicked = false;
                break;
        }
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int month, int day) {
        MutableDateTime mutableTime = new MutableDateTime();
        mutableTime.setYear(year);
        //bug of library show previous month
        mutableTime.setMonthOfYear(month + 1);
        mutableTime.setDayOfMonth(day);

        String date = mutableTime.toString("EE, MMMM, dd, yyyy");

        if (mEvent != null) {
            DateTime d = new DateTime(mEvent.getWhen().start_time);
            String time = d.toString("EE, MMMM, dd, yyyy");
            checkUpdatedEvent(time, date);
        }

        mTvDate.setTag(mutableTime.toDateTime());
        mTvDate.setText(date);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_event, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem itemSave = menu.getItem(0);

        if (isEnableCreateEvent)
            itemSave.setIcon(R.drawable.ic_check_white_action_bar);
        else
            itemSave.setIcon(R.drawable.ic_check_grey_action_bar);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionDone:
                boolean checkEmail = checkEmailValid();

                if (mEvent == null) {
                    create(checkEmail);
                } else
                    update(checkEmail);

                break;
            case R.id.actionCancel:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void update(boolean checkEmail) {

        if (isEnableCreateEvent && checkEmail) {
            Event event = createViewEvent();

            String eventJson = JsonUtilFactory.getJsonUtil().toJson(event);
            final TypedInput in = new TypedJsonString(eventJson);

            final AccountInfo accountInfo = getAccount();

            if (accountInfo != null) {
                NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
                nylasServer.updateEvent(event.id, in, new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        Log.d(PlanckMailApplication.TAG, "response " + response.getUrl());
                        Intent intent = new Intent();
                        intent.putExtra(BundleKeys.TOAST_TEXT, getString(R.string.updateEvent));

                        sendEmail(accountInfo, R.string.meetingUpdated);

                        setResult(RESULT_OK, intent);

                        finish();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null) {
                            TypedInput input = r.getBody();
                            try {
                                String errorBody = UserHelper.fromStream(input.in());
                                Toast.makeText(CreateUpdateEventActivity.this, errorBody, Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Log.e(PlanckMailApplication.TAG, "error: " + e);
                            }
                        }
                    }
                });
            }
        }
    }

    public void create(boolean checkEmail) {
        if (isEnableCreateEvent && checkEmail) {
            final Event event = createViewEvent();

            String eventJson = JsonUtilFactory.getJsonUtil().toJson(event);
            final TypedInput in = new TypedJsonString(eventJson);

            int position = mSpinnerEmail.getSelectedItemPosition();
            final AccountInfo accountInfo = mAccountInfoList.get(position);

            NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
            nylasServer.addNewEvent(in, new Callback<Object>() {
                @Override
                public void success(Object o, Response response) {
                    Log.d(PlanckMailApplication.TAG, "response " + response.getUrl());

                    sendEmail(accountInfo, R.string.meetingCreated);

                    Intent intent = new Intent();
                    intent.putExtra(BundleKeys.TOAST_TEXT, getString(R.string.createEvent));

                    setResult(RESULT_OK, intent);
                    finish();
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null) {
                        TypedInput input = r.getBody();
                        try {
                            String errorBody = UserHelper.fromStream(input.in());
                            Toast.makeText(CreateUpdateEventActivity.this, errorBody, Toast.LENGTH_SHORT).show();
                            Log.e(PlanckMailApplication.TAG, "error: " + errorBody);
                        } catch (IOException e) {
                            Log.e(PlanckMailApplication.TAG, "error: " + e);
                        }
                    }
                }
            });
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

        sendMessage.setTo(mEtInvitees.getObjects());

        String from = getResources().getString(R.string.from) + " " + mFromTime.getText();
        String to = getResources().getString(R.string.to) + " " + mToTime.getText();
        String date = getResources().getString(R.string.date) + " " + mTvDate.getText();

        sendMessage.setSubject(getResources().getString(text) + " " + mEtSubject.getText().toString() + " " + date + " " + from + " " + to);

        String notes = mEtNotes.getText().toString().replace("\n", "<br>");
        String location = getResources().getString(R.string.location) + " " + mEtLocation.getText().toString();

        String body = location + "<br>" + notes;

        sendMessage.setBody(body);

        return sendMessage;
    }

    private Event createViewEvent() {
        String location = mEtLocation.getText().toString();
        String subject = mEtSubject.getText().toString();
        String notes = mEtNotes.getText().toString();

        DateTime fromTime = (DateTime) mFromTime.getTag();
        DateTime toTime = (DateTime) mToTime.getTag();

        Event event;

        if (mEvent == null) {
            event = new Event();
            setTime(event, fromTime, toTime);
            setCalendar(event);
        } else {
            event = mEvent;
            setWhen(event, fromTime, toTime);
        }

        if (!TextUtils.isEmpty(location))
            event.setLocation(location);

        if (!TextUtils.isEmpty(subject))
            event.setTitle(subject);

        if (!TextUtils.isEmpty(notes))
            event.setDescription(notes);

        setParticipants(event);
        setRecurrence(event, fromTime);

        String status = (String) mSpinnerStatus.getSelectedItem();
        event.setStatus(status);

        return event;
    }

    private void setCalendar(Event event) {
        int calendarPosition = mSpinnerCalendar.getSelectedItemPosition();
        com.planckmail.web.response.nylas.Calendar calendar = mListCalendars.get(calendarPosition);
        String calendarId = calendar.getId();
        event.setCalendar_id(calendarId);
    }

    private void setParticipants(Event event) {
        List<Participant> list = new ArrayList<>();

        for (Contact contact : mEtInvitees.getObjects()) {
            Participant participant = new Participant();
            participant.setEmail(contact.getEmail());
            list.add(participant);
        }
        event.setParticipants(list);
    }

    private void setWhen(Event event, DateTime fromTime, DateTime toTime) {
        boolean isDateType = event.when.object.equals(Event.CALENDAR_DATE.DATE.toString());
        boolean isDateSnapType = event.when.object.equals(Event.CALENDAR_DATE.DATE_SNAP.toString());

        if (isDateType || isDateSnapType) {
            DateTime time = (DateTime) mTvDate.getTag();
            event.when.date = time.toString("yyyy-MM-dd");
            event.when.start_time = 0;
            event.when.end_time = 0;
        } else {
            When when = new When();
            DateTime time = (DateTime) mTvDate.getTag();

            MutableDateTime mutableFromTime = time.toMutableDateTime();
            mutableFromTime.setHourOfDay(fromTime.getHourOfDay());
            mutableFromTime.setMinuteOfHour(fromTime.getMinuteOfHour());
            //covert to seconds requirement for sending params to server
            when.setStart_time(mutableFromTime.getMillis() / 1000);

            MutableDateTime mutableToTime = time.toMutableDateTime();
            mutableToTime.setHourOfDay(toTime.getHourOfDay());
            mutableToTime.setMinuteOfHour(toTime.getMinuteOfHour());
            //covert to seconds requirement for sending params to server
            when.setEnd_time(mutableToTime.getMillis() / 1000);

            event.setWhen(when);
        }
    }

    private void setRecurrence(Event event, DateTime fromTime) {
        int position = mSpinnerRecurrence.getSelectedItemPosition();

        if (position != 0) {
            String recurrenceRule = getRecurrenceRule(position, fromTime);

            Recurrence recurrence = new Recurrence();
            ArrayList<String> ruleList = new ArrayList<>();

            ruleList.add(recurrenceRule);

            recurrence.timezone = fromTime.getZone().getID();
            recurrence.rrule = ruleList;
            event.setRecurrence(recurrence);
        }
    }

    private void setTime(Event event, DateTime fromTime, DateTime toTime) {
        When when = new When();
        //covert to seconds requirement for sending params to server

        when.setStart_time(fromTime.getMillis() / 1000);
        when.setEnd_time(toTime.getMillis() / 1000);

        event.setWhen(when);
    }

    private String getRecurrenceRule(int position, DateTime fromTime) {
        String day = fromTime.toString("EE").toUpperCase();
        String rule = "";

        switch (position) {
            case DAILY:
                rule = RecurrenceRule.DAILY.toString();
                break;
            case EVERY_WEEK_DAY:
                rule = RecurrenceRule.EVERY_WEEK_DAY.toString();
                break;
            case EVERY_MN_WE_FR:
                rule = RecurrenceRule.EVERY_MO_WE_FR.toString();
                break;
            case EVERY_TU_TH:
                rule = RecurrenceRule.EVERY_TU_TH.toString();
                break;
            case WEEKLY:
                rule = RecurrenceRule.WEEKLY.toString() + day;
                break;
            case MONTHLY:
                rule = RecurrenceRule.MONTLY.toString();
                break;
        }
        return rule;
    }


    private boolean checkEmailValid() {
        //replace char \n because we write email
        boolean firstCheck;

        boolean secondCheck = true;

        String text = mEtInvitees.getText().toString().replaceAll("[,\\s]", "");
        if (!TextUtils.isEmpty(text))
            secondCheck = UserHelper.isValidEmail(text);

        firstCheck = isValid(mEtInvitees.getObjects());

        if (!firstCheck || !secondCheck) {
            showError(mEtInvitees);
        }

        return firstCheck && secondCheck;
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

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
        int period = view.getIsCurrentlyAmOrPm();
        String timePeriod = period == 0 ? "AM" : "PM";
        String timeZone = UserHelper.getCurrentTimeZoneOffset();
        String minutes;

        int i = 10;
        if (minute < i)
            minutes = "0" + String.valueOf(minute);
        else
            minutes = String.valueOf(minute);

        if (isToClicked) {
            mToTime.setText(String.valueOf(hourOfDay) + ":" + minutes + " " + timePeriod);

            DateTime dateTime = setTime(mToTime, hourOfDay, minute);
            mToTime.setTag(dateTime);

            if (mEvent != null) {
                String time = new DateTime(mEvent.getWhen().end_time).toString("hh:mm a");
                checkUpdatedEvent(time, mToTime.getText().toString());
            } else
                checkFillCorrectData();
        } else {
            mFromTime.setText(String.valueOf(hourOfDay) + ":" + minutes + " " + timePeriod);

            DateTime dateTime = setTime(mFromTime, hourOfDay, minute);
            mFromTime.setTag(dateTime);

            if (mEvent != null) {
                String time = new DateTime(mEvent.getWhen().start_time).toString("hh:mm a");
                checkUpdatedEvent(time, mFrom.getText().toString());
            } else
                checkFillCorrectData();
        }

        invalidateOptionsMenu();
    }

    /**
     * set time from each time view
     */
    private DateTime setTime(TextView view, int hourOfDay, int minute) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("H:m a");
        DateTime time = formatter.parseDateTime(view.getText().toString());

        DateTime dateTime = (DateTime) mTvDate.getTag();

        MutableDateTime mutableTime = time.toMutableDateTime();

        mutableTime.setYear(dateTime.getYear());
        mutableTime.setMonthOfYear(dateTime.getMonthOfYear());
        mutableTime.setDayOfMonth(dateTime.getDayOfMonth());
        mutableTime.setHourOfDay(hourOfDay);
        mutableTime.setMinuteOfHour(minute);

        return mutableTime.toDateTime();
    }

    private boolean checkIsValidCreatedEvent() {
        boolean textSubject = !TextUtils.isEmpty(mEtSubject.getText().toString());

        boolean from = !TextUtils.isEmpty(mFromTime.getText());
        boolean to = !TextUtils.isEmpty(mToTime.getText());
        boolean isCalendarAvailable = !mListCalendars.isEmpty();

        return textSubject && from && to && isCalendarAvailable;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spEmail:
                AccountInfo accountInfo = mAccountInfoList.get(position);
                getCalendars(accountInfo);
                getContacts(true);
                break;
            case R.id.spStatus:
                if (mStatusPosition != position) {
                    mStatusPosition = position;
                    isEnableCreateEvent = true;
                } else
                    isEnableCreateEvent = false;
                invalidateOptionsMenu();
                break;
            case R.id.spRecurrence:
                if (mRecurrencePosition != position) {
                    mRecurrencePosition = position;
                    isEnableCreateEvent = true;
                } else
                    isEnableCreateEvent = false;
                invalidateOptionsMenu();
                break;

        }
    }

    public void getCalendars(AccountInfo accountInfo) {
        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
        nylasServer.getAllCalendars(new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.d(PlanckMailApplication.TAG, "response " + response.getUrl());
                String json = (String) o;
                List<com.planckmail.web.response.nylas.Calendar> list = JsonUtilFactory.getJsonUtil().fromJsonArray(json, com.planckmail.web.response.nylas.Calendar.class);
                //clear data
                mListCalendars.clear();

                //show only calendars which we can modify
                for (com.planckmail.web.response.nylas.Calendar calendar : list) {
                    if (!calendar.read_only)
                        mListCalendars.add(calendar);
                }

                List<String> nameCalendarList = new ArrayList<>();

                for (com.planckmail.web.response.nylas.Calendar cal : mListCalendars) {
                    nameCalendarList.add(cal.name);
                }

                if (nameCalendarList.isEmpty())
                    nameCalendarList.add(getResources().getString(R.string.noCalendars));

                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(CreateUpdateEventActivity.this, android.R.layout.simple_spinner_item, nameCalendarList);
                spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                mSpinnerCalendar.setAdapter(spinnerArrayAdapter);

                checkFillCorrectData();
                invalidateOptionsMenu();
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

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
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

        mAdapter.setListData(filteredList);
        mAdapter.getFilter().filter(modifyString);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    public class CustomTextWatcher implements TextWatcher {
        private final EditText autoCompleteView;

        public CustomTextWatcher(EditText view) {
            autoCompleteView = view;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (!TextUtils.isEmpty(s)) {
                String text = s.toString();
                //when we select item from AutoCompleteTextView on textChange return object Contact. In our case we need just email which we select from list
                String mail = Contact.class.getCanonicalName();
                if (!text.startsWith(mail)) {
                    autoCompleteView.setTag(R.string.tag_composed_contacts, text);
                }
            }

            if (mEvent != null) {
                switch (autoCompleteView.getId()) {
                    case R.id.etInvitees:
                        //we add here extraObject variable for detect changes if new participant was added or not
                        int extraObject = 1;
                        String first = String.valueOf(mEvent.getParticipants().size() + extraObject);
                        String second = String.valueOf(mEtInvitees.getObjects().size());

                        checkUpdatedEvent(first, second);
                        break;
                    case R.id.etSubject:
                        checkUpdatedEvent(mEvent.getTitle(), mEtSubject.getText().toString());
                        break;
                    case R.id.etNotes:
                        String notes = TextUtils.isEmpty(mEvent.getLocation()) ? "" : mEvent.getDescription();
                        checkUpdatedEvent(notes, mEtNotes.getText().toString());
                        break;
                    case R.id.etLocation:
                        String location = TextUtils.isEmpty(mEvent.getLocation()) ? "" : mEvent.getLocation();
                        checkUpdatedEvent(location, mEtLocation.getText().toString());
                        break;
                }
            } else {
                checkFillCorrectData();
            }
            invalidateOptionsMenu();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }

    }

    private void checkUpdatedEvent(String first, String second) {
        if (!first.equalsIgnoreCase(second)) {
            isEnableCreateEvent = true;
        } else
            isEnableCreateEvent = false;

    }

    public void checkFillCorrectData() {
        if (checkIsValidCreatedEvent()) {
            isEnableCreateEvent = true;
        } else
            isEnableCreateEvent = false;
    }

    public enum RecurrenceRule {
        DAILY("RRULE:FREQ=DAILY"), EVERY_WEEK_DAY("RRULE:FREQ=WEEKLY;MO,TU,WE,TH,FR"),
        EVERY_MO_WE_FR("RRULE:FREQ=WEEKLY;MO,WE,FR"), EVERY_TU_TH("RRULE:FREQ=WEEKLY;TU,TH"),
        WEEKLY("RRULE:FREQ=WEEKLY;"), MONTLY("RRULE:FREQ=MONTHLY");

        private String rule;

        RecurrenceRule(String rule) {
            this.rule = rule;
        }

        public String getString() {
            return rule;
        }
    }
}
