package com.planckmail.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.planckmail.R;
import com.planckmail.adapters.RecycleThreadAdapter.SWIPE_TYPE;
import com.planckmail.fragments.BaseFragment;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Thread;
import com.planckmail.web.response.nylas.wrapper.Participant;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Terry on 11/18/2015.
 */
public class SwipeConfirmationDialog extends DialogFragment implements View.OnClickListener, BaseFragment.OnBackPressed {

    private Button mButtonConfirm;
    private Button mButtonCancel;
    private SwipeLayout mSwipeLayout;
    private TextView mTvFrom;
    private TextView mTvSubject;
    private TextView mTvTime;
    private OnConfirmButtonClick mListener;
    private CheckBox mCheckResponse;
    private CheckBox mCheckRemind;
    private TextView mTvDialogType;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_swipe_confirmation, container, false);
        intViews(view);
        setListeners();
        fillView();
        return view;
    }

    private void fillView() {
        String threadJson = getArguments().getString(BundleKeys.THREAD);
        long time = getArguments().getLong(BundleKeys.TIME);

        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("MM/dd/y HH:mm a", Locale.getDefault());
        String dateFormatted = formatter.format(date);
        mTvTime.setText(dateFormatted);

        boolean isNotifyMe = getArguments().getBoolean(BundleKeys.IS_NOTIFY);

        if (isNotifyMe) {
            mCheckRemind.setVisibility(View.VISIBLE);
            mCheckResponse.setVisibility(View.VISIBLE);
        }

        SWIPE_TYPE type = (SWIPE_TYPE) getArguments().getSerializable(BundleKeys.SWIPE_TYPE);

        if (type == SWIPE_TYPE.NOTIFY) {
            mTvDialogType.setText(getString(R.string.notifyConfirmation));
        } else {
            mTvDialogType.setText(getString(R.string.snoozeConfirmation));
        }

        Thread thread = JsonUtilFactory.getJsonUtil().fromJson(threadJson, Thread.class);
        if (thread != null) {
            Participant participant = thread.getParticipants().get(0);
            Spanned from = Html.fromHtml(getString(R.string.from, participant.email));
            Spanned subject = Html.fromHtml(getString(R.string.subject, thread.subject));
            mTvFrom.setText(from);
            mTvSubject.setText(subject);
        }
    }

    public void setSwipeLayout(SwipeLayout swipeLayout) {
        mSwipeLayout = swipeLayout;
    }

    public void setListener(OnConfirmButtonClick listener) {
        mListener = listener;
    }

    private void setListeners() {
        mButtonCancel.setOnClickListener(this);
        mButtonConfirm.setOnClickListener(this);
    }

    private void intViews(View view) {
        mButtonConfirm = (Button) view.findViewById(R.id.btConfirm);
        mButtonCancel = (Button) view.findViewById(R.id.btCancel);
        mTvFrom = (TextView) view.findViewById(R.id.tvFrom);
        mTvSubject = (TextView) view.findViewById(R.id.tvSubject);
        mTvDialogType = (TextView) view.findViewById(R.id.tvDialogType);
        mTvTime = (TextView) view.findViewById(R.id.tvDate);
        mCheckResponse = (CheckBox) view.findViewById(R.id.cbResponse);
        mCheckRemind = (CheckBox) view.findViewById(R.id.cbRemind);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btConfirm:
                String threadJson = getArguments().getString(BundleKeys.THREAD);
                int position = getArguments().getInt(BundleKeys.POSITION);
                Thread thread = JsonUtilFactory.getJsonUtil().fromJson(threadJson, Thread.class);
                boolean isMessage = getArguments().getBoolean(BundleKeys.IS_MESSAGE);

                long time = getArguments().getLong(BundleKeys.TIME);
                SWIPE_TYPE type = (SWIPE_TYPE) getArguments().getSerializable(BundleKeys.SWIPE_TYPE);

                if (mListener != null && !isMessage)
                    mListener.confirmSwipeActionClick(type, mSwipeLayout, thread, time, position, mCheckRemind.isChecked());
                else if (mListener != null) {
                    mListener.confirmSwipeSnoozeClick(thread, time);
                }
                break;
        }
        dismiss();
    }

    @Override
    public void onBackPress() {
        getFragmentManager().popBackStackImmediate();
    }

    public interface OnConfirmButtonClick {
        void confirmSwipeActionClick(SWIPE_TYPE type, SwipeLayout swipe, Thread thread, long millis, int position, boolean remindState);

        void confirmSwipeSnoozeClick(Thread thread, long millis);
    }
}
