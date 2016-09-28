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
import com.planckmail.fragments.BlockedFragment;
import com.planckmail.web.response.planck.BlackList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Terry on 3/13/2016.
 */
public class BlockedAdapter extends RecyclerView.Adapter<BlockedAdapter.BlockedViewHolder> {
    private Context mContext;
    private List<BlackList.BlackSpammer> mListBlackEmails = new ArrayList<>();
    private Map<Integer, BlackList.BlackSpammer> mMapCheckedBlock = new HashMap<>();
    private OnChangeStateListener mListener;
    private boolean mSelectAllState;

    public BlockedAdapter(Context context, BlockedFragment blockedFragment) {
        mContext = context;
        mListener = blockedFragment;
    }

    @Override
    public BlockedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_unsubscribe_email, parent, false);
        return new BlockedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BlockedViewHolder holder, int position) {
        BlackList.BlackSpammer spammer = mListBlackEmails.get(position);

        holder.checkBoxBlockedEmail.setTag(R.string.tag_position, position);

        holder.mSpammerEmail.setText(spammer.email);
        holder.mSpammerName.setText(spammer.name);

        if (mMapCheckedBlock.containsKey(position)) {
            holder.checkBoxBlockedEmail.setChecked(true);
        } else {
            holder.checkBoxBlockedEmail.setChecked(false);
        }

        if (!TextUtils.isEmpty(spammer.name))
            holder.mSpammerName.setText(String.valueOf(spammer.name));
        else
            holder.mSpammerName.setText(mContext.getString(R.string.noName));
    }

    public Map<Integer, BlackList.BlackSpammer> getListCheckedBlock() {
        return mMapCheckedBlock;
    }

    public void updateListEmails(List<BlackList.BlackSpammer> list) {
        mListBlackEmails = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mListBlackEmails.size();
    }

    public void clearData() {
        mMapCheckedBlock.clear();
        mListBlackEmails.clear();

        notifyDataSetChanged();
    }

    public void selectAll() {
        mSelectAllState = true;
        for (int i = 0; i < mListBlackEmails.size(); i++) {
            BlackList.BlackSpammer spammer = mListBlackEmails.get(i);
            if (!mMapCheckedBlock.containsKey(i))
                mMapCheckedBlock.put(i, spammer);
        }
        notifyDataSetChanged();

        if (mListener != null)
            mListener.onChanged(mMapCheckedBlock);
    }

    public void selectNone() {
        mSelectAllState = false;
        mMapCheckedBlock.clear();
        notifyDataSetChanged();

        if (mListener != null)
            mListener.onChanged(mMapCheckedBlock);
    }

    public class BlockedViewHolder extends RecyclerView.ViewHolder implements OnCheckedChangeListener {

        public CheckBox checkBoxBlockedEmail;
        public TextView mCounter;
        public TextView mSpammerName;
        public TextView mSpammerEmail;

        public BlockedViewHolder(View itemView) {
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

            if (state && !mMapCheckedBlock.containsKey(position)) {
                BlackList.BlackSpammer spammer = mListBlackEmails.get(position);
                mMapCheckedBlock.put(position, spammer);
            }else if (!state && mMapCheckedBlock.containsKey(position)) {
                mMapCheckedBlock.remove(position);
            }

            if (mListener != null && !mSelectAllState)
                mListener.onChanged(mMapCheckedBlock);
        }
    }

    public interface OnChangeStateListener {
        void onChanged(Map<Integer, BlackList.BlackSpammer> map);
    }
}
