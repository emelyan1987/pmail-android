package com.planckmail.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.data.model.TrackedItem;
import com.planckmail.data.model.TrackingItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by Taras Matolinets on 01.06.15.
 */
public class RecycleTrackedListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<TrackedItem> mTrackedList;
    private HashMap<Integer, Integer> mMailMapColor = new HashMap<>();

    public RecycleTrackedListAdapter(Context context, ArrayList<TrackedItem> list) {
        mContext = context;
        mTrackedList = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_tracked_item, parent, false);

        return new ViewHolder(view);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TrackedItem item = mTrackedList.get(position);

        if (holder instanceof ViewHolder) {
//            int color = getColorForBox(position);
//
//            int radius = 300;
//            GradientDrawable shape = new GradientDrawable();
//            shape.setCornerRadius(radius);
//            shape.setColor(color);

            holder.itemView.setTag(item);

            ((ViewHolder) holder).tvActor.setText(item.getActor());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            ((ViewHolder) holder).tvTime.setText(item.getCreatedTime().toLocaleString());
            ((ViewHolder) holder).tvLocation.setText(item.getLocation());

            if(item.getAction().equalsIgnoreCase("O")) {
                ((ViewHolder) holder).ivAction.setImageResource(R.drawable.ic_opened);
            } else if(item.getAction().equalsIgnoreCase("L")) {
                ((ViewHolder) holder).ivAction.setImageResource(R.drawable.ic_clicked);
            } else if(item.getAction().equalsIgnoreCase("R")) {
                ((ViewHolder) holder).ivAction.setImageResource(R.drawable.ic_clicked);
            }

            if(item.isMobile()) {
                ((ViewHolder) holder).ivDevice.setImageResource(R.drawable.ic_mobile);
            } else {
                ((ViewHolder) holder).ivDevice.setImageResource(R.drawable.ic_desktop);
            }

        }
    }

    @Override
    public int getItemCount() {
        return mTrackedList.size();
    }



    @Override
    public long getItemId(int position) {
        return mTrackedList.size();
    }



    @Override
    public void onClick(View v) {

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivAction;
        public TextView tvActor;
        public TextView tvTime;
        public TextView tvLocation;
        public ImageView ivDevice;

        public ViewHolder(View itemView) {
            super(itemView);

            ivAction = (ImageView) itemView.findViewById(R.id.tracked_item_iv_action);
            tvTime = (TextView) itemView.findViewById(R.id.tracked_item_tv_time);
            tvActor = (TextView) itemView.findViewById(R.id.tracked_item_tv_actor);
            tvLocation = (TextView) itemView.findViewById(R.id.tracked_item_tv_location);
            ivDevice = (ImageView) itemView.findViewById(R.id.tracked_item_iv_device);

        }
    }
}
