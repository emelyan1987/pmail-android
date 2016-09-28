package com.planckmail.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alamkanak.weekview.DateTimeInterpreter;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.google.ical.compat.jodatime.LocalDateIterable;
import com.google.ical.compat.jodatime.LocalDateIteratorFactory;
import com.planckmail.R;
import com.planckmail.activities.CreateUpdateEventActivity;
import com.planckmail.activities.EventDetailsActivity;
import com.planckmail.activities.MenuActivity;
import com.planckmail.adapters.CalendarAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.dialogs.DialogSelectCalendar.ISelectCalendar;
import com.planckmail.helper.SpacesItemDecoration;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.EventDecorator;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Event;
import com.planckmail.web.response.nylas.wrapper.When;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.service.NylasService;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateChangedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.DurationFieldType;
import org.joda.time.LocalDate;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.fabric.sdk.android.services.concurrency.AsyncTask;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Taras Matolinets on 05.06.15.
 */
public class CalendarFragment extends BaseFragment implements BaseFragment.OnBackPressed, WeekView.EventClickListener, WeekView.MonthChangeListener,
        OnDateChangedListener, OnMonthChangedListener, View.OnClickListener, WeekView.ScrollListener, ISelectCalendar {
    public static final String CALENDAR_CACHING_ACTION = "com.plancklabs.local.caching.calendar";
    public static final String MESSAGE_ACTION = "com.plancklabs.message.action";
    public static final int LIMIT_EVENTS = 500;
    private static final int REQUEST_CODE = 444;
    private static final long TIME_STAMP = 1000L;
    private boolean mLoadCachedData = false;

    private WeekView mWeekView;
    private CalendarAdapter mAdapter;
    private MaterialCalendarView mCalendarView;
    private RecyclerView mRecycleView;
    private FloatingActionButton mActionButton;
    private TextView mTextMonth;
    private LinearLayoutManager mLayoutManager;
    private List<AccountInfo> mAccountInfoList = new ArrayList<>();
    private int mCounterEvents;
    private List<Event> mListAllEvents = new ArrayList<>();
    private ProgressBar mProgressBar;
    private TextView mTvNoInternetConnection;
    private CoordinatorLayout mLayout;
    private ArrayList<DateTime> mDatesList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        initViews(view);
        setListener();
        registerReceiver();
        setRecycleSettings();
        initActionBar();
        mAccountInfoList = UserHelper.getEmailAccountList(true);
        setDataAdapter();

        return view;
    }

    public void setRecycleSettings() {
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecycleView.setLayoutManager(mLayoutManager);
        mRecycleView.setHasFixedSize(true);
        mRecycleView.addItemDecoration(new SpacesItemDecoration(10));
    }

    public void registerReceiver() {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mCacheActionReceiver, new IntentFilter(CALENDAR_CACHING_ACTION));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageActionReceiver, new IntentFilter(MESSAGE_ACTION));
    }

    public void setDataAdapter() {
        String dateList = getArguments().getString(BundleKeys.CACHED_DATA);
        String eventList = getArguments().getString(BundleKeys.CACHED_LIST_EVENTS);

        AsyncLoadCacheData task = new AsyncLoadCacheData();
        task.execute(dateList, eventList);
    }

    @Override
    public void selectCalendar(String time, int position) {

    }

    private class AsyncLoadCacheData extends AsyncTask<String, Void, List<CalendarDay>> {

        @Override
        protected List<CalendarDay> doInBackground(String... strings) {
            String dateList = strings[0];
            String eventList = strings[1];

            if (!TextUtils.isEmpty(dateList)) {
                mDatesList = JsonUtilFactory.getJsonUtil().fromJsonArray(dateList, DateTime.class);
                mListAllEvents = JsonUtilFactory.getJsonUtil().fromJsonArray(eventList, Event.class);
            }
            convertDateEvent();
            createInterval();
            getAllCalendars();

            getAllEvents();

            return decorateCalendar();
        }

        @Override
        protected void onPostExecute(List<CalendarDay> list) {
            super.onPostExecute(list);
            mCalendarView.addDecorator(new EventDecorator(Color.WHITE, list));

            mLoadCachedData = true;
            if (!mListAllEvents.isEmpty()) {
                mProgressBar.setVisibility(View.GONE);
            } else {
                mRecycleView.setVisibility(View.GONE);
            }

            mAdapter = new CalendarAdapter(getActivity(), mDatesList, mListAllEvents);
            mRecycleView.setAdapter(mAdapter);

            scrollListToDate(new Date());

            mWeekView.goToDate(Calendar.getInstance());
            setupDateTimeInterpreter(false);
        }
    }

    private void getAllCalendars() {

    }

    private void createInterval() {
        DateTime currentTime = new DateTime();

        DateTime startDate = currentTime.withDayOfYear(1);
        DateTime endDate = startDate.plusYears(1);

        Days days = Days.daysBetween(startDate, endDate);

        DurationFieldType type = days.getFieldType();
        type.getName();

        mDatesList = new ArrayList<>();

        for (int i = 0; i < days.getDays(); i++) {
            DateTime d = startDate.withFieldAdded(DurationFieldType.days(), i);
            mDatesList.add(d);
        }
    }

    /**
     * receiver for save local data
     */
    private BroadcastReceiver mCacheActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mCounterEvents = 0;
            mProgressBar.setVisibility(View.GONE);
            mLoadCachedData = true;
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mCacheActionReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageActionReceiver);
    }

    private ArrayList<CalendarDay> decorateCalendar() {
        ArrayList<CalendarDay> listCalendarDay = new ArrayList<>();

        for (Event e : mListAllEvents) {
            DateTime time = new DateTime(e.when.start_time);

            CalendarDay calendarDay = new CalendarDay(time.toDate());
            listCalendarDay.add(calendarDay);
        }

        return listCalendarDay;
    }

    /**
     * Set up a date time interpreter which will show short date value when in week view and long
     * date value otherwise.
     *
     * @param shortDate True if the date value should be short.
     */

    private void setupDateTimeInterpreter(final boolean shortDate) {
        mWeekView.setDateTimeInterpreter(new DateTimeInterpreter() {
            @Override
            public String interpretDate(Calendar date) {
                SimpleDateFormat weekdayNameFormat = new SimpleDateFormat("EEE", Locale.getDefault());
                String weekday = weekdayNameFormat.format(date.getTime());
                SimpleDateFormat format = new SimpleDateFormat(" M/d", Locale.getDefault());

                // All android api level do not have a standard way of getting the first letter of
                // the week day name. Hence we get the first char programmatically.
                // Details: http://stackoverflow.com/questions/16959502/get-one-letter-abbreviation-of-week-day-of-a-date-in-java#answer-16959657
                if (shortDate)
                    weekday = String.valueOf(weekday.charAt(0));
                return weekday.toUpperCase() + format.format(date.getTime());
            }

            @Override
            public String interpretTime(int hour) {
                return hour > 11 ? (hour - 12) + " PM" : (hour == 0 ? "12 AM" : hour + " AM");
            }
        });
    }

    /**
     * load all events from server
     */
    private void getAllEvents() {

        for (int i = 0; i < mAccountInfoList.size(); i++) {
            AccountInfo accountInfo = mAccountInfoList.get(i);

            NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
            nylasServer.getAllEvents(LIMIT_EVENTS, new Callback<Object>() {
                        @Override
                        public void success(Object o, Response response) {
                            mTvNoInternetConnection.setVisibility(View.GONE);
                            ++mCounterEvents;

                            Log.i(PlanckMailApplication.TAG, response.getUrl());

                            if (mLoadCachedData) {
                                mListAllEvents.clear();
                                mLoadCachedData = false;
                            }

                            addEventToList((String) o);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            showError(error);
                        }
                    }
            );
        }
    }

    public void showError(RetrofitError error) {
        if (error.getKind().equals(RetrofitError.Kind.NETWORK)) {
            mTvNoInternetConnection.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        }

        Response r = error.getResponse();
        if (r != null)
            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
    }

    public void addEventToList(String json) {
        if (!TextUtils.isEmpty(json)) {
            AsyncLoadAllEvents task = new AsyncLoadAllEvents();
            task.execute(json);
        }
    }

    private class AsyncLoadAllEvents extends AsyncTask<String, Void, List<Event>> {
        @Override
        protected List<Event> doInBackground(String... strings) {
            String json = strings[0];
            return JsonUtilFactory.getJsonUtil().fromJsonArray(json, Event.class);
        }

        @Override
        protected void onPostExecute(List<Event> list) {
            super.onPostExecute(list);
            mListAllEvents.addAll(list);

            //update adapter after last response
            if (mCounterEvents == mAccountInfoList.size()) {
                fillAdapter();
            }
        }
    }

    public void fillAdapter() {
        mProgressBar.setVisibility(View.GONE);
        mRecycleView.setVisibility(View.VISIBLE);

        getRecurrenceEvents();
        convertDateEvent();

        createInterval();
        decorateCalendar();

        scrollListToDate(new Date());
        sendCalendarData();

        mAdapter.setListDates(mDatesList);
        mAdapter.setListEvents(mListAllEvents);
        mAdapter.notifyDataSetChanged();
    }

    private void sendCalendarData() {
        String dateJson = JsonUtilFactory.getJsonUtil().toJson(mDatesList);
        String listAllEventsJson = JsonUtilFactory.getJsonUtil().toJson(mListAllEvents);

        Intent intent = new Intent(MenuActivity.CACHING_ACTION);
        intent.putExtra(BundleKeys.CACHED_DATA, dateJson);
        intent.putExtra(BundleKeys.CACHED_LIST_EVENTS, listAllEventsJson);
        intent.putExtra(BundleKeys.ENUM_MENU, MenuActivity.EnumMenuActivity.CALENDAR);

        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
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

    public void convertDateEvent() {
        List<Event> recurrenceEventList = new ArrayList<>();
        //set color in each event
        for (Event e : mListAllEvents) {
            //convert all dates to millis
            boolean isDateType = e.when.object.equals(Event.CALENDAR_DATE.DATE.toString()) || e.when.object.equals(Event.CALENDAR_DATE.DATE_SNAP.toString());
            boolean isDateNotNull = e.when.date != null;

            if (isDateType && isDateNotNull) {
                DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
                DateTime time = formatter.parseDateTime(e.when.getDate());
                e.when.setStart_time(time.toDate().getTime());
            } else if (e.when.object.equals(Event.CALENDAR_DATE.TIME_SNAP.toString()) && e.recurrence == null) {
                e.when.setStart_time(e.when.getStart_time() * TIME_STAMP);
                e.when.setEnd_time(e.when.getEnd_time() * TIME_STAMP);
            }
            AccountInfo accountInfo = UserHelper.getAccountInfo(mAccountInfoList, e.account_id);
            int color = accountInfo.getCalendarColor();
            e.setColorEvent(color);

            recurrenceEventList.add(e);
        }

        mListAllEvents = recurrenceEventList;
    }

    /**
     * generate recurring events
     */
    public void getRecurrenceEvents() {
        List<Event> recurrenceEventList = new ArrayList<>();
        List<String> mListEventsId = new ArrayList<>();

        for (Event event : mListAllEvents) {
            if (event.recurrence != null && !mListEventsId.contains(event.id)) {
                //set id for prevent duplication recurring events
                if (!mListEventsId.contains(event.id))
                    mListEventsId.add(event.id);

                for (int i = 0; i < event.recurrence.rrule.size(); i++) {
                    String rule = event.recurrence.rrule.get(i);
                    List<Event> innerRecurrenceList = generateInnerRecurrentEventList(event, rule);

                    if (i == event.recurrence.rrule.size() - 1 && !innerRecurrenceList.isEmpty())
                        recurrenceEventList.addAll(innerRecurrenceList);
                }
            }
        }

        if (!recurrenceEventList.isEmpty())
            mListAllEvents.addAll(recurrenceEventList);
    }

    private BroadcastReceiver mMessageActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mProgressBar.setVisibility(View.GONE);
            mCounterEvents = 0;
            mLoadCachedData = true;

            String text = intent.getStringExtra(BundleKeys.TOAST_TEXT);
            Snackbar.make(mLayout, text, Snackbar.LENGTH_SHORT).show();
        }
    };


    private List<Event> generateInnerRecurrentEventList(Event event, String rule) {
        LocalDate start;
        //set time to millis with recurring rules
        if (event.when.object.equals(Event.CALENDAR_DATE.DATE.toString()) || event.when.object.equals(Event.CALENDAR_DATE.DATE_SNAP.toString())) {
            start = changeLocalDate(event);
        } else
            start = new LocalDate(TIME_STAMP * event.getWhen().start_time);
        return getInnerRecurrenceEvents(event, rule, start);
    }

    @NonNull
    private List<Event> getInnerRecurrenceEvents(Event event, String rule, LocalDate start) {
        DateTime innerTime = new DateTime();

        List<Event> innerRecurrenceList = new ArrayList<>();
        try {
            LocalDateIterable range = LocalDateIteratorFactory.createLocalDateIterable(rule, start, true);

            for (LocalDate localDate : range) {
                if (localDate.getYear() == innerTime.getYear()) {
                    Event recurrentEvent = createRecurrenceEvent(event, new DateTime(localDate.toDate()));
                    innerRecurrenceList.add(recurrentEvent);
                } else
                    break;
            }
        } catch (ParseException exception) {
            Log.e(PlanckMailApplication.TAG, exception.toString());
        }
        return innerRecurrenceList;
    }

    @NonNull
    private LocalDate changeLocalDate(Event event) {
        LocalDate start;
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        DateTime time = formatter.parseDateTime(event.when.getDate());
        event.when.setStart_time(time.toDate().getTime());
        start = new LocalDate(event.getWhen().start_time);
        return start;
    }

    /**
     * here we create new recurring event and all to all event list collection
     */
    private Event createRecurrenceEvent(Event event, DateTime localDate) {
        Event recurrenceEvent = new Event();

        recurrenceEvent.setId(event.id);
        recurrenceEvent.setObject(event.object);
        recurrenceEvent.setCalendar_id(event.calendar_id);
        recurrenceEvent.setAccount_id(event.account_id);
        recurrenceEvent.setMessage_id(event.message_id);
        recurrenceEvent.setDescription(event.description);
        recurrenceEvent.setLocation(event.location);
        recurrenceEvent.setParticipants(event.participants);
        recurrenceEvent.setRead_only(event.read_only);
        recurrenceEvent.setTitle(event.title);
        recurrenceEvent.setBusy(event.busy);
        recurrenceEvent.setStatus(event.status);
        recurrenceEvent.setRecurrence(event.recurrence);

        When when = getWhen(event, localDate);
        recurrenceEvent.setWhen(when);

        return recurrenceEvent;
    }

    @NonNull
    private When getWhen(Event event, DateTime localDate) {
        When when = new When();
        when.object = event.when.object;

        // set current hours and minutes for event
        DateTime startTime = new DateTime(TIME_STAMP * event.when.start_time);
        DateTime endTime = new DateTime(TIME_STAMP * event.when.end_time);

        int startTimeHour = startTime.getHourOfDay();
        int startTimeMinutes = startTime.getMinuteOfHour();

        int endTimeHour = endTime.getHourOfDay();
        int endTimeMinutes = endTime.getMinuteOfHour();

        MutableDateTime mutableTimeStartTime = localDate.toMutableDateTime();
        mutableTimeStartTime.setHourOfDay(startTimeHour);
        mutableTimeStartTime.setMinuteOfHour(startTimeMinutes);

        MutableDateTime mutableTimeEndTime = localDate.toMutableDateTime();
        mutableTimeEndTime.setHourOfDay(endTimeHour);
        mutableTimeEndTime.setMinuteOfHour(endTimeMinutes);

        DateTime mutableDateStartTime = mutableTimeStartTime.toDateTime();
        DateTime mutableDateEndTime = mutableTimeEndTime.toDateTime();

        when.setStart_time(mutableDateStartTime.toDate().getTime());
        when.setEnd_time(mutableDateEndTime.toDate().getTime());
        return when;
    }

    private void initActionBar() {
        Toolbar toolBar = ((MenuActivity) getActivity()).getToolbar();
        ActionBar actionBar = ((MenuActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setCustomView(R.layout.action_bar_calendar_view);
            actionBar.setHomeButtonEnabled(true);

            View view = actionBar.getCustomView();

            mTextMonth = (TextView) view.findViewById(R.id.tvShowMonth);
            mTextMonth.setOnClickListener(this);

            DateTime dateTime = new DateTime();
            mTextMonth.setText(dateTime.toString("MMM yyyy"));

            actionBar.setDisplayShowCustomEnabled(true);

            toolBar.setTitle("");
            toolBar.setSubtitle("");
        }
    }

    private void setListener() {
        Calendar calendar = Calendar.getInstance();

        mWeekView.setScrollListener(this);
        mWeekView.setOnEventClickListener(this);
        mWeekView.setMonthChangeListener(this);

        mActionButton.setOnClickListener(this);
        mCalendarView.setSelectedDate(calendar);
        mCalendarView.setOnDateChangedListener(this);
        mCalendarView.setOnMonthChangedListener(this);

        mRecycleView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int firstPosComp = mLayoutManager.findLastCompletelyVisibleItemPosition();

                if (newState == 0 && firstPosComp != -1) {
                    DateTime time = mDatesList.get(firstPosComp);
                    mCalendarView.setSelectedDate(time.toDate(), false);
                    mTextMonth.setText(time.toString("MMM yyyy"));
                } else if (newState == 1 && firstPosComp != -1) {
                    mCalendarView.setDayClicked(false);
                }
            }
        });
    }

    private void initViews(View view) {
        mWeekView = (WeekView) view.findViewById(R.id.weekViewAllCalendar);
        mCalendarView = (MaterialCalendarView) view.findViewById(R.id.calendarView);
        mRecycleView = (RecyclerView) view.findViewById(R.id.recycleCalendar);
        mProgressBar = (ProgressBar) view.findViewById(R.id.prLoading);
        mActionButton = (FloatingActionButton) view.findViewById(R.id.abCreateEvent);
        mTvNoInternetConnection = (TextView) view.findViewById(R.id.tvNoInternetConnection);
        mLayout = (CoordinatorLayout) view.findViewById(R.id.coordinateLayout);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main_calendar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionDay:
                mWeekView.setVisibility(View.VISIBLE);
                mRecycleView.setVisibility(View.GONE);
                break;
            case R.id.actionAgenda:
                mWeekView.setVisibility(View.GONE);
                mRecycleView.setVisibility(View.VISIBLE);
                break;
            case R.id.actionDefaultCalendar:
//                DialogSelectCalendar calendar = new DialogSelectCalendar();
//                calendar.setListener(this);
//                calendar.show(getFragmentManager(),calendar.getClass().toString());
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem settingItem = menu.getItem(3);
        //hide settings menu
        if (settingItem != null)
            settingItem.setVisible(false);
    }


    public void scrollListToDate(Date date) {
        for (int i = 0; i < mDatesList.size(); i++) {
            DateTime dataTime = mDatesList.get(i);
            DateTime dataTimeCurrent = new DateTime(date);

            boolean isDay = dataTime.getDayOfMonth() == dataTimeCurrent.getDayOfMonth();
            boolean isMonth = dataTime.getMonthOfYear() == dataTimeCurrent.getMonthOfYear();

            if (isDay && isMonth) {
                //scroll to selected date to top
                mCalendarView.setSelectedDate(date, true);
                mRecycleView.scrollToPosition(i);

                mTextMonth.setText(dataTimeCurrent.toString("MMM yyyy"));
                return;
            }
        }
    }

    @Override
    public void onBackPress() {
        ((MenuActivity) getActivity()).setMailIconEnable();
        getActivity().getFragmentManager().popBackStackImmediate();
    }

    @Override
    public void onEventClick(WeekViewEvent weekViewEvent, RectF rectF) {
        Event e = (Event) weekViewEvent.getEvent();

        String json = JsonUtilFactory.getJsonUtil().toJson(e);

        Bundle bundle = new Bundle();
        bundle.putString(BundleKeys.KEY_EVENT, json);

        Intent intent = new Intent(getActivity(), EventDetailsActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public List<WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        ArrayList<WeekViewEvent> eventsDayList = new ArrayList<>();

        for (int i = 0; i < mListAllEvents.size(); i++) {
            Event e = mListAllEvents.get(i);

            WeekViewEvent event = setAllDayEvent(i, e);
            event.setColor(e.getColorEvent());
            event.setEvent(e);

            eventsDayList.add(event);
        }
        return eventsDayList;
    }

    @NonNull
    public WeekViewEvent setAllDayEvent(int i, Event e) {
        Calendar startTime;
        Calendar endTime;
        //modify date event and set event for hole day
        if (e.when.object.equals(Event.CALENDAR_DATE.DATE.toString()) || e.when.object.equals(Event.CALENDAR_DATE.DATE_SNAP.toString())) {
            DateTime calendarEvent = new DateTime(e.when.getStart_time());
            MutableDateTime mutableTimeStartTime = calendarEvent.toMutableDateTime();
            mutableTimeStartTime.setHourOfDay(0);

            MutableDateTime mutableTimeEndTime = calendarEvent.toMutableDateTime();
            mutableTimeEndTime.setHourOfDay(23);

            DateTime mutableDateStartTime = mutableTimeStartTime.toDateTime();
            DateTime mutableDateEndTime = mutableTimeEndTime.toDateTime();

            startTime = mutableDateStartTime.toGregorianCalendar();
            endTime = mutableDateEndTime.toGregorianCalendar();
        } else {
            DateTime start = new DateTime(e.when.start_time);
            DateTime end = new DateTime(e.when.end_time);

            startTime = start.toGregorianCalendar();
            endTime = end.toGregorianCalendar();
        }

        return new WeekViewEvent(i, e.title, startTime, endTime);
    }

    @Override
    public void onDateChanged(MaterialCalendarView materialCalendarView, CalendarDay calendarDay) {
        Calendar calendar = calendarDay.getCalendar();
        mWeekView.goToDate(calendar);

        if (mCalendarView.getVisibility() == View.VISIBLE && mRecycleView.getVisibility() == View.VISIBLE)
            scrollListToDate(calendarDay.getDate());
    }

    @Override
    public void onMonthChanged(MaterialCalendarView materialCalendarView, CalendarDay calendarDay) {
        Calendar calendar = calendarDay.getCalendar();
        mWeekView.goToDate(calendar);

        if (mCalendarView.getVisibility() == View.VISIBLE && mRecycleView.getVisibility() == View.VISIBLE)
            scrollListToDate(calendarDay.getDate());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvShowMonth:
                if (mCalendarView.getVisibility() == View.GONE)
                    mCalendarView.setVisibility(View.VISIBLE);
                else {
                    mCalendarView.setVisibility(View.GONE);
                }
                break;
            case R.id.abCreateEvent:
                Intent intent = new Intent(getActivity(), CreateUpdateEventActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
                break;
        }
    }

    @Override
    public void onFirstVisibleDayChanged(Calendar calendar, Calendar calendar1) {
        mCalendarView.setSelectedDate(calendar.getTime(), false);
        mTextMonth.setText(new DateTime(calendar.getTime()).toString("MMM yyyy"));
    }
}