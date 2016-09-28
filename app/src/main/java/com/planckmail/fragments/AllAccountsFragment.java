package com.planckmail.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.BaseActivity;
import com.planckmail.activities.ComposeActivity;
import com.planckmail.activities.MenuActivity;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.enums.AccountType;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Terry on 12/14/2015.
 */
public class AllAccountsFragment extends BaseFragment implements BaseFragment.OnBackPressed, OnClickListener {
    private List<AccountInfo> mListAccountInfo;
    private LinearLayout mLayoutFiles;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setActionBar();
    }

    public void setActionBar() {
        ActionBar mActionBar = null;
        if (getActivity() instanceof MenuActivity) {
            mActionBar = ((MenuActivity) getActivity()).getSupportActionBar();
        } else if (getActivity() instanceof ComposeActivity) {
            mActionBar = ((ComposeActivity) getActivity()).getSupportActionBar();
        }
        if (mActionBar != null)
            mActionBar.setDisplayShowCustomEnabled(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_email_account, container, false);

        initViews(view);
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        mListAccountInfo = manager.getAllAccountInfoList();
        fillViews();

        return view;
    }

    private void fillViews() {
        boolean isEmailViewAdded = false;
        boolean isFileViewAdded = false;

        //sort emails accounts first then files accounts
        sort();

        for (AccountInfo accountInfo : mListAccountInfo) {
            isEmailViewAdded = isEmailViewAdded(isEmailViewAdded, accountInfo);
            isFileViewAdded = isFileViewAdded(isFileViewAdded, accountInfo);

            View view = View.inflate(getActivity(), R.layout.elem_account_file, null);
            TextView tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
            tvAccountName.setText(accountInfo.getEmail());
            tvAccountName.setTag(R.string.tag_account_type, accountInfo.getAccountType());
            tvAccountName.setOnClickListener(this);
            Drawable drawable = UserHelper.getAccountDrawable(getActivity(), accountInfo.getAccountType());
            tvAccountName.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            mLayoutFiles.addView(view);
        }
    }

    private void sort() {
        Collections.sort(mListAccountInfo, new Comparator<AccountInfo>() {
            @Override
            public int compare(AccountInfo obj1, AccountInfo obj2) {
                boolean b1 = obj2.isEmailAccount();
                boolean b2 = obj1.isEmailAccount();
                if (b1 && !b2) {
                    return +1;
                }
                if (!b1 && b2) {
                    return -1;
                }
                return 0;
            }
        });
    }

    private boolean isFileViewAdded(boolean isFileViewAdded, AccountInfo accountInfo) {
        if (!isFileViewAdded && accountInfo.isEmailAccount) {
            addAccountToLayout(getResources().getString(R.string.emailAccounts), R.layout.elem_setting_header, R.id.tvName);
            isFileViewAdded = true;
        }
        return isFileViewAdded;
    }

    private boolean isEmailViewAdded(boolean isEmailViewAdded, AccountInfo accountInfo) {
        if (!isEmailViewAdded && !accountInfo.isEmailAccount) {
            addAccountToLayout(getResources().getString(R.string.fileAccounts), R.layout.elem_setting_header, R.id.tvName);
            isEmailViewAdded = true;
        }

        return isEmailViewAdded;
    }

    private void addAccountToLayout(String text, int viewId, int tvView) {
        View view = View.inflate(getActivity(), viewId, null);
        TextView settingOption = (TextView) view.findViewById(tvView);
        settingOption.setOnClickListener(this);
        settingOption.setText(text);

        mLayoutFiles.addView(view);
    }

    private void initViews(View view) {
        mLayoutFiles = (LinearLayout) view.findViewById(R.id.llAccountFiles);
    }

    @Override
    public void onBackPress() {
        getFragmentManager().popBackStackImmediate();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvAccountName:
                AccountType accountType = (AccountType) v.getTag(R.string.tag_account_type);
                Bundle bundle = new Bundle();
                int placeHolderId = getArguments().getInt(BundleKeys.VIEW_ID);
                bundle.putSerializable(BundleKeys.ACCOUNT_TYPE, accountType);

                switch (accountType) {
                    case DROP_BOX:
                        ((BaseActivity) getActivity()).replace(DropBoxFileFragment.class, placeHolderId, bundle, true);
                        break;
                    case ONE_DRIVE:
                        ((BaseActivity) getActivity()).replace(OneDriveFileFragment.class, placeHolderId, bundle, true);
                        break;
                    case GOOGLE_DRIVE:
                        ((BaseActivity) getActivity()).replace(GoogleDriveFileFragment.class, placeHolderId, bundle, true);
                        break;
                    case BOX:
                        ((BaseActivity) getActivity()).replace(BoxDriveFileFragment.class, placeHolderId, bundle, true);
                        break;
                    default:
                        ((BaseActivity) getActivity()).replace(EmailAccountsFragment.class, placeHolderId, bundle, true);
                        break;
                }
                break;
        }
    }

}
