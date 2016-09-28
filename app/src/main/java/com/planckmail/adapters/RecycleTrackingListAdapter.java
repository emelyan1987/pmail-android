package com.planckmail.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.data.model.TrackingItem;
import com.planckmail.web.helper.UtilHelpers;
import com.planckmail.web.response.nylas.Contact;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


/**
 * Created by Taras Matolinets on 01.06.15.
 */
public class RecycleTrackingListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<TrackingItem> mTrackingList;
    private HashMap<Integer, Integer> mMailMapColor = new HashMap<>();

    public RecycleTrackingListAdapter(Context context, ArrayList<TrackingItem> list) {
        mContext = context;
        mTrackingList = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_tracking_item, parent, false);

        return new ViewHolder(view);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TrackingItem item = mTrackingList.get(position);

        if (holder instanceof ViewHolder) {
//            int color = getColorForBox(position);
//
//            int radius = 300;
//            GradientDrawable shape = new GradientDrawable();
//            shape.setCornerRadius(radius);
//            shape.setColor(color);

            holder.itemView.setTag(item);

            ((ViewHolder) holder).tvSubject.setText(item.getSubject());
            ((ViewHolder) holder).tvTime.setText(item.getCreatedTime().toLocaleString());
            ((ViewHolder) holder).tvEmails.setText(item.getTargetEmails());
            ((ViewHolder) holder).tvOpens.setText(String.valueOf(item.getOpens()));
            ((ViewHolder) holder).tvClicks.setText(String.valueOf(item.getLinks()));
            ((ViewHolder) holder).tvReplies.setText(String.valueOf(item.getReplies()));
        }
    }

    @Override
    public int getItemCount() {
        return mTrackingList.size();
    }



    @Override
    public long getItemId(int position) {
        return mTrackingList.size();
    }



    @Override
    public void onClick(View v) {

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvSubject;
        public TextView tvEmails;
        public TextView tvTime;
        public TextView tvOpens;
        public TextView tvClicks;
        public TextView tvReplies;

        public ViewHolder(View itemView) {
            super(itemView);

            tvSubject = (TextView) itemView.findViewById(R.id.tracking_item_tv_subject);
            tvTime = (TextView) itemView.findViewById(R.id.tracking_item_tv_time);
            tvEmails = (TextView) itemView.findViewById(R.id.tracking_item_tv_emails);
            tvOpens = (TextView) itemView.findViewById(R.id.tracking_item_tv_opens);
            tvClicks = (TextView) itemView.findViewById(R.id.tracking_item_tv_clicks);
            tvReplies = (TextView) itemView.findViewById(R.id.tracking_item_tv_replies);

        }
    }
}
