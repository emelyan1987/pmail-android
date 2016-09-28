package com.planckmail.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.BaseActivity;
import com.planckmail.activities.MenuActivity;
import com.planckmail.activities.PeopleActivity;
import com.planckmail.adapters.RecyclePeopleAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.helper.InternetConnection;
import com.planckmail.helper.SpacesItemDecoration;
import com.planckmail.helper.UserHelper;
import com.planckmail.listeners.RecyclerItemClickListener;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Contact;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.service.NylasService;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Taras Matolinets on 01.06.15.
 */
public class PeopleFragment extends BaseFragment implements BaseFragment.OnBackPressed, TextWatcher {

    private static final int LIMIT_LOAD_MAILS = 500;
    private int mCount = 0;
    private boolean mFirstInit = false;
    private EditText mTextSearch;
    private RecyclePeopleAdapter mAdapter;
    private RecyclerView mRecyclePeople;
    private List<AccountInfo> mAccountInfoList;
    private ProgressBar mPrLoadContacts;
    private TextView mTvNoInternetConnection;

    private List<Contact> mListContacts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_people, container, false);

        initViews(view);
        setListeners();

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclePeople.setLayoutManager(layoutManager);
        mRecyclePeople.setHasFixedSize(true);

        addDivider();

//        // Add the sticky headers decoration
//        final StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mAdapter);
//        mRecyclePeople.addItemDecoration(headersDecor);

        //clear collection on back press
        String jsonList = getArguments().getString(BundleKeys.CACHED_DATA);

        if (!TextUtils.isEmpty(jsonList)) {
            mListContacts = JsonUtilFactory.getJsonUtil().fromJsonArray(jsonList, Contact.class);
        }
        getActivity().setTitle(getString(R.string.all_accounts));
        ((MenuActivity) getActivity()).getToolbar().setSubtitle(getString(R.string.contacts));

        setActionBar();

        mListContacts = contactsFromCursor();

        mAdapter = new RecyclePeopleAdapter(getActivity(), mListContacts);
        mRecyclePeople.setAdapter(mAdapter);
        mRecyclePeople.addItemDecoration(new SpacesItemDecoration(10));

        mAccountInfoList = UserHelper.getEmailAccountList(true);

        if (InternetConnection.isNetworkConnected(getActivity()))
            getEmailContacts();

        return view;
    }

    public void setActionBar() {
        ActionBar mActionBar = ((MenuActivity) getActivity()).getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayShowCustomEnabled(false);
        }
    }

    private void getEmailContacts() {
        for (int i = 0; i < mAccountInfoList.size(); i++) {
            AccountInfo accountInfo = mAccountInfoList.get(i);

            NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
            nylasServer.getAllContacts(LIMIT_LOAD_MAILS, new Callback<Object>() {

                        @Override
                        public void success(Object o, Response response) {
                            String json = (String) o;

                            ++mCount;
                            Log.i(PlanckMailApplication.TAG, "url " + response.getUrl());

                            List<Contact> list = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Contact.class);
                            mListContacts.addAll(list);

                            if (mCount == mAccountInfoList.size()) {
                                sendPeopleData();
                                //  Collections.sort(mListContacts);
                                mAdapter.updateData(mListContacts);
                                mAdapter.notifyDataSetChanged();
                                mFirstInit = true;
                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            if (error.getKind().equals(RetrofitError.Kind.NETWORK)) {
                                mTvNoInternetConnection.setVisibility(View.VISIBLE);
                                mPrLoadContacts.setVisibility(View.GONE);
                            }

                            Response r = error.getResponse();
                            if (r != null)
                                Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                        }
                    }
            );
        }
    }

    private void sendPeopleData() {
        String dateJson = JsonUtilFactory.getJsonUtil().toJson(mListContacts);

        Intent intent = new Intent(MenuActivity.CACHING_ACTION);
        intent.putExtra(BundleKeys.CACHED_DATA, dateJson);
        intent.putExtra(BundleKeys.ENUM_MENU, MenuActivity.EnumMenuActivity.CONTACTS);

        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCount = 0;
    }

    private void addDivider() {
        mRecyclePeople.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .sizeResId(R.dimen.edge_tiny)
                .colorResId(R.color.gray_divider)
                .build());
    }

    private void setListeners() {
        mTextSearch.addTextChangedListener(this);
        setRecycleListener();
    }

    private void initViews(View view) {
        mTextSearch = (EditText) view.findViewById(R.id.etSearch);
        mRecyclePeople = (RecyclerView) view.findViewById(R.id.recyclePeople);
        mPrLoadContacts = (ProgressBar) view.findViewById(R.id.prLoadContacts);
        mTvNoInternetConnection = (TextView) view.findViewById(R.id.tvNoInternetConnection);
    }

    private void setRecycleListener() {
        mRecyclePeople.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {

                    @Override
                    public void onItemClick(View view, int position) {
                        Contact contact = (Contact) view.getTag();
                        contact.setColor(mAdapter.getColorForBox(position));
                        String json = JsonUtilFactory.getJsonUtil().toJson(contact);
//                        Bundle bundle = new Bundle();
//                        bundle.putString(BundleKeys.CONTACT, json);
                        Intent intent = new Intent(getActivity(), PeopleActivity.class);
                        intent.putExtra(BundleKeys.CONTACT, json);
                        startActivity(intent);
                        // ((BaseActivity) getActivity()).replace(ContactDetailsFragment.class, R.id.mainContainer, bundle, true);
                    }
                })
        );
    }

    @Override
    public void onBackPress() {
        ((MenuActivity) getActivity()).setMailIconEnable();
        getActivity().getFragmentManager().popBackStackImmediate();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        List<Contact> filteredContact = new ArrayList<>();
        for (Contact c : mListContacts) {
            if (!TextUtils.isEmpty(c.getEmail())) {
                if (c.getEmail().contains(s.toString())) {
                    filteredContact.add(c);
                }
            } else if (!TextUtils.isEmpty(c.getEmail())) {
                if (c.getEmail().contains(s.toString())) {
                    filteredContact.add(c);
                }
            }

            mAdapter.updateData(filteredContact);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private List<Contact> contactsFromCursor() {
        Uri contactUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

        Cursor cursor = getActivity().getContentResolver().query(contactUri, null, null, null, null);

        List<Contact> allContacts = new ArrayList<>();
        String _ID = ContactsContract.Contacts._ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        String ADDRESS = ContactsContract.CommonDataKinds.Email.ADDRESS;
        Uri EmailCONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
        String DATA = ContactsContract.CommonDataKinds.Email.DATA;

        while (cursor.moveToNext()) {
            Contact contact = new Contact();

            String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            contact.setName(name);

            String phoneNumber = cursor.getString(cursor.getColumnIndex(NUMBER));
            contact.setPhone(phoneNumber);
            allContacts.add(contact);
        }
        cursor.close();

        return allContacts;
    }
}
