package com.planckmail.listeners;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.planckmail.application.PlanckMailApplication;

/**
 * Created by Taras Matolinets on 16.05.15.
 */
public abstract class EndlessRecyclerOnScrollListener extends RecyclerView.OnScrollListener {
    public static String TAG = EndlessRecyclerOnScrollListener.class.getSimpleName();

    private int previousTotal = 0; // The total number of items in the dataset after the last load
    private boolean loading = true; // True if we are still waiting for the last set of data to load.
    private int firstVisibleItem;

    private int mScrolledDistance = 0;
    private static final float HIDE_THRESHOLD = 10;

    private boolean mControlsVisible = true;
    private int current_page = 1;

    private LinearLayoutManager mLinearLayoutManager;

    public EndlessRecyclerOnScrollListener(LinearLayoutManager linearLayoutManager) {
        this.mLinearLayoutManager = linearLayoutManager;
    }

    public void setLayout(LinearLayoutManager linearLayoutManager) {
        this.mLinearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int visibleItemCount = recyclerView.getChildCount();
        int totalItemCount = mLinearLayoutManager.getItemCount();
        firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition();

        if (loading) {
            if (totalItemCount > previousTotal) {
                loading = false;
                previousTotal = totalItemCount;
            }
        }
        hideShowToolBar(dy);

        int visibleThreshold = 1;
        if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
            // End has been reached
            // Do something
            current_page++;
            onLoadMore(current_page);
            loading = true;
        }
    }

    public void hideShowToolBar(int dy) {
        if (firstVisibleItem == 0) {
            if (!mControlsVisible) {
                onShow();
                mControlsVisible = true;
            }
        } else {
            if (mScrolledDistance > HIDE_THRESHOLD && mControlsVisible) {
                onHide();
                mControlsVisible = false;
                mScrolledDistance = 0;
            } else if (mScrolledDistance < -HIDE_THRESHOLD && !mControlsVisible) {
                onShow();
                mControlsVisible = true;
                mScrolledDistance = 0;
            }
        }
        if ((mControlsVisible && dy > 0) || (!mControlsVisible && dy < 0)) {
            mScrolledDistance += dy;
        }
    }

    public void reset(int previousTotal, boolean loading) {
        this.previousTotal = previousTotal;
        this.loading = loading;
    }

    public abstract void onLoadMore(int current_page);

    public abstract void onHide();

    public abstract void onShow();
}

