package com.planckmail.helper;

import android.content.Context;
import android.support.annotation.NonNull;

import com.planckmail.R;
import com.planckmail.activities.MenuActivity;
import com.planckmail.adapters.SimpleSectionedRecyclerViewAdapter;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.enums.Folders;
import com.planckmail.fragments.ThreadFragment;
import com.planckmail.web.response.nylas.Thread;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Taras Matolinets on 24.09.15.
 */
public class MailFragmentHelper {

    public static final String GMAIL_APP = "gmail";

    public static ThreadFragment.ScheduleTime getSectionType(DateTime systemTime, DateTime threadTime) {
        ThreadFragment.ScheduleTime scheduleTime;
        boolean currentMonth = threadTime.getMonthOfYear() == systemTime.getMonthOfYear();
        boolean currentDayOfMonth = threadTime.getDayOfMonth() == systemTime.getDayOfMonth();
        boolean weekYear = threadTime.getWeekOfWeekyear() == systemTime.getWeekOfWeekyear();
        int week = threadTime.getWeekOfWeekyear();

        int timeEvening = 18;
        int nextMonth = 1;
        int nextWeek = 1;
        int nextDay = 1;
        int nexTwoWeeks = 2;
        int nexThreeWeeks = 3;

        if (threadTime.getHourOfDay() >= timeEvening && currentMonth) {
            scheduleTime = ThreadFragment.ScheduleTime.THIS_EVENING;
        } else if (currentMonth && currentDayOfMonth) {
            scheduleTime = ThreadFragment.ScheduleTime.TODAY;
        } else if (currentMonth && threadTime.getDayOfMonth() == systemTime.getDayOfMonth() + nextDay) {
            scheduleTime = ThreadFragment.ScheduleTime.TOMORROW;
        } else if (weekYear) {
            scheduleTime = ThreadFragment.ScheduleTime.THIS_WEEKEND;
        } else if (threadTime.getWeekOfWeekyear() == systemTime.getWeekOfWeekyear() + nextWeek) {
            scheduleTime = ThreadFragment.ScheduleTime.NEXT_WEEK;
        } else if (threadTime.getWeekOfWeekyear() == systemTime.getWeekOfWeekyear() + nexTwoWeeks) {
            scheduleTime = ThreadFragment.ScheduleTime.NEXT_TWO_WEEKS;
        } else if (threadTime.getWeekOfWeekyear() == systemTime.getWeekOfWeekyear() + nexThreeWeeks) {
            scheduleTime = ThreadFragment.ScheduleTime.NEXT_THREE_WEEKS;
        } else if (threadTime.getMonthOfYear() == systemTime.getMonthOfYear() + nextMonth) {
            scheduleTime = ThreadFragment.ScheduleTime.NEXT_MONTH;
        } else
            scheduleTime = ThreadFragment.ScheduleTime.ANOTHER_DATE;

        return scheduleTime;
    }

    public static String getSectionTitle(Context context, ThreadFragment.ScheduleTime scheduleTime) {
        String title;

        switch (scheduleTime) {
            case THIS_EVENING:
                title = context.getResources().getString(R.string.scheduleThisEvening);
                break;
            case TODAY:
                title = context.getResources().getString(R.string.scheduleLaterToday);
                break;
            case TOMORROW:
                title = context.getResources().getString(R.string.scheduleTomorrow);
                break;
            case THIS_WEEKEND:
                title = context.getResources().getString(R.string.scheduleThisWeekend);
                break;
            case NEXT_WEEK:
                title = context.getResources().getString(R.string.scheduleNextWeek);
                break;
            case NEXT_TWO_WEEKS:
                title = context.getResources().getString(R.string.scheduleNextTwoWeeks);
                break;
            case NEXT_THREE_WEEKS:
                title = context.getResources().getString(R.string.scheduleNextThreeWeeks);
                break;
            case NEXT_MONTH:
                title = context.getResources().getString(R.string.scheduleInMonth);
                break;
            default:
                title = context.getResources().getString(R.string.scheduleAnotherDate);
                break;
        }
        return title;
    }

    public static SimpleSectionedRecyclerViewAdapter.Section[] getSchedule(Context context, List<Thread> list) {
        int TIME_STAMP = 1000;

        Collections.sort(list, new CustomThreadComparator());

        List<SimpleSectionedRecyclerViewAdapter.Section> sections = new ArrayList<>();

        ArrayList<String> listTitles = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Thread thread = list.get(i);
            long convertedTime = thread.last_message_timestamp * TIME_STAMP;

            DateTime systemTime = new DateTime();
            DateTime threadTime = new DateTime(convertedTime);

            ThreadFragment.ScheduleTime type = MailFragmentHelper.getSectionType(systemTime, threadTime);

            if (!listTitles.contains(type.toString())) {
                listTitles.add(type.toString());
                addScheduleTimeSection(context, sections, i, type);
            }
        }
        SimpleSectionedRecyclerViewAdapter.Section[] arraySections = new SimpleSectionedRecyclerViewAdapter.Section[sections.size()];
        return sections.toArray(arraySections);
    }

    private static void addScheduleTimeSection(Context context, List<SimpleSectionedRecyclerViewAdapter.Section> sections, int i, ThreadFragment.ScheduleTime type) {
        String title = MailFragmentHelper.getSectionTitle(context, type);
        sections.add(new SimpleSectionedRecyclerViewAdapter.Section(i, title));
    }

    @NonNull
    public static HashMap<String, String> getMapTags(ThreadFragment.TARGET_FOLDER folder, int childId, AccountInfo accountInfo) {
        HashMap<String, String> mapParams = new HashMap<>();

        switch (childId) {
            case MenuActivity.INBOX:
                getInboxFolder(folder, mapParams, accountInfo);
                break;
            case MenuActivity.DRAFTS:
                mapParams.put(Folders.IN.toString(), Folders.DRAFTS.toString());
                break;
            case MenuActivity.TRASH:
                mapParams.put(Folders.IN.toString(), Folders.TRASH.toString());
                break;
            case MenuActivity.SENT_MAIL:
                mapParams.put(Folders.IN.toString(), Folders.SENT.toString());
                break;
            case MenuActivity.ALL_MAIL:
                mapParams.put(Folders.IN.toString(), Folders.ALL_MAIL.toString());
                break;
            case MenuActivity.STARRED:
                mapParams.put(Folders.IN.toString(), Folders.INBOX.toString());
                break;
            case MenuActivity.SPAM:
                mapParams.put(Folders.IN.toString(), Folders.SPAM.toString());
                break;
            case MenuActivity.FOLDERS:
                mapParams.put(Folders.IN.toString(), Folders.SPAM.toString());
                break;
        }
        return mapParams;
    }

    private static void getInboxFolder(ThreadFragment.TARGET_FOLDER folder, HashMap<String, String> mapParams, AccountInfo accountInfo) {
        switch (folder) {
            case READ_NOW:
                if (accountInfo.getEmail().contains(GMAIL_APP))
                    mapParams.put(Folders.IN.toString(), Folders.READ_NOW.toString());
                else
                    mapParams.put(Folders.IN.toString(), Folders.INBOX.toString());
                break;
            case READ_LATER:
                mapParams.put(Folders.IN.toString(), Folders.READ_LATER.toString());
                break;
            case FOLLOW_UP:
                mapParams.put(Folders.IN.toString(), Folders.FOLLOW_UP.toString());
                break;
            case SOCIAL:
                mapParams.put(Folders.IN.toString(), Folders.SOCIAL.toString());
                break;

        }
    }


    public static class CustomThreadComparator implements Comparator<Thread> {
        @Override
        public int compare(Thread obj1, Thread obj2) {
            long timeThread1 = obj1.last_message_timestamp;
            long timeThread2 = obj2.last_message_timestamp;

            return (int) (timeThread1 - timeThread2);
        }
    }
}
