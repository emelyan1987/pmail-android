package com.planckmail.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.fragments.ArchiveSpammerFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Terry on 3/13/2016.
 */
public class ArchiveSpammerAdapter extends RecyclerView.Adapter<ArchiveSpammerAdapter.ActiveSpammerViewHolder> {
    private Context mContext;
    private List<ArchiveSpammerFragment.ArchiveSpammer> mListBlackEmails = new ArrayList<>();
    private Map<Integer, ArchiveSpammerFragment.ArchiveSpammer> mMapCheckedSpammer = new HashMap<>();
    private OnChangeStateListener mListener;
    private boolean mSelectAllState;

    public ArchiveSpammerAdapter(Context context, ArchiveSpammerFragment fragment) {
        mContext = context;
        mListener = fragment;
    }

    @Override
    public ActiveSpammerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_unsubscribe_email, parent, false);
        return new ActiveSpammerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ActiveSpammerViewHolder holder, int position) {
        ArchiveSpammerFragment.ArchiveSpammer spammer = mListBlackEmails.get(position);
        holder.checkBoxBlockedEmail.setTag(R.string.tag_position, position);

        if (mMapCheckedSpammer.containsKey(position)) {
            holder.checkBoxBlockedEmail.setChecked(true);
        } else {
            holder.checkBoxBlockedEmail.setChecked(false);
        }

        holder.mSpammerEmail.setText(spammer.email);
        holder.mCounter.setText(String.valueOf(spammer.counter));

        if (!TextUtils.isEmpty(spammer.name))
            holder.mSpammerName.setText(String.valueOf(spammer.name));
        else
            holder.mSpammerName.setText(mContext.getString(R.string.noName));
    }

    public void updateListEmails(List<ArchiveSpammerFragment.ArchiveSpammer> list) {
        mListBlackEmails = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mListBlackEmails.size();
    }

    public Map<Integer, ArchiveSpammerFragment.ArchiveSpammer> getCheckedBlockedEmails() {
        return mMapCheckedSpammer;
    }

    public void clearData() {
        mListBlackEmails.clear();
        mMapCheckedSpammer.clear();
        notifyDataSetChanged();
    }

    public void selectNone() {
        mSelectAllState = false;
        mMapCheckedSpammer.clear();
        notifyDataSetChanged();

        if (mListener != null)
            mListener.onChanged(mMapCheckedSpammer);
    }

    public void selectAll() {
        mSelectAllState = true;
        for (int i = 0; i < mListBlackEmails.size(); i++) {
            ArchiveSpammerFragment.ArchiveSpammer spammer = mListBlackEmails.get(i);
            if (!mMapCheckedSpammer.containsKey(i))
                mMapCheckedSpammer.put(i, spammer);
        }
        notifyDataSetChanged();

        if (mListener != null)
            mListener.onChanged(mMapCheckedSpammer);
    }

    public class ActiveSpammerViewHolder extends RecyclerView.ViewHolder implements OnCheckedChangeListener {

        public CheckBox checkBoxBlockedEmail;
        public TextView mCounter;
        public TextView mSpammerName;
        public TextView mSpammerEmail;

        public ActiveSpammerViewHolder(View itemView) {
            super(itemView);
            checkBoxBlockedEmail = (CheckBox) itemView.findViewById(R.id.cbSpammer);
            mCounter = (TextView) itemView.findViewById(R.id.tvMessageCount);
            mSpammerName = (TextView) itemView.findViewById(R.id.tvSpammerName);
            mSpammerEmail = (TextView) itemView.findViewById(R.id.tvSpammerEmail);
            checkBoxBlockedEmail.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean state) {
            int position = (int) compoundButton.getTag(R.string.tag_position);

            if (state && !mMapCheckedSpammer.containsKey(position)) {
                ArchiveSpammerFragment.ArchiveSpammer spammer = mListBlackEmails.get(position);
                mMapCheckedSpammer.put(getAdapterPosition(), spammer);
            } else if (!state && mMapCheckedSpammer.containsKey(position)) {
                mMapCheckedSpammer.remove(position);
            }

            if (mListener != null && !mSelectAllState) {
                mListener.onChanged(mMapCheckedSpammer);
            }
        }
    }

    public interface OnChangeStateListener {
        void onChanged(Map<Integer, ArchiveSpammerFragment.ArchiveSpammer> list);
    }
}
