package com.planckmail.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.web.helper.UtilHelpers;
import com.planckmail.web.response.nylas.Contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by Taras Matolinets on 25.05.15.
 */
public class AutoCompleteContactAdapter extends BaseAdapter implements Filterable {

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;

    private List<Contact> mListData = new ArrayList<>();
    private HashMap<String, Integer> mMailMapColor = new HashMap<>();

    public AutoCompleteContactAdapter(Context context) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mListData.size();
    }

    @Override
    public Object getItem(int position) {
        return mListData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public void setListData(List<Contact> listData) {
        mListData = listData;
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = getView(parent);
        }
        ViewHolderContacts holder = (ViewHolderContacts) convertView.getTag();
        Contact contact = (Contact) getItem(position);
        int color = getColorForBox(contact.id);

        GradientDrawable shape = new GradientDrawable();
        float radius = 300;

        shape.setCornerRadius(radius);
        shape.setColor(color);

        holder.tvShortName.setBackgroundDrawable(shape);

        if (!TextUtils.isEmpty(contact.name) && contact.name.length() > 0) {
            String shortTitle = UtilHelpers.buildTitle(contact.name);
            holder.tvName.setText(contact.name);
            holder.tvEmail.setText(contact.email);
            holder.tvShortName.setText(shortTitle.toUpperCase());
        } else {
            String shortTitle = UtilHelpers.buildTitle(contact.email);
            holder.tvShortName.setText(shortTitle.toUpperCase());
            holder.tvName.setText(contact.email);
        }

        return convertView;
    }

    @NonNull
    public View getView(ViewGroup parent) {
        View convertView;
        ViewHolderContacts holder = new ViewHolderContacts();
        convertView = mLayoutInflater.inflate(R.layout.elem_contact, parent, false);

        holder.tvShortName = (TextView) convertView.findViewById(R.id.tvMailPicture);
        holder.tvName = (TextView) convertView.findViewById(R.id.tvName);
        holder.tvEmail = (TextView) convertView.findViewById(R.id.tvEmail);

        convertView.setTag(holder);
        return convertView;
    }

    private int getColorForBox(String id) {
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

    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    private Filter nameFilter = new Filter() {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            publishResults(constraint, filterResults);

            return filterResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    };

    public static class ViewHolderContacts {
        public TextView tvShortName;
        public TextView tvName;
        public TextView tvEmail;
    }
}
