package com.planckmail.fragments;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.utils.BundleKeys;

import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/**
 * Created by Terry on 1/2/2016.
 */
public class TutorialFragment extends BaseFragment {

    public static TutorialFragment getInstance(Bundle bundle) {
        TutorialFragment fragmentTutorial = new TutorialFragment();
        fragmentTutorial.setArguments(bundle);
        return fragmentTutorial;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutorial, container, false);

        intViews(view);
        return view;
    }

    private void intViews(View view) {
        TextView tvTitle = (TextView) view.findViewById(R.id.tvTutorialTitle);
        TextView tvSubTitle = (TextView) view.findViewById(R.id.tvTutorialSubTitle);
        GifImageView tvDescription = (GifImageView) view.findViewById(R.id.tvTutorialDescription);
        int position = getArguments().getInt(BundleKeys.TUTORIAL_POSITION);
        TypedArray gifArray = getResources().obtainTypedArray(R.array.array_tutorialGifDescription);

        try {
            GifDrawable gifFromResource = new GifDrawable(getResources(), gifArray.getResourceId(position, -1));
            tvDescription.setImageDrawable(gifFromResource);
            gifArray.recycle();
        } catch (IOException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }

        String title = getArguments().getString(BundleKeys.TUTORIAL_TITLE);
        String subtitle = getArguments().getString(BundleKeys.TUTORIAL_SUB_TITLE);

        tvTitle.setText(title);
        tvSubTitle.setText(subtitle);

    }
}
