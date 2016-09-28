package com.planckmail.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.MessageActivity;
import com.planckmail.adapters.RecycleMailPeopleInfoAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.helper.UserHelper;
import com.planckmail.listeners.RecyclerItemClickListener;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Contact;
import com.planckmail.web.response.nylas.Thread;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.service.NylasService;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Terry on 3/23/2016.
 */
public class MailPeopleFragment extends BaseFragment {
    private RecyclerView mRecycleView;
    private TextView mTvNoViews;
    private RecycleMailPeopleInfoAdapter mAdapter;
    private ProgressBar mProgress;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people_mail, container, false);
        initViews(view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        mRecycleView.setHasFixedSize(true);
        mRecycleView.setLayoutManager(layoutManager);
        mAdapter = new RecycleMailPeopleInfoAdapter(getActivity());
        mRecycleView.setAdapter(mAdapter);
        addDivider(R.dimen.edge_tiny);

        String jsonContact = getArguments().getString(BundleKeys.CONTACT);
        Contact contact = JsonUtilFactory.getJsonUtil().fromJson(jsonContact, Contact.class);
        if (contact.getEmail() != null)
            setData(contact);
        else {
            noEmails(contact);
        }
        addClickListener();

        return view;
    }

    private void addClickListener() {
        mRecycleView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                List<Thread> list = mAdapter.getListData();
                Thread thread = list.get(position);

                AccountInfo accountInfo = getAccountInfo(thread.account_id);
                String jsonThread = JsonUtilFactory.getJsonUtil().toJson(thread);
                Bundle bundle = new Bundle();

                if (accountInfo != null) {
                    bundle.putString(BundleKeys.ACCESS_TOKEN, accountInfo.accessToken);
                }
                bundle.putString(BundleKeys.TITLE, getString(R.string.messageDetails));
                bundle.putString(BundleKeys.THREAD, jsonThread);

                Intent intent = new Intent(getActivity(), MessageActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        }));
    }

    private void addDivider(int divider) {
        mRecycleView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .sizeResId(divider)
                .colorResId(R.color.gray_divider)
                .build());
    }

    private void noEmails(Contact contact) {
        mTvNoViews.setVisibility(View.VISIBLE);
        mTvNoViews.setText(getString(R.string.noEmailWith, contact.getName()));
    }

    private void setData(final Contact contact) {
        mProgress.setVisibility(View.VISIBLE);
        AccountInfo accountInfo = getAccountInfo(contact.account_id);

        if (accountInfo != null) {
            HashMap<String, String> map = new HashMap<>();
            map.put("any_email", contact.getEmail());
            NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
            nylasServer.getThreadInMail(map, new Callback<Object>() {
                @Override
                public void success(Object o, Response response) {
                    mProgress.setVisibility(View.GONE);
                    mTvNoViews.setVisibility(View.GONE);

                    String json = (String) o;
                    List<Thread> list = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Thread.class);
                    if (!list.isEmpty())
                        mAdapter.updateInfo(list);
                    else
                        noEmails(contact);
                }

                @Override
                public void failure(RetrofitError error) {
                    mProgress.setVisibility(View.GONE);
                    mTvNoViews.setVisibility(View.GONE);

                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
            });
        }
    }

    @Nullable
    private AccountInfo getAccountInfo(String account_id) {
        List<AccountInfo> accountInfoList = UserHelper.getEmailAccountList(true);

        AccountInfo accountInfo = null;
        for (AccountInfo a : accountInfoList) {
            if (account_id.equalsIgnoreCase(a.accountId))
                accountInfo = a;
        }
        return accountInfo;
    }

    private void initViews(View view) {
        mRecycleView = (RecyclerView) view.findViewById(R.id.recyclePeopleMessages);
        mTvNoViews = (TextView) view.findViewById(R.id.tvNoPeopleEmails);
        mProgress = (ProgressBar) view.findViewById(R.id.prLoadPeopleMails);
    }
}
