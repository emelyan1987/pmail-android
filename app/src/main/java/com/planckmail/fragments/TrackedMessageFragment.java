package com.planckmail.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.TrackedListActivity;
import com.planckmail.adapters.RecycleTrackedListAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.model.TrackedItem;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.views.DividerItemDecoration;
import com.planckmail.web.response.nylas.Message;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.service.NylasService;
import com.planckmail.web.restClient.service.PlanckService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class TrackedMessageFragment extends Fragment{

    private WebView mWebView;
    private ProgressBar mProgressBar;


    public TrackedMessageFragment() {}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_tracked_message, container, false);

        mWebView = (WebView) rootView.findViewById(R.id.tracked_wv_message);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.tracked_pb_loading);



        loadMessage();
        return rootView;
    }

    private void loadMessage() {

        mProgressBar.setVisibility(View.VISIBLE);

        TrackedListActivity activity = (TrackedListActivity)getActivity();
        final NylasService service = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, activity.mAccessToken, "");
        service.getMessage(activity.mTrackingItem.getMessagetId(), new Callback<String>() {

                @Override
                public void success(String s, Response response) {
                    mProgressBar.setVisibility(View.GONE);


                    Message message = JsonUtilFactory.getJsonUtil().fromJson(s, Message.class);

                    Log.i(PlanckMailApplication.TAG, "response url " + response.getUrl());

                    mWebView.getSettings().setJavaScriptEnabled(true);
                    mWebView.loadDataWithBaseURL("", message.body, "text/html", "UTF-8", "");
                }

                @Override
                public void failure(RetrofitError error) {
                    mProgressBar.setVisibility(View.GONE);
                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
            }
        );
    }
}
