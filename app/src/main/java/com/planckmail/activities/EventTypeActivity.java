package com.planckmail.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alamkanak.weekview.DateTimeInterpreter;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;
import com.planckmail.R;
import com.planckmail.utils.BundleKeys;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateChangedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Taras Matolinets on 04.06.15.
 */
public class EventTypeActivity extends BaseActivity implements
        OnDateChangedListener, OnMonthChangedListener, View.OnClickListener, WeekView.EventClickListener, WeekView.MonthChangeListener {

    private int mTypeEvent;

    private MaterialCalendarView mCalendarView;
    private TextView mTextMonth;
    private WeekView mWeekView;
    private LinearLayout mLayoutMain;
    private boolean isEnableCreateEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        initView();
        setListeners();

        setActionBar();

        Calendar calendar = Calendar.getInstance();
        mCalendarView.setSelectedDate(calendar);
        setData();
        setupDateTimeInterpreter(false);
    }

    private void setActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setCustomView(R.layout.action_bar_calendar_view);
            getSupportActionBar().setHomeButtonEnabled(true);

            View view = getSupportActionBar().getCustomView();

            mTextMonth = (TextView) view.findViewById(R.id.tvShowMonth);
            mTextMonth.setOnClickListener(this);

            DateTime dateTime = new DateTime();
            mTextMonth.setText(dateTime.toString("MMM yyyy"));

            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    private void setData() {
        Calendar calendar = Calendar.getInstance();
        mWeekView.goToDate(calendar);
        mTypeEvent = getIntent().getIntExtra(BundleKeys.KEY_EVENT_TYPE, -1);
    }

    private void setListeners() {
        mCalendarView.setOnDateChangedListener(this);
        mCalendarView.setOnMonthChangedListener(this);
        mWeekView.setOnEventClickListener(this);
        mWeekView.setMonthChangeListener(this);
    }

    private void initView() {
        mLayoutMain = (LinearLayout) findViewById(R.id.llMain);
        mWeekView = (WeekView) findViewById(R.id.weekViewAllActivity);
        mCalendarView = (MaterialCalendarView) findViewById(R.id.calendarViewEvent);
    }

    @Override
    public void onDateChanged(MaterialCalendarView materialCalendarView, CalendarDay calendarDay) {
        DateTime dateTime = new DateTime(calendarDay.getDate());
    }

    @Override
    public void onMonthChanged(MaterialCalendarView materialCalendarView, CalendarDay calendarDay) {
        DateTime dateTime = new DateTime(calendarDay.getDate());
        mTextMonth.setText(dateTime.toString("MMM yyyy"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_event_details, menu);

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
                if (isEnableCreateEvent) {

                    Intent intent = new Intent();

                    intent.putExtra(BundleKeys.KEY_EVENT_TYPE, mTypeEvent);
                    setResult(RESULT_OK, intent);
                    finish();
                }
                break;
            case R.id.actionCancel:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ComposeActivity.RESULT_EVENT_CREATED) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String text = data.getStringExtra(BundleKeys.TOAST_TEXT);
                Snackbar snackbar = Snackbar.make(mLayoutMain, text, Snackbar.LENGTH_SHORT);
                View snackBarView = snackbar.getView();
                snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.lightGreen));
                snackbar.show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvShowMonth:
                if (mCalendarView.getVisibility() == View.GONE)
                    mCalendarView.setVisibility(View.VISIBLE);
                else
                    mCalendarView.setVisibility(View.GONE);
                break;
        }
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


    @Override
    public void onEventClick(WeekViewEvent event, RectF eventRect) {
    }

    @Override
    public List<WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        ArrayList<WeekViewEvent> eventsDayList = new ArrayList<>();

//        for (int i = 0; i < mListAllEvents.size(); i++) {
//            Event e = mListAllEvents.get(i);
//
//            WeekViewEvent event = setAllDayEvent(i, e);
//            event.setColor(e.getColorEvent());
//            event.setEvent(e);
//
//            eventsDayList.add(event);
//        }
        return eventsDayList;
    }
}
