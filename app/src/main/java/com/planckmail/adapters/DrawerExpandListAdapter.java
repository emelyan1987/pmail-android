package com.planckmail.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.MenuActivity;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.fragments.ThreadFragment;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.views.FooterExpandableListView;
import com.planckmail.web.response.nylas.wrapper.Folder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Taras Matolinets on 02.05.15.
 */
public class DrawerExpandListAdapter extends BaseExpandableListAdapter implements ExpandableListView.OnChildClickListener {

    public static final int COUNT_ACCOUNT = 2;
    public static final int NORMAL_VIEW = 0;
    public static final int FOOTER_VIEW = 1;
    private static final int CHILD_TYPE_COUNT = 2;
    private final LayoutInflater mInflater;

    private int mChildPosition;
    private int mSelectedChild;
    private int mSelectedGroup;
    private Context mContext;

    private TypedArray mImagesArrayBack;
    private final TypedArray mImagesArrayGrey;

    private String[] mTitleArraySingleAccount;
    private String[] mTitleArrayAllAccount;
    private List<AccountInfo> mListDataParent = new ArrayList<>();

    private LinkedHashMap<String, Integer> mCountEmails = new LinkedHashMap<>();
    private List<Folder> mListFolders = new ArrayList<>();
    private int mPrimaryGroupId;
    private DrawerFooterExpandableAdapter mInnerAdapter;
    private int mPreviousGroupId = -1;

    public DrawerExpandListAdapter(Context context) {
        mContext = context;

        mInnerAdapter = new DrawerFooterExpandableAdapter(mContext, mListFolders);
        mImagesArrayBack = context.getResources().obtainTypedArray(R.array.array_mail_section_black);
        mImagesArrayGrey = context.getResources().obtainTypedArray(R.array.array_mail_section_grey);
        mTitleArraySingleAccount = context.getResources().getStringArray(R.array.arrayMailSectionSingleAccount);
        mTitleArrayAllAccount = context.getResources().getStringArray(R.array.arrayMailSectionAllAccount);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void loadAccounts(boolean isMail, boolean loadAllAccounts) {
        int countMail = 2;
        List<AccountInfo> list;

        if (loadAllAccounts)
            list = getAllListAccounts();
        else
            list = getListAccounts(isMail);

        if (list.size() >= countMail) {
            AccountInfo accountInfo = new AccountInfo();
            accountInfo.setEmail(mContext.getResources().getString(R.string.all_accounts));
            accountInfo.setIsEmailAccount(isMail);
            accountInfo.setAccountId(MenuActivity.KEY_ALL_MAIL_ACCOUNT_ID);
            list.add(0, accountInfo);
        }
        mListDataParent = list;
        notifyDataSetChanged();
    }

    public void setMapFolders(List<Folder> list) {
        mListFolders = list;
    }

    private List<AccountInfo> getListAccounts(boolean isMail) {
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        return manager.getEmailAccountInfoList(isMail);
    }

    private List<AccountInfo> getAllListAccounts() {
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        return manager.getAllAccountInfoList();
    }

    @Override
    public int getGroupCount() {
        return mListDataParent.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (groupPosition == 0 && mListDataParent.size() >= COUNT_ACCOUNT)
            return mTitleArrayAllAccount.length;
        else {
            return mTitleArraySingleAccount.length;
        }
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mListDataParent.get(groupPosition);
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
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        AccountInfo accountInfo = mListDataParent.get(groupPosition);

        if (groupPosition == 0 && mListDataParent.size() >= COUNT_ACCOUNT) {
            //build all account item
            convertView = setGroupViewHeader(convertView, parent);
        } else {
            convertView = setNormalGroupView(convertView, parent);
        }
        setGroupData(groupPosition, convertView, accountInfo);
        return convertView;
    }

    public void setGroupData(int groupPosition, View convertView, AccountInfo accountInfo) {
        ViewHolderNormal holder = (ViewHolderNormal) convertView.getTag();

        if (holder != null) {
            Drawable drawable;
            if (groupPosition == 0 && mListDataParent.size() >= COUNT_ACCOUNT) {
                drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_mail_black);
            } else
                drawable = UserHelper.getAccountDrawable(mContext, accountInfo.getAccountType());
            holder.tvMail.setCompoundDrawablesWithIntrinsicBounds(drawable, null, ContextCompat.getDrawable(mContext, R.drawable.ic_expand_more_black), null);

            holder.tvMail.setText(accountInfo.email);
        }
    }

    private View setGroupViewHeader(View convertView, ViewGroup parent) {
        if (convertView == null) {
            ViewHolderNormal holder = new ViewHolderNormal();
            convertView = mInflater.inflate(R.layout.elem_menu_group_header, parent, false);
            holder.tvMail = (TextView) convertView.findViewById(R.id.tvMail);
            holder.llMainGroup = (LinearLayout) convertView.findViewById(R.id.llMainGroup);

            convertView.setTag(holder);
        }
        return convertView;
    }

    private View setNormalGroupView(View convertView, ViewGroup parent) {
        if (convertView == null) {
            ViewHolderNormal holder = new ViewHolderNormal();

            convertView = mInflater.inflate(R.layout.grop_list_element, parent, false);
            holder.tvMail = (TextView) convertView.findViewById(R.id.tvMail);
            holder.llMainGroup = (LinearLayout) convertView.findViewById(R.id.llMainGroup);

            convertView.setTag(holder);
        }
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String title;

        if (groupPosition == 0 && mListDataParent.size() >= COUNT_ACCOUNT) {
            title = mTitleArrayAllAccount[childPosition];
        } else {
            title = mTitleArraySingleAccount[childPosition];
        }
        convertView = getChildView(groupPosition, childPosition, convertView, parent, title);

        return convertView;
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        boolean firstRule = groupPosition == 0 && mListDataParent.size() >= COUNT_ACCOUNT;
        boolean secondRule = groupPosition != 0 && mListDataParent.size() >= COUNT_ACCOUNT;
        boolean thirdRule = groupPosition == 0 && mListDataParent.size() < COUNT_ACCOUNT;
        int length = getArrayLength(firstRule);

        if (firstRule)
            return NORMAL_VIEW;
        else if (secondRule && childPosition != length - 1)
            return NORMAL_VIEW;
        else if (thirdRule && childPosition != length - 1)
            return NORMAL_VIEW;
        else
            return FOOTER_VIEW;
    }

    public int getArrayLength(boolean firstRule) {
        int size;

        if (firstRule)
            size = mTitleArrayAllAccount.length;
        else
            size = mTitleArraySingleAccount.length;
        return size;
    }


    public LinkedHashMap<String, Integer> getCountEmails() {
        return mCountEmails;
    }

    @Override
    public int getChildTypeCount() {
        return CHILD_TYPE_COUNT;
    }

    public View getChildView(int groupPosition, int childPosition, View convertView, ViewGroup parent, String title) {

        mPrimaryGroupId = groupPosition;
        int childType = getChildType(groupPosition, childPosition);

        switch (childType) {
            case NORMAL_VIEW:
                if (convertView == null) {
                    convertView = getNormalView(parent);
                }
                setDate(groupPosition, childPosition, convertView, title);
                break;
            case FOOTER_VIEW:
                if (convertView == null)
                    convertView = getFooterView();
                else {
                    if (mPrimaryGroupId != mPreviousGroupId) {
                        mPreviousGroupId = mPrimaryGroupId;
                        ((ExpandableListView) convertView).collapseGroup(0);
                    }
                    mInnerAdapter.updateData(mListFolders);
                }
                break;
        }
        return convertView;
    }

    public void setDate(int groupPosition, int childPosition, View convertView, String title) {
        ViewHolderNormal holder = (ViewHolderNormal) convertView.getTag();

        holder.tvTitle.setText(title);

        Drawable drawable = setBackground(groupPosition, childPosition, convertView, holder);
        //show count in inbox and all mail section
        countInbox(groupPosition, childPosition, holder);
        holder.tvTitle.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
    }

    public void countInbox(int groupPosition, int childPosition, ViewHolderNormal holder) {
        if (!mCountEmails.isEmpty() && mChildPosition == childPosition) {
            String key = mListDataParent.get(groupPosition).getAccountId();
            if (mCountEmails.containsKey(key) && mCountEmails != null) {
                int count = mCountEmails.get(key);
                if (count != 0)
                    holder.tvCount.setText(String.valueOf(count));
                else
                    holder.tvCount.setText("");
            }
        } else
            holder.tvCount.setText("");
    }

    public Drawable setBackground(int groupPosition, int childPosition, View convertView, ViewHolderNormal holder) {
        Drawable drawable;
        if (mSelectedChild == childPosition && groupPosition == mSelectedGroup) {
            drawable = mImagesArrayBack.getDrawable(childPosition);
            holder.tvTitle.setTextColor(Color.BLACK);
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.lightGreen));
        } else {
            drawable = mImagesArrayGrey.getDrawable(childPosition);
            holder.tvTitle.setTextColor(mContext.getResources().getColor(R.color.gray));
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.gray_email));
        }
        return drawable;
    }

    @NonNull
    public View getNormalView(ViewGroup parent) {
        ViewHolderNormal holder = new ViewHolderNormal();

        View convertView = mInflater.inflate(R.layout.elem_child_menu, parent, false);
        holder.tvTitle = (TextView) convertView.findViewById(R.id.tvItem);
        holder.tvCount = (TextView) convertView.findViewById(R.id.tvCount);

        convertView.setTag(holder);
        return convertView;
    }

    @NonNull
    public View getFooterView() {
        mInnerAdapter.updateData(mListFolders);
        FooterExpandableListView listView = new FooterExpandableListView(mContext);
        listView.setAdapter(mInnerAdapter);
        listView.setGroupIndicator(null);
        listView.setOnChildClickListener(this);
        return listView;
    }

    public void changeBackgroundSelectedItem(int groupPosition, int childPosition) {
        mSelectedGroup = groupPosition;
        mSelectedChild = childPosition;
        notifyDataSetChanged();
    }

    public void setInboxCount(LinkedHashMap<String, Integer> list, int childPosition) {
        mChildPosition = childPosition;
        mCountEmails = list;
        notifyDataSetChanged();
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

        Folder folder = mListFolders.get(childPosition);
        Bundle bundle = new Bundle();

        bundle.putBoolean(BundleKeys.IS_SECOND_LAYER_LIST, true);
        bundle.putInt(BundleKeys.GROUP_ID, mPrimaryGroupId);
        bundle.putInt(BundleKeys.CHILD_ID, childPosition);
        bundle.putString(BundleKeys.FOLDER_NAME, folder.display_name);

        ((MenuActivity) mContext).closeDrawer();
        ((MenuActivity) mContext).getToolbar().setTitle(folder.display_name);

        for (AccountInfo accountInfo : mListDataParent) {
            if (accountInfo.getAccountId().equalsIgnoreCase(folder.account_id))
                ((MenuActivity) mContext).getToolbar().setSubtitle(accountInfo.getEmail());
        }

        ((MenuActivity) mContext).replace(ThreadFragment.class, R.id.mainContainer, bundle, false);
        return false;
    }

    private class ViewHolderNormal {
        public TextView tvTitle;
        public TextView tvCount;
        public TextView tvMail;
        public LinearLayout llMainGroup;
    }
}
