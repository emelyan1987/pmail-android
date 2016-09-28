package com.planckmail.activities;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.adapters.RecycleTrackedListAdapter;
import com.planckmail.adapters.RecycleTrackingListAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.model.TrackedItem;
import com.planckmail.data.model.TrackingItem;
import com.planckmail.fragments.TrackedListFragment;
import com.planckmail.fragments.TrackedMessageFragment;
import com.planckmail.utils.BundleKeys;
import com.planckmail.views.DividerItemDecoration;
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

public class TrackedListActivity extends FragmentActivity {


    private TextView mTextViewSubject;
    private TextView mTextViewRecipients;
    private TextView mTextViewTime;

    private Button mBtnCall;
    private Button mBtnEmail;
    private Button mBtnViewMessage;

    private ViewPager mViewPager;
    private ViewPagerAdapter mViewPagerAdapter;
    public TrackingItem mTrackingItem;
    public String mAccessToken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracked_list);

        mTextViewSubject = (TextView)findViewById(R.id.tracked_tv_subject);
        mTextViewTime = (TextView)findViewById(R.id.tracked_tv_time);
        mTextViewRecipients = (TextView) findViewById(R.id.tracked_tv_recipients);

        mBtnEmail = (Button) findViewById(R.id.tracked_btn_email);
        mBtnEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrackedListActivity.this, ComposeActivity.class);
                intent.putExtra(BundleKeys.COMPOSE_TO, mTrackingItem.getTargetEmails());
                startActivity(intent);
            }
        });

        mBtnCall = (Button) findViewById(R.id.tracked_btn_call);
        mBtnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mBtnViewMessage = (Button) findViewById(R.id.tracked_btn_view_message);
        mBtnViewMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBtnViewMessage.getText().toString().equalsIgnoreCase("View Message")) {
                    mViewPager.setCurrentItem(1);
                    mBtnViewMessage.setText("View List");
                } else {
                    mViewPager.setCurrentItem(0);
                    mBtnViewMessage.setText("View Message");
                }
            }
        });

        mViewPager = (ViewPager) findViewById(R.id.tracked_viewpager);
        mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        Intent intent = getIntent();

        mTrackingItem = (TrackingItem) intent.getExtras().getSerializable("trackingItem");
        mAccessToken = intent.getStringExtra("accessToken");

        mViewPagerAdapter.addFragment(new TrackedListFragment());
        mViewPagerAdapter.addFragment(new TrackedMessageFragment());

        mViewPager.setAdapter(mViewPagerAdapter);

        mTextViewSubject.setText(mTrackingItem.getSubject());
        mTextViewTime.setText(mTrackingItem.getCreatedTime().toLocaleString());
        mTextViewRecipients.setText(mTrackingItem.getTargetEmails());
    }


    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment) {
            mFragmentList.add(fragment);
        }


        public Fragment getFragmentByPosition(int position) {
            return mFragmentList.get(position);
        }
    }
}
