package com.planckmail.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.planckmail.R;
import com.planckmail.adapters.RecycleThreadAdapter;
import com.planckmail.utils.BundleKeys;

import org.joda.time.MutableDateTime;

/**
 * Created by Taras Matolinets on 26.09.15.
 */
public class SwipeDialog extends DialogFragment implements View.OnClickListener, DatePickerFragment.IPickedDate, TimePickerFragment.IPickedTime {

    private IPickedCustomDate mListener;
    private TextView mTvPickADate;
    private TextView mTvToday;
    private TextView mTvThisEvening;
    private TextView mTvTomorrow;
    private TextView mTvNextWeek;
    private TextView mTvInAMonth;
    private SwipeLayout mSwipeLayout;
    private TextView mTvThisWeekend;
    private TextView mTvSomeDay;

    public void setListener(IPickedCustomDate listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.fragment_schedule, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);

        initViews(view);
        setListeners();
        return builder.create();
    }

    private void setListeners() {
        mTvToday.setOnClickListener(this);
        mTvThisEvening.setOnClickListener(this);
        mTvTomorrow.setOnClickListener(this);
        mTvNextWeek.setOnClickListener(this);
        mTvInAMonth.setOnClickListener(this);
        mTvThisWeekend.setOnClickListener(this);
        mTvPickADate.setOnClickListener(this);
        mTvSomeDay.setOnClickListener(this);
    }

    private void initViews(View view) {
        mTvToday = (TextView) view.findViewById(R.id.tvLaterToday);
        mTvThisEvening = (TextView) view.findViewById(R.id.tvThisEvening);
        mTvTomorrow = (TextView) view.findViewById(R.id.tvTomorrow);
        mTvThisWeekend = (TextView) view.findViewById(R.id.tvThisWeekend);
        mTvNextWeek = (TextView) view.findViewById(R.id.tvNextWeek);
        mTvInAMonth = (TextView) view.findViewById(R.id.tvInMonth);
        mTvPickADate = (TextView) view.findViewById(R.id.tvPickADate);
        mTvSomeDay = (TextView) view.findViewById(R.id.tvSomeDay);
    }

    @Override
    public void onClick(View v) {
        long time = 0;
        int minuteOfHour = 0;
        int hourOfDay;
        int dayOfWeek;
        int weekOfWeekYear;
        int dayOfYear;
        MutableDateTime mutableToday = new MutableDateTime();

        switch (v.getId()) {
            case R.id.tvLaterToday:
                hourOfDay = 3;
                mutableToday.addHours(hourOfDay);
                time = mutableToday.getMillis();
                sendResults(time);
                break;
            case R.id.tvThisEvening:
                hourOfDay = 18;

                mutableToday.setHourOfDay(hourOfDay);
                mutableToday.setMinuteOfHour(minuteOfHour);
                time = mutableToday.getMillis();
                sendResults(time);
                break;
            case R.id.tvTomorrow:
                hourOfDay = 9;
                dayOfWeek = 1;

                mutableToday.setDayOfWeek(mutableToday.getDayOfWeek() + dayOfWeek);
                mutableToday.setHourOfDay(hourOfDay);
                mutableToday.setMinuteOfHour(minuteOfHour);
                time = mutableToday.getMillis();
                sendResults(time);
                break;

            case R.id.tvThisWeekend:
                hourOfDay = 9;
                dayOfWeek = 6;
                mutableToday.setDayOfWeek(dayOfWeek);
                mutableToday.setHourOfDay(hourOfDay);
                mutableToday.setMinuteOfHour(minuteOfHour);
                time = mutableToday.getMillis();
                sendResults(time);
                break;
            case R.id.tvNextWeek:
                hourOfDay = 9;
                dayOfWeek = 1;
                weekOfWeekYear = 1;
                mutableToday.setWeekOfWeekyear(mutableToday.getWeekOfWeekyear() + weekOfWeekYear);
                mutableToday.setDayOfWeek(dayOfWeek);
                mutableToday.addHours(hourOfDay);
                mutableToday.setMinuteOfHour(minuteOfHour);
                time = mutableToday.getMillis();
                sendResults(time);
                break;
            case R.id.tvInMonth:
                hourOfDay = 9;
                dayOfYear = 30;
                mutableToday.setHourOfDay(hourOfDay);
                mutableToday.setMinuteOfHour(minuteOfHour);
                mutableToday.addDays( dayOfYear);
                time = mutableToday.getMillis();
                sendResults(time);
                break;
            case R.id.tvSomeDay:
                hourOfDay = 9;
                dayOfYear = 90;
                mutableToday.setHourOfDay(hourOfDay);
                mutableToday.addDays(dayOfYear);
                time = mutableToday.getMillis();
                sendResults(time);
                break;

            case R.id.tvPickADate:
                DatePickerFragment dateFragment = new DatePickerFragment();
                dateFragment.setListener(this);
                dateFragment.show(getFragmentManager(), dateFragment.getClass().toString());
                break;
        }
    }

    private void sendResults(long time) {
        boolean isThread = getArguments().getBoolean(BundleKeys.IS_THREAD);

        RecycleThreadAdapter.SWIPE_TYPE type = (RecycleThreadAdapter.SWIPE_TYPE) getArguments().getSerializable(BundleKeys.SWIPE_TYPE);

        if (mListener != null && isThread) {
            mListener.pickedDate(mSwipeLayout, type, time);
            dismiss();
        } else if (mListener != null) {
            String threadId = getArguments().getString(BundleKeys.ID);
            mListener.pickedDate(threadId, time);
            dismiss();
        }
    }

    public void setSwipeLayout(SwipeLayout swipeLayout) {
        mSwipeLayout = swipeLayout;
    }

    @Override
    public void pickedDate(int year, int month, int day) {
        Bundle bundle = new Bundle();

        bundle.putInt(BundleKeys.YEAR, year);
        bundle.putInt(BundleKeys.MONTH, month);
        bundle.putInt(BundleKeys.DAY, day);

        TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setListener(this);
        timePickerFragment.setArguments(bundle);
        timePickerFragment.show(getFragmentManager(), timePickerFragment.getClass().toString());
    }

    @Override
    public void pickedTime(int year, int month, int day, int hourOfDay, int minute) {
        MutableDateTime mutableDateTime = new MutableDateTime();
        mutableDateTime.setYear(year);
        mutableDateTime.setMonthOfYear(month);
        mutableDateTime.setDayOfWeek(day);
        mutableDateTime.setHourOfDay(hourOfDay);
        mutableDateTime.setMinuteOfHour(minute);

        long time = mutableDateTime.getMillis();
        sendResults(time);
    }


    public interface IPickedCustomDate {
        void pickedDate(SwipeLayout swipeLayout, RecycleThreadAdapter.SWIPE_TYPE swipeType, long millis);

        void pickedDate(String threadId, long millis);
    }
}
