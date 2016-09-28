package com.planckmail.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.UnsubscribeListActivity;
import com.planckmail.activities.UnsubscribeListActivity.ISelectAction;
import com.planckmail.adapters.BlockedAdapter;
import com.planckmail.adapters.BlockedAdapter.OnChangeStateListener;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.planck.BlackList;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.service.PlanckService;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.view.View.GONE;

/**
 * Created by Terry on 3/13/2016.
 */
public class BlockedFragment extends BaseFragment implements OnClickListener, OnChangeStateListener, OnRefreshListener, ISelectAction {
    private RecyclerView mRecyclerView;
    private BlockedAdapter mBlockedAdapter;
    private TextView mTvKeep;
    private TextView mTvNoMails;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private AccountInfo mAccountInfo;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.blocked_fragment, container, false);
        String jsonAccountInfo = getArguments().getString(BundleKeys.ACCOUNT);
        mAccountInfo = JsonUtilFactory.getJsonUtil().fromJson(jsonAccountInfo, AccountInfo.class);

        initView(view);
        setListeners();
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mBlockedAdapter = new BlockedAdapter(getActivity(), this);
        mRecyclerView.setAdapter(mBlockedAdapter);

        loadData();
        return view;
    }

    private void setListeners() {
        mTvKeep.setOnClickListener(this);
        mSwipeRefreshLayout.setOnRefreshListener(this);
    }

    private void loadData() {
        RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL3);
        PlanckService service = client.getPlankService();
        service.getBlackListIds(mAccountInfo.getEmail(), new Callback<String>() {
            @Override
            public void success(String json, Response response) {
                mProgressBar.setVisibility(GONE);
                mTvKeep.setVisibility(View.VISIBLE);
                mTvKeep.setEnabled(false);
                mTvKeep.setTextColor(Color.GRAY);

                BlackList blackList = JsonUtilFactory.getJsonUtil().fromJson(json, BlackList.class);
                if (!blackList.blacklist.isEmpty()) {
                    mBlockedAdapter.updateListEmails(blackList.blacklist);
                    mTvNoMails.setVisibility(View.GONE);
                } else {
                    mTvNoMails.setVisibility(View.VISIBLE);
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void failure(RetrofitError error) {
                mProgressBar.setVisibility(GONE);
                mTvKeep.setVisibility(View.VISIBLE);
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private void initView(View view) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycleArchiveSpammer);
        mTvKeep = (TextView) view.findViewById(R.id.tvKeep);
        mTvNoMails = (TextView) view.findViewById(R.id.tvNoMails);
        mProgressBar = (ProgressBar) view.findViewById(R.id.prLoadSpam);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshKeep);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvKeep:
                ((UnsubscribeListActivity) getActivity()).setSelectAllState(false);
                getActivity().invalidateOptionsMenu();

                String keepListEmail = getKeepListEmails();
                RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL3);
                PlanckService service = client.getPlankService();
                service.removeFromBlackList(mAccountInfo.getEmail(), keepListEmail, new Callback<String>() {
                    @Override
                    public void success(String s, Response response) {
                        Log.i(PlanckMailApplication.TAG, "thread removed from blackList");
                        mProgressBar.setVisibility(View.VISIBLE);
                        mTvKeep.setVisibility(GONE);
                        mBlockedAdapter.clearData();
                        loadData();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                });
                break;
        }
    }

    public String getKeepListEmails() {
        Map<Integer, BlackList.BlackSpammer> map = mBlockedAdapter.getListCheckedBlock();
        StringBuilder builder = new StringBuilder();
        Set s1 = map.entrySet();
        Iterator itr = s1.iterator();

        while (itr.hasNext()) {
            Map.Entry m = (Map.Entry) itr.next();
            {
                BlackList.BlackSpammer spammer = (BlackList.BlackSpammer) m.getValue();
                builder.append(spammer.email);

                if (itr.hasNext()) {
                    builder.append(";");
                }
            }
        }
        return builder.toString();
    }

    @Override
    public void onChanged(Map<Integer, BlackList.BlackSpammer> list) {
        if (!list.isEmpty()) {
            mTvKeep.setEnabled(true);
            mTvKeep.setTextColor(Color.WHITE);
        } else {
            mTvKeep.setEnabled(false);
            mTvKeep.setTextColor(Color.GRAY);
        }
    }

    @Override
    public void onRefresh() {
        loadData();
    }

    @Override
    public void accountChanged(AccountInfo accountInfo) {
        mAccountInfo = accountInfo;
        mProgressBar.setVisibility(View.VISIBLE);
        mTvNoMails.setVisibility(GONE);
        mBlockedAdapter.clearData();
        loadData();
    }

    @Override
    public void selectAll() {
        mBlockedAdapter.selectAll();
    }

    @Override
    public void selectNone() {
        mBlockedAdapter.selectNone();
    }
}
