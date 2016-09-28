package com.planckmail.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.ical.compat.jodatime.LocalDateIterable;
import com.google.ical.compat.jodatime.LocalDateIteratorFactory;
import com.planckmail.R;
import com.planckmail.adapters.CalendarAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.helper.SpacesItemDecoration;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Contact;
import com.planckmail.web.response.nylas.Event;
import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.response.nylas.wrapper.When;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.service.NylasService;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.DurationFieldType;
import org.joda.time.LocalDate;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Terry on 3/23/2016.
 */
public class EventsPeopleFragment extends BaseFragment {

    public static final int LIMIT_EVENTS = 500;
    private static final long TIME_STAMP = 1000L;
    private boolean mLoadCachedData = false;

    private CalendarAdapter mAdapter;
    private RecyclerView mRecycleView;
    private LinearLayoutManager mLayoutManager;
    private List<AccountInfo> mAccountInfoList = new ArrayList<>();
    private int mCounterEvents;
    private List<Event> mListAllEvents = new ArrayList<>();
    private ProgressBar mProgressBar;
    private TextView mTvNoInternetConnection;
    private ArrayList<DateTime> mDatesList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people_events, container, false);

        initViews(view);
        setListener();
        setRecycleSettings();
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

    public void setDataAdapter() {
        String dateList = getArguments().getString(BundleKeys.CACHED_DATA);
        String eventList = getArguments().getString(BundleKeys.CACHED_LIST_EVENTS);

        AsyncLoadData task = new AsyncLoadData();
        task.execute(dateList, eventList);
    }

    private class AsyncLoadData extends AsyncTask<String, Void, List<CalendarDay>> {

        @Override
        protected List<CalendarDay> doInBackground(String... strings) {
            convertDateEvent();
            createInterval();

            getAllEvents();

            return decorateCalendar();
        }

        @Override
        protected void onPostExecute(List<CalendarDay> list) {
            super.onPostExecute(list);
            mLoadCachedData = true;
            if (!mListAllEvents.isEmpty()) {
                mProgressBar.setVisibility(View.GONE);
            } else {
                mRecycleView.setVisibility(View.GONE);
            }

            mAdapter = new CalendarAdapter(getActivity(), mDatesList, mListAllEvents);
            mRecycleView.setAdapter(mAdapter);

            scrollListToDate(new Date());
        }
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

        mAdapter.setListDates(mDatesList);
        mAdapter.setListEvents(mListAllEvents);
        mAdapter.notifyDataSetChanged();
    }

    public void convertDateEvent() {
        List<Event> recurrenceEventList = new ArrayList<>();
        String jsonContact = getArguments().getString(BundleKeys.CONTACT);

        Contact contact = JsonUtilFactory.getJsonUtil().fromJson(jsonContact, Contact.class);
        //set color in each event
        for (Event e : mListAllEvents) {
            //convert all dates to millis
            boolean isAddRecurrenceEvents = false;
            for (Participant participant : e.getParticipants()) {
                if (participant.email.equalsIgnoreCase(contact.getEmail()))
                    isAddRecurrenceEvents = true;
            }

            if (isAddRecurrenceEvents) {
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

    private void setListener() {
    }

    private void initViews(View view) {
        mRecycleView = (RecyclerView) view.findViewById(R.id.recyclePeopleEvents);
        mProgressBar = (ProgressBar) view.findViewById(R.id.prLoading);
        mTvNoInternetConnection = (TextView) view.findViewById(R.id.tvNoInternetConnection);
    }

    public void scrollListToDate(Date date) {
        for (int i = 0; i < mDatesList.size(); i++) {
            DateTime dataTime = mDatesList.get(i);
            DateTime dataTimeCurrent = new DateTime(date);

            boolean isDay = dataTime.getDayOfMonth() == dataTimeCurrent.getDayOfMonth();
            boolean isMonth = dataTime.getMonthOfYear() == dataTimeCurrent.getMonthOfYear();

            if (isDay && isMonth) {
                //scroll to selected date to top
                mRecycleView.scrollToPosition(i);
                return;
            }
        }
    }
}
