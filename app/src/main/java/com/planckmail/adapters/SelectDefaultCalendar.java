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
 * Created by Terry on 12/21/2015.
 */
public class SelectDefaultCalendar extends RecyclerView.Adapter<SelectDefaultCalendar.ViewHolderSelectDefaultCalendarAdapter> {

    private final Context mContext;
    private List<String> mListData;
    private int mSelectedPosition;

    public SelectDefaultCalendar(Context context, int position, List<String> data) {
        mListData = data;
        mContext = context;
        mSelectedPosition = position;
    }

    @Override
    public ViewHolderSelectDefaultCalendarAdapter onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_calendar_select_default_dialog, parent, false);
        return new ViewHolderSelectDefaultCalendarAdapter(view);
    }

    @Override
    public void onBindViewHolder(ViewHolderSelectDefaultCalendarAdapter holder, int position) {
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

    public class ViewHolderSelectDefaultCalendarAdapter extends RecyclerView.ViewHolder {
        public RadioButton radioSelectTime;

        public ViewHolderSelectDefaultCalendarAdapter(View itemView) {
            super(itemView);
            radioSelectTime = (RadioButton) itemView.findViewById(R.id.rbDefaultDialog);
        }
    }
}
