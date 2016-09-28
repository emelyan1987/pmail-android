package com.planckmail.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.opengl.Visibility;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.planckmail.R;
import com.planckmail.activities.PeopleActivity;
import com.planckmail.activities.TrackedListActivity;
import com.planckmail.adapters.RecycleTrackingListAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.model.TrackingItem;
import com.planckmail.listeners.RecyclerItemClickListener;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.views.DividerItemDecoration;
import com.planckmail.web.response.nylas.Contact;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.service.PlanckService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class TrackingListFragment extends Fragment{

    private AccountInfo mAccountInfo;
    private String mFilterStatus;
    private String mFilterTime;


    private ArrayList<TrackingItem> mTrackingList = new ArrayList<TrackingItem>();

    private int mTotals = 0;
    private int mOpens = 0;
    private int mClicks = 0;
    private int mReplies = 0;

    private RadioButton mRadioOpened;
    private RadioButton mRadioSent;
    private RadioButton mRadioUnopened;

    private TextView mTextViewTotals;
    private TextView mTextViewOpens;
    private TextView mTextViewClicks;
    private TextView mTextViewReplies;

    private RecyclerView mRecyclerView;
    private RecycleTrackingListAdapter mAdapter;
    private ProgressBar mProgressBar;



    public TrackingListFragment() {}

    @SuppressLint("ValidFragment")
    public TrackingListFragment(AccountInfo accountInfo, String filterTime) {
        super();
        this.mAccountInfo = accountInfo;
        this.mFilterTime = filterTime;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_tracking_list, container, false);

        mTextViewTotals = (TextView)rootView.findViewById(R.id.tracking_tv_totals);
        mTextViewOpens = (TextView)rootView.findViewById(R.id.tracking_tv_opens);
        mTextViewClicks = (TextView)rootView.findViewById(R.id.tracking_tv_clicks);
        mTextViewReplies = (TextView)rootView.findViewById(R.id.tracking_tv_replies);

        mRadioOpened = (RadioButton)rootView.findViewById(R.id.tracking_radio_opened);
        mRadioSent = (RadioButton)rootView.findViewById(R.id.tracking_radio_sent);
        mRadioUnopened = (RadioButton)rootView.findViewById(R.id.tracking_radio_unopened);

        mRadioOpened.setOnClickListener(radioOnClickListener);
        mRadioSent.setOnClickListener(radioOnClickListener);
        mRadioUnopened.setOnClickListener(radioOnClickListener);

        mRadioSent.setChecked(true);
        mFilterStatus = null;

        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.tracking_rv_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new RecycleTrackingListAdapter(getActivity(), mTrackingList);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {

            @Override
            public void onItemClick(View view, int position) {
                TrackingItem item = (TrackingItem) view.getTag();


                Intent intent = new Intent(getActivity(), TrackedListActivity.class);

                intent.putExtra("trackingItem", item);
                intent.putExtra("accessToken", mAccountInfo.getAccessToken());

                startActivity(intent);

                Log.i(PlanckMailApplication.TAG, item.getSubject());
            }
        }));

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.tracking_pb_loading);



        loadData();
        return rootView;
    }

    RadioButton.OnClickListener radioOnClickListener = new RadioButton.OnClickListener() {

        public void onClick(View v) {

            if(mRadioOpened.isChecked()) mFilterStatus = RestPlankMail.TrackingListFilterStatusOpened;
            else if(mRadioUnopened.isChecked()) mFilterStatus = RestPlankMail.TrackingListFilterStatusUnopened;
            else mFilterStatus = null;

            loadData();
        }
    };

    public void changeAccount(AccountInfo accountInfo) {
        mAccountInfo = accountInfo;

        loadData();
    }

    private void loadData() {
        PlanckService service = new RestPlankMail(RestPlankMail.BASE_URL4).getPlankService();

        mProgressBar.setVisibility(View.VISIBLE);
        service.getTrackingList(mAccountInfo.getEmail(), mFilterStatus, mFilterTime, new Callback<String>() {

            @Override
            public void success(String s, Response response) {
                try {
                    JSONObject jsonResult = new JSONObject(s);

                    if(jsonResult!=null && jsonResult.get("success")==true) {
                        JSONArray jsonData = jsonResult.getJSONArray("data");

                        mTrackingList.clear();

                        if(mFilterStatus == null) {
                            mOpens = 0;
                            mClicks = 0;
                            mReplies = 0;
                        }
                        for(int i = 0; i<jsonData.length(); i++) {
                            JSONObject jsonItem = jsonData.getJSONObject(i);
                            TrackingItem trackingItem = new TrackingItem(jsonItem);

                            mTrackingList.add(i, trackingItem);

                            if(mFilterStatus == null) {
                                mOpens += trackingItem.getOpens();
                                mClicks += trackingItem.getLinks();
                                mReplies += trackingItem.getReplies();
                            }
                        }


                        mAdapter.notifyDataSetChanged();

                        if(mFilterStatus == null) {
                            mTotals = mTrackingList.size();

                            mTextViewTotals.setText(String.valueOf(mTotals) + " Totals");
                            mTextViewOpens.setText(String.valueOf(mOpens) + " Opens");
                            mTextViewClicks.setText(String.valueOf(mClicks) + " Clicks");
                            mTextViewReplies.setText(String.valueOf(mReplies) + " Replies");
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null) {
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
                mProgressBar.setVisibility(View.GONE);
            }
        });
    }
}
