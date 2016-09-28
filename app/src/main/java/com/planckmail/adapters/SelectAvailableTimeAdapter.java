package com.planckmail.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.planckmail.R;

import java.util.List;

/**
 * Created by Terry on 12/6/2015.
 */
public class SelectAvailableTimeAdapter extends RecyclerView.Adapter<SelectAvailableTimeAdapter.ViewHolderSelectTimeAdapter> {

    private final Context mContext;
    private List<String> mListData;
    private int mSelectedPosition;

    public SelectAvailableTimeAdapter(Context context, int position, List<String> data) {
        mListData = data;
        mContext = context;
        mSelectedPosition = position;
    }

    @Override
    public ViewHolderSelectTimeAdapter onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_select_available_time, parent, false);
        return new ViewHolderSelectTimeAdapter(view);
    }

    @Override
    public void onBindViewHolder(ViewHolderSelectTimeAdapter holder, int position) {
        String time = mListData.get(position);

        if (mSelectedPosition == position)
            holder.radioSelectTime.setChecked(true);
        else
            holder.radioSelectTime.setChecked(false);

        holder.radioSelectTime.setText(time);
    }

    @Override
    public int getItemCount() {
        return mListData.size();
    }

    public class ViewHolderSelectTimeAdapter extends RecyclerView.ViewHolder {
        public RadioButton radioSelectTime;

        public ViewHolderSelectTimeAdapter(View itemView) {
            super(itemView);
            radioSelectTime = (RadioButton) itemView.findViewById(R.id.rbTime);
        }
    }
}
