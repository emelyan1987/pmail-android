package com.planckmail.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.planckmail.R;
import com.planckmail.adapters.SearchPlaceAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.dao.AutoCompletePlace;
import com.planckmail.listeners.RecyclerItemClickListener;
import com.planckmail.utils.BundleKeys;
import com.planckmail.views.DelayAutoCompleteTextView;

import java.util.ArrayList;

/**
 * Created by Terry on 12/6/2015.
 */
public class FindLocationActivity extends BaseActivity implements SearchView.OnQueryTextListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks,
        SearchPlaceAdapter.OnGetLocationResult {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int MESSAGE_TEXT_CHANGED = 100;
    private static final long mAutoCompleteDelay = 1000;

    private RecyclerView mRecycleSelectLocation;
    private SearchPlaceAdapter mAdapter;
    private GoogleApiClient mGoogleApiClient;
    private TextView mTextNoResults;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_location);
        registerGoogleApiClient();
        initViews();
        setListeners();
        fillViews();
    }

    Handler mIncomingHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String text = (String) msg.obj;
            mAdapter.getFilter().filter(text);
            return false;
        }
    });


    private void registerGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .enableAutoManage(this, 0, this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Now lets connect to the API
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Disconnect from API onPause()
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private void fillViews() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        mRecycleSelectLocation.setHasFixedSize(true);
        mRecycleSelectLocation.setLayoutManager(layoutManager);
        mAdapter = new SearchPlaceAdapter(this, mGoogleApiClient, new ArrayList<AutoCompletePlace>());
        mAdapter.setListener(this);
        mRecycleSelectLocation.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_location, menu);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(this);
        return true;
    }

    private void initViews() {
        mRecycleSelectLocation = (RecyclerView) findViewById(R.id.recycleFindLocation);
        mTextNoResults = (TextView) findViewById(R.id.tvNoSearchResult);
        mProgressBar = (ProgressBar) findViewById(R.id.prLoading);
    }

    private void setListeners() {
        mRecycleSelectLocation.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        String location = (String) view.getTag(R.string.tag_location);
                        Intent intent = new Intent();
                        intent.putExtra(BundleKeys.LOCATION, location);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                })
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case android.R.id.closeButton:
                mAdapter.clear();
                mAdapter.notifyDataSetChanged();
                mProgressBar.setVisibility(View.GONE);
                mTextNoResults.setVisibility(View.VISIBLE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            mProgressBar.setVisibility(View.GONE);
            mTextNoResults.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.VISIBLE);
            mTextNoResults.setVisibility(View.GONE);
        }
        mIncomingHandler.removeMessages(MESSAGE_TEXT_CHANGED);
        mIncomingHandler.sendMessageDelayed(mIncomingHandler.obtainMessage(MESSAGE_TEXT_CHANGED, newText ), mAutoCompleteDelay);

        return false;
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                Log.e(PlanckMailApplication.TAG, e.getMessage());
            }
        } else {
            Log.e(PlanckMailApplication.TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void getResultLocation(boolean isDataEmpty) {
        mProgressBar.setVisibility(View.GONE);

        if (isDataEmpty)
            mTextNoResults.setVisibility(View.VISIBLE);
        else
            mTextNoResults.setVisibility(View.GONE);
    }
}
