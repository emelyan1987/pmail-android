package com.planckmail.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.planckmail.R;

import java.util.ArrayList;

/**
 * Created by Taras Matolinets on 25.05.15.
 */
public class SpinnerAdapter extends ArrayAdapter<String> {

    private final ArrayList<String> mListData;
    private final LayoutInflater mLayoutInflater;
    private Context mContext;

    public SpinnerAdapter(Context context, ArrayList<String> list, int resource) {
        super(context, resource, list);
        mContext = context;
        mListData = list;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        String email = mListData.get(position);

        if (convertView == null) {
            ViewHolderSpinner holder = new ViewHolderSpinner();

            convertView = mLayoutInflater.inflate(R.layout.elem_spinner, parent, false);
            holder.tvEmail = (TextView) convertView.findViewById(R.id.tvSpinnerEmail);

            convertView.setTag(holder);
        }
        ViewHolderSpinner holder = (ViewHolderSpinner) convertView.getTag();

        holder.tvEmail.setText(email);

        return convertView;
    }

    public static class ViewHolderSpinner {
        public TextView tvEmail;
    }
}
