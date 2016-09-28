package com.planckmail.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.web.response.nylas.wrapper.Folder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taras Matolinets on 27.09.15.
 */
public class DrawerFooterExpandableAdapter extends BaseExpandableListAdapter {

    private final LayoutInflater mInflater;
    private List<Folder> mListData = new ArrayList<>();
    private Context mContext;

    public DrawerFooterExpandableAdapter(Context context, List<Folder> list) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mListData = list;
    }

    @Override
    public int getGroupCount() {
        return 1;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mListData.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public void updateData(List<Folder> list) {
        mListData = list;
        notifyDataSetChanged();
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = getGroupView(parent);
        }
        ViewHolderFooter holder = (ViewHolderFooter) convertView.getTag();
        holder.tvTitle.setText(R.string.item_folders);

        return convertView;
    }

    @NonNull
    public View getChildView(ViewGroup parent) {
        View convertView;
        ViewHolderFooter holder = new ViewHolderFooter();

        convertView = mInflater.inflate(R.layout.elem_footer_child_expand_view, parent, false);
        holder.tvTitle = (TextView) convertView.findViewById(R.id.tvItem);

        convertView.setTag(holder);
        return convertView;
    }

    @NonNull
    public View getGroupView(ViewGroup parent) {
        View convertView;
        ViewHolderFooter holder = new ViewHolderFooter();

        convertView = mInflater.inflate(R.layout.elem_footer_group_expand_view, parent, false);
        holder.tvTitle = (TextView) convertView.findViewById(R.id.tvItemGroup);

        convertView.setTag(holder);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Folder folder = mListData.get(childPosition);
        if (convertView == null) {
            convertView = getChildView(parent);
        }
        ViewHolderFooter holder = (ViewHolderFooter) convertView.getTag();
        holder.tvTitle.setText(folder.display_name);

        return convertView;
    }


    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private class ViewHolderFooter {
        public TextView tvTitle;
    }
}
