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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.UnsubscribeListActivity;
import com.planckmail.activities.UnsubscribeListActivity.ISelectAction;
import com.planckmail.adapters.ArchiveSpammerAdapter;
import com.planckmail.adapters.ArchiveSpammerAdapter.OnChangeStateListener;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.dialogs.UnsubscribeDialog;
import com.planckmail.dialogs.UnsubscribeDialog.IRemoveUnsubscribeThread;
import com.planckmail.enums.Folders;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Thread;
import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.service.NylasService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.view.View.GONE;

public class ArchiveSpammerFragment extends BaseFragment implements OnClickListener, OnChangeStateListener, IRemoveUnsubscribeThread, OnRefreshListener, ISelectAction {
    private static final int COUNT_LIMIT_MAIL = 100;
    private static final int OFFSET = 0;
    private RecyclerView mRecyclerView;
    private ArchiveSpammerAdapter mArchiveSpammerAdapter;
    private TextView mTvNoMails;
    private ProgressBar mProgressBar;
    private TextView mTvUnsubscribe;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private AccountInfo mAccountInfo;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_archive_spammer, container, false);
        initView(view);

        setListeners();
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mArchiveSpammerAdapter = new ArchiveSpammerAdapter(getActivity(), this);
        mRecyclerView.setAdapter(mArchiveSpammerAdapter);
        String jsonAccountInfo = getArguments().getString(BundleKeys.ACCOUNT);
        mAccountInfo = JsonUtilFactory.getJsonUtil().fromJson(jsonAccountInfo, AccountInfo.class);

        loadData();

        return view;
    }

    private void setListeners() {
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mTvUnsubscribe.setOnClickListener(this);
    }

    private void loadData() {
        HashMap<String, String> mapParams = new HashMap<>();
        mapParams.put(Folders.IN.toString(), Folders.READ_LATER.toString());

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, mAccountInfo.getAccessToken(), "");
        nylasServer.getThreadsInbox(mapParams, COUNT_LIMIT_MAIL, OFFSET, new Callback<Object>() {
            @Override
            public void failure(RetrofitError restError) {
                mProgressBar.setVisibility(GONE);
                disableKeep();

                mSwipeRefreshLayout.setRefreshing(false);
                Response r = restError.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }

            @Override
            public void success(Object o, Response response) {
                String json = (String) o;

                disableKeep();

                mProgressBar.setVisibility(GONE);
                mSwipeRefreshLayout.setRefreshing(false);

                if (!json.equalsIgnoreCase("[]")) {
                    ArrayList<Thread> list = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Thread.class);
                    List<Participant> listParticipant = new ArrayList<>();

                    List<ArchiveSpammer> listSpammer = new ArrayList<>();

                    for (Thread thread : list) {
                        Participant participant = thread.participants.get(0);
                        listParticipant.add(participant);
                    }

                    Set<Participant> uniqueSet = new HashSet<>(listParticipant);

                    for (Participant participant : uniqueSet) {
                        ArchiveSpammer archiveSpammer = new ArchiveSpammer();
                        archiveSpammer.email = participant.getEmail();
                        archiveSpammer.name = participant.getName();
                        archiveSpammer.counter = Collections.frequency(listParticipant, participant);

                        listSpammer.add(archiveSpammer);
                    }

                    detectCommonSpammer(listSpammer);

                    mSwipeRefreshLayout.setRefreshing(false);
                    mArchiveSpammerAdapter.updateListEmails(listSpammer);
                    mTvNoMails.setVisibility(View.GONE);
                } else {
                    mTvNoMails.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void detectCommonSpammer(List<ArchiveSpammer> listSpammer) {
        Collections.sort(listSpammer, new Comparator<ArchiveSpammer>() {
            @Override
            public int compare(ArchiveSpammer obj1, ArchiveSpammer obj2) {
                if (obj1.counter > obj2.counter) {
                    return -1;
                } else if (obj1.counter < obj2.counter) {
                    return 1;
                }
                return 0;
            }
        });
    }

    private void disableKeep() {
        mTvUnsubscribe.setVisibility(View.VISIBLE);
        mTvUnsubscribe.setEnabled(false);
        mTvUnsubscribe.setTextColor(Color.GRAY);
    }

    private void initView(View view) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycleArchiveSpammer);
        mTvNoMails = (TextView) view.findViewById(R.id.tvNoMails);
        mProgressBar = (ProgressBar) view.findViewById(R.id.prLoadSpam);
        mTvUnsubscribe = (TextView) view.findViewById(R.id.tvUnsubscribe);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshUnsubscribe);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvUnsubscribe:
                ((UnsubscribeListActivity) getActivity()).setSelectAllState(false);
                getActivity().invalidateOptionsMenu();

                String jsonAccountInfo = JsonUtilFactory.getJsonUtil().toJson(mAccountInfo);
                String blockedListEmail = getBlockedListEmails();

                Participant participant = new Participant();
                participant.setEmail(blockedListEmail);
                String jsonParticipantInfo = JsonUtilFactory.getJsonUtil().toJson(participant);

                Bundle bundle = new Bundle();
                bundle.putString(BundleKeys.ACCOUNT, jsonAccountInfo);
                bundle.putString(BundleKeys.PARTICIPANT, jsonParticipantInfo);

                UnsubscribeDialog dialog = new UnsubscribeDialog();
                dialog.setArguments(bundle);
                dialog.setListener(this);
                dialog.show(getActivity().getFragmentManager(), dialog.getClass().getName());
                break;
        }
    }

    public String getBlockedListEmails() {
        Map<Integer, ArchiveSpammer> map = mArchiveSpammerAdapter.getCheckedBlockedEmails();
        StringBuilder builder = new StringBuilder();
        Set s1 = map.entrySet();
        Iterator itr = s1.iterator();

        while (itr.hasNext()) {
            Map.Entry m = (Map.Entry) itr.next();
            {
                ArchiveSpammer spammer = (ArchiveSpammer) m.getValue();
                builder.append(spammer.email);
                if (itr.hasNext()) {
                    builder.append(";");
                }
            }
        }
        return builder.toString();
    }

    @Override
    public void onChanged(Map<Integer, ArchiveSpammer> map) {
        if (!map.isEmpty()) {
            mTvUnsubscribe.setEnabled(true);
            mTvUnsubscribe.setTextColor(Color.WHITE);
        } else {
            mTvUnsubscribe.setEnabled(false);
            mTvUnsubscribe.setTextColor(Color.GRAY);
        }
    }

    @Override
    public void removeUnsubscribeThread(int position) {
        mProgressBar.setVisibility(View.VISIBLE);
        mTvUnsubscribe.setVisibility(GONE);
        mArchiveSpammerAdapter.clearData();
        loadData();
    }

    @Override
    public void onRefresh() {
        loadData();
    }

    @Override
    public void accountChanged(AccountInfo accountInfo) {
        mProgressBar.setVisibility(View.VISIBLE);
        mTvNoMails.setVisibility(GONE);
        mAccountInfo = accountInfo;
        mArchiveSpammerAdapter.clearData();

        loadData();
    }

    @Override
    public void selectAll() {
        mArchiveSpammerAdapter.selectAll();
    }

    @Override
    public void selectNone() {
        mArchiveSpammerAdapter.selectNone();
    }

    public class ArchiveSpammer {
        public String email;
        public String name;
        public int counter;
    }
}
