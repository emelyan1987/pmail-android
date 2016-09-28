package com.planckmail.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.BaseActivity;
import com.planckmail.activities.EventDetailsActivity;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Event;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taras Matolinets on 05.06.15.
 */
public class CalendarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {

    private final Context mContext;

    private  List<DateTime> mListDates;
    private  List<Event> mListEvents;

    public CalendarAdapter(Context context, List<DateTime> listDates, List<Event> listEvents) {
        mContext = context;
        mListDates = listDates;
        mListEvents = listEvents;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_calendar_list, parent, false);
        return new ViewHolderCalendar(view);
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        DateTime time = mListDates.get(position);
        String numberDate = String.valueOf(time.getDayOfMonth());
        ViewHolderCalendar viewHolder = (ViewHolderCalendar) holder;

        String day = time.toString("E");
        viewHolder.tvDay.setText(day);
        viewHolder.tvNumberDate.setText(numberDate);

        List<Event> listEvent = getEventList(time, ((ViewHolderCalendar) holder).llContent);

        if (!listEvent.isEmpty()) {
            ((ViewHolderCalendar) holder).llContent.setVisibility(View.VISIBLE);
            ((ViewHolderCalendar) holder).flNoEvents.setVisibility(View.GONE);
        } else {
            ((ViewHolderCalendar) holder).llContent.setVisibility(View.GONE);
            ((ViewHolderCalendar) holder).flNoEvents.setVisibility(View.VISIBLE);
            return;
        }

        for (Event e : listEvent) {
            createListEventView((ViewHolderCalendar) holder, e);
        }
    }

    public void createListEventView(ViewHolderCalendar holder, Event e) {
        View view = View.inflate(mContext, R.layout.elem_event, null);

        LinearLayout llEventContent = (LinearLayout)view.findViewById(R.id.llEvent);
        TextView tvEventTitle = (TextView) view.findViewById(R.id.tvEventTitle);
        TextView tvEventTime = (TextView) view.findViewById(R.id.tvEventTime);
        TextView tvEventType = (TextView) view.findViewById(R.id.tvEventLocation);

        if (!e.when.object.equals(Event.CALENDAR_DATE.DATE.toString()) && !e.when.object.equals(Event.CALENDAR_DATE.DATE_SNAP.toString())) {
            String startTime = new DateTime(e.when.start_time).toString("h:mm a");
            String endTime = new DateTime(e.when.end_time).toString("h:mm a");
            tvEventTime.setText(startTime + " - " + endTime);
        } else
            tvEventTime.setVisibility(View.GONE);

        tvEventTitle.setText(e.title);

        if (e.location != null) {
            tvEventType.setText(e.location);
        } else
            tvEventType.setVisibility(View.GONE);

        llEventContent.setOnClickListener(this);
        llEventContent.setTag(e);

        int radius = 15;
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radius);
        shape.setColor(e.getColorEvent());

        llEventContent.setBackgroundDrawable(shape);

        holder.llContent.addView(view);
    }

    private List<Event> getEventList(DateTime dateTime, LinearLayout llContent) {
        List<Event> eventList = new ArrayList<>();

        for (Event e : mListEvents) {
            llContent.removeAllViews();
            e.setColorEvent(e.getColorEvent());

            DateTime calendarEvent = new DateTime(e.when.getStart_time());

            int dayDate = dateTime.getDayOfMonth();
            int monthDate = dateTime.getMonthOfYear();
            int yearDate = dateTime.getYear();

            int eventDay = calendarEvent.getDayOfMonth();
            int eventMonth = calendarEvent.getMonthOfYear();
            int eventYear = calendarEvent.getYear();

            if (dayDate == eventDay && monthDate == eventMonth && yearDate == eventYear) {
                eventList.add(e);
            }
        }
        return eventList;
    }


    @Override
    public int getItemCount() {
        return mListDates.size();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.llEvent:
                Event event = (Event) v.getTag();

                String json = JsonUtilFactory.getJsonUtil().toJson(event);

                Bundle bundle = new Bundle();
                bundle.putString(BundleKeys.KEY_EVENT, json);

                Intent intent = new Intent(mContext, EventDetailsActivity.class);
                intent.putExtras(bundle);
                mContext.startActivity(intent);
                break;
        }
    }

    public class ViewHolderCalendar extends RecyclerView.ViewHolder {
        public TextView tvNoEvents;
        public FrameLayout flNoEvents;
        public LinearLayout llContent;
        public TextView tvNumberDate;
        public TextView tvDay;

        public ViewHolderCalendar(View itemView) {
            super(itemView);
            flNoEvents = (FrameLayout) itemView.findViewById(R.id.flNoEvents);
            tvNoEvents = (TextView) itemView.findViewById(R.id.tvNoEvents);
            llContent = (LinearLayout) itemView.findViewById(R.id.llCalendarContent);
            tvNumberDate = (TextView) itemView.findViewById(R.id.tvNumberDay);
            tvDay = (TextView) itemView.findViewById(R.id.tvDay);
        }
    }

    public void setListEvents(List<Event> mListEvents) {
        this.mListEvents = mListEvents;
    }

    public void setListDates(List<DateTime> mListDates) {
        this.mListDates = mListDates;
    }

}
