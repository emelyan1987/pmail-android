package com.planckmail.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.web.helper.UtilHelpers;
import com.planckmail.web.response.nylas.Thread;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by Terry on 3/28/2016.
 */
public class RecycleMailPeopleInfoAdapter extends RecyclerView.Adapter<RecycleMailPeopleInfoAdapter.MailViewHolder> {
    private static final long TIME_STAMP = 1000;

    private final Context mContext;
    private List<Thread> mListData = new ArrayList<>();
    private HashMap<Integer, Integer> mMailMapColor = new HashMap<>();

    public RecycleMailPeopleInfoAdapter(Context context) {
        mContext = context;
    }

    @Override
    public MailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.people_mail_info, parent, false);

        return new MailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MailViewHolder holder, int position) {
        Thread thread = mListData.get(position);
        setBackground(thread, holder, position);
        setTitle(thread, holder);
        setMessageCount(holder, thread);
        setThreadInformation(holder, thread);
    }

    public void updateInfo(List<Thread> list) {
        mListData = list;
        notifyDataSetChanged();
    }

    private void setMessageCount(MailViewHolder viewHolder, Thread thread) {
        int size = thread.message_ids.size();

        int minMessage = 1;
        if (size > minMessage) {
            viewHolder.tvCountMessage.setVisibility(View.VISIBLE);
            viewHolder.tvCountMessage.setText(String.valueOf(size));
        } else
            viewHolder.tvCountMessage.setVisibility(View.GONE);
    }

    private void setThreadInformation(MailViewHolder viewHolder, Thread thread) {
        viewHolder.tvDate.setText(parseDate(thread.last_message_timestamp));
        Typeface typeFace = getTypeFace(thread);
        viewHolder.tvSubject.setTypeface(typeFace);

        if (!TextUtils.isEmpty(thread.subject)) {
            viewHolder.tvSubject.setText(thread.subject);
        } else {
            viewHolder.tvSubject.setText("-");
        }
        viewHolder.tvSnippet.setSingleLine();

        if (!TextUtils.isEmpty(thread.snippet))
            viewHolder.tvSnippet.setText(thread.snippet);
        else
            viewHolder.tvSnippet.setText("-");
    }

    private Typeface getTypeFace(Thread thread) {
        if (thread.unread) {
            return Typeface.DEFAULT_BOLD;
        }
        return Typeface.DEFAULT;
    }

    private void setTitle(Thread thread, final MailViewHolder holder) {
        final String title = UtilHelpers.getParticipants(thread.participants);
        String shortName = UtilHelpers.buildTitle(title);
        holder.tvName.setText(title);
        holder.tvMailPicture.setText(shortName);
    }

    private void setBackground(Thread thread, MailViewHolder holder, int position) {
        int color = getColorForBox(position);

        int radius = 300;
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radius);
        shape.setColor(color);

        holder.tvMailPicture.setBackgroundDrawable(shape);

//        int backgroundColor = getColorBackground(thread);
//        holder.flPicture.setBackgroundColor(backgroundColor);

        int resId = getActionPicture(thread);
        if (resId != 0) {
            holder.ivShowFile.setBackgroundResource(resId);
            holder.ivShowFile.setVisibility(View.VISIBLE);
        } else
            holder.ivShowFile.setVisibility(View.GONE);
    }

    private int getColorForBox(int id) {
        int color;
        if (mMailMapColor.containsKey(id))
            color = mMailMapColor.get(id);
        else {
            Random rnd = new Random();
            color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
            mMailMapColor.put(id, color);
        }
        return color;
    }

    private int getColorBackground(Thread thread) {
        if (thread.unread) {
            return mContext.getResources().getColor(R.color.gray_email);
        }
        return Color.WHITE;
    }

    private int getActionPicture(Thread thread) {
        if (thread.has_attachments) {
            return R.drawable.ic_attachment_black;
        }
        return 0;
    }

    private String parseDate(long millis) {
        Calendar calendar = Calendar.getInstance();
        String formattedDate;

        Date date = new Date(millis * TIME_STAMP);
        SimpleDateFormat formatter;
        calendar.setTime(date);

        boolean day = calendar.get(java.util.Calendar.DAY_OF_MONTH) != calendar.get(java.util.Calendar.DAY_OF_MONTH);
        boolean month = calendar.get(java.util.Calendar.MONTH) != calendar.get(java.util.Calendar.MONTH);
        boolean year = calendar.get(java.util.Calendar.YEAR) != calendar.get(java.util.Calendar.YEAR);

        if (day && month && year) {
            formatter = new SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault());
            formattedDate = formatter.format(date);
        } else {
            formatter = new SimpleDateFormat("HH:mm a", java.util.Locale.getDefault());
            formattedDate = formatter.format(date);
        }
        return formattedDate;
    }

    @Override
    public int getItemCount() {
        return mListData.size();
    }

    public List<Thread> getListData() {
        return mListData;
    }

    public static class MailViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout mailContent;
        public FrameLayout flPicture;
        public TextView tvMailPicture;
        public TextView tvName;
        public TextView tvSubject;
        public TextView tvSnippet;
        public TextView tvDate;
        public TextView tvCountMessage;
        public ImageView ivShowFile;

        public MailViewHolder(View itemView) {
            super(itemView);
            flPicture = (FrameLayout) itemView.findViewById(R.id.flPicture);
            mailContent = (LinearLayout) itemView.findViewById(R.id.llMainContent);
            tvMailPicture = (TextView) itemView.findViewById(R.id.tvMailPicture);
            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvSubject = (TextView) itemView.findViewById(R.id.tvSubject);
            tvSnippet = (TextView) itemView.findViewById(R.id.tvSnippet);
            tvDate = (TextView) itemView.findViewById(R.id.tvDate);
            tvCountMessage = (TextView) itemView.findViewById(R.id.tvCountMessages);
            ivShowFile = (ImageView) itemView.findViewById(R.id.ivShowFile);

        }
    }
}
