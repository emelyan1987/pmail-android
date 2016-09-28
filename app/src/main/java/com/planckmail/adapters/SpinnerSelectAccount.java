package com.planckmail.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.data.db.beans.AccountInfo;

import java.util.List;

/**
 * Created by Terry on 4/3/2016.
 */
public class SpinnerSelectAccount extends BaseAdapter {
    private Context context;
    private List<AccountInfo> mData;
    public Resources res;
    private LayoutInflater inflater;

    public SpinnerSelectAccount(Context context, List<AccountInfo> objects) {
        this.context = context;
        mData = objects;
        inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AccountInfo accountInfo = mData.get(position);
        ViewHolderSpinner holderSpinner;

        if (convertView == null) {
            holderSpinner = new ViewHolderSpinner();
            convertView = inflater.inflate(R.layout.elem_spinner_email, parent, false);
            holderSpinner.tvAccountName = (TextView) convertView.findViewById(R.id.tvAccount);
            convertView.setTag(holderSpinner);
        } else {
            holderSpinner = (ViewHolderSpinner) convertView.getTag();
        }
        holderSpinner.tvAccountName.setText(accountInfo.getEmail());

        return convertView;
    }

    static class ViewHolderSpinner {
        public TextView tvAccountName;
    }
}
