package com.planckmail.fragments;


import android.app.Fragment;

/**
 * Created by Taras Matolinets on 07.05.15.
 */
public class BaseFragment extends Fragment {

    public interface OnBackPressed {
        public void onBackPress();
    }
}
