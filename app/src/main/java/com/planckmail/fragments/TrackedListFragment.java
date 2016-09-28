package com.planckmail.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.TrackedListActivity;
import com.planckmail.adapters.RecycleTrackedListAdapter;
import com.planckmail.adapters.RecycleTrackingListAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.model.TrackedItem;
import com.planckmail.data.model.TrackingItem;
import com.planckmail.listeners.RecyclerItemClickListener;
import com.planckmail.views.DividerItemDecoration;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.service.PlanckService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class TrackedListFragment extends Fragment{

    private RecyclerView mRecyclerView;
    private ArrayList<TrackedItem> mTrackedList = new ArrayList<TrackedItem>();
    private RecycleTrackedListAdapter mAdapter;

    private ProgressBar mProgressBar;

    private int mOpens;
    private int mClicks;
    private int mReplies;

    private TextView mTextViewOpens;
    private TextView mTextViewClicks;
    private TextView mTextViewReplies;




    public TrackedListFragment() {}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_tracked_list, container, false);

        mTextViewOpens = (TextView) rootView.findViewById(R.id.tracked_tv_opens);
        mTextViewClicks = (TextView) rootView.findViewById(R.id.tracked_tv_clicks);
        mTextViewReplies = (TextView) rootView.findViewById(R.id.tracked_tv_replies);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.tracked_rv_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new RecycleTrackedListAdapter(getActivity(), mTrackedList);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.tracked_pb_loading);



        loadData();
        return rootView;
    }

    private void loadData() {
        PlanckService service = new RestPlankMail(RestPlankMail.BASE_URL4).getPlankService();

        mProgressBar.setVisibility(View.VISIBLE);
        service.getTrackDetails(((TrackedListActivity)getActivity()).mTrackingItem.getId(), new Callback<String>() {

            @Override
            public void success(String s, Response response) {
                try {
                    JSONObject jsonResult = new JSONObject(s);

                    if(jsonResult!=null && jsonResult.get("success")==true) {
                        JSONArray jsonData = jsonResult.getJSONArray("data");

                        mTrackedList.clear();


                        mOpens = 0;
                        mClicks = 0;
                        mReplies = 0;

                        for(int i = 0; i<jsonData.length(); i++) {
                            JSONObject jsonItem = jsonData.getJSONObject(i);
                            TrackedItem trackedItem = new TrackedItem(jsonItem);

                            mTrackedList.add(i, trackedItem);


                            if(trackedItem.getAction().equalsIgnoreCase("O")) {
                                mOpens ++;
                            } else if(trackedItem.getAction().equalsIgnoreCase("L")) {
                                mClicks ++;
                            } else if(trackedItem.getAction().equalsIgnoreCase("R")) {
                                mReplies ++;
                            }
                        }


                        mAdapter.notifyDataSetChanged();

                        mTextViewOpens.setText(String.valueOf(mOpens) + " Opens");
                        mTextViewClicks.setText(String.valueOf(mClicks) + " Clicks");
                        mTextViewReplies.setText(String.valueOf(mReplies) + " Replies");
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
