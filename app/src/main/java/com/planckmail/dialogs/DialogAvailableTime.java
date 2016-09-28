package com.planckmail.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.planckmail.R;
import com.planckmail.adapters.SelectAvailableTimeAdapter;
import com.planckmail.listeners.RecyclerItemClickListener;
import com.planckmail.utils.BundleKeys;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Terry on 12/6/2015.
 */
public class DialogAvailableTime extends DialogFragment {

    private RecyclerView mRecycleSelectTime;
    private SelectAvailableTimeAdapter mAdapter;
    private ISelectTime mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.selectTime));

        View view = inflater.inflate(R.layout.dialog_select_time_fragment, container, false);
        initViews(view);
        setListeners();
        setDialogAdapter();

        return view;
    }

    public void setListener(ISelectTime listener) {
        mListener = listener;
    }

    private void setDialogAdapter() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecycleSelectTime.setHasFixedSize(true);
        mRecycleSelectTime.setLayoutManager(layoutManager);

        String[] array = getResources().getStringArray(R.array.arraySelectTime);
        ArrayList<String> list = new ArrayList<>(Arrays.asList(array));
        int position = getArguments().getInt(BundleKeys.POSITION);

        mAdapter = new SelectAvailableTimeAdapter(getActivity(), position,list);
        mRecycleSelectTime.setAdapter(mAdapter);
    }

    private void initViews(View view) {
        mRecycleSelectTime = (RecyclerView) view.findViewById(R.id.recycleSelectTime);
    }

    private void setListeners() {
        mRecycleSelectTime.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        String[] array = getResources().getStringArray(R.array.arraySelectTime);

                        String time = array[position];
                        int[] arrayTime = getResources().getIntArray(R.array.arraySelectTimeSeconds);
                        int seconds = arrayTime[position];

                        if (mListener != null)
                            mListener.selectTime(time, position,seconds);

                        dismiss();
                    }
                })
        );
    }

    public interface ISelectTime {
        void selectTime(String time, int position,int seconds);
    }
}
