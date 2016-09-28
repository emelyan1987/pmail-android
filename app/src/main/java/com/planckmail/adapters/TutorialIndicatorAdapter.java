package com.planckmail.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import com.planckmail.R;
import com.planckmail.fragments.TutorialFragment;
import com.planckmail.utils.BundleKeys;

/**
 * Created by Terry on 1/2/2016.
 */
public class TutorialIndicatorAdapter extends FragmentPagerAdapter{

    private String[] mTitleArray;
    private String[] mSubTitleArray;

    public TutorialIndicatorAdapter(Context context, FragmentManager fm) {
        super(fm);
        mTitleArray = context.getResources().getStringArray(R.array.array_tutorialTitle);
        mSubTitleArray = context.getResources().getStringArray(R.array.array_tutorialSubTitle);
    }

    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        bundle.putString(BundleKeys.TUTORIAL_TITLE, mTitleArray[position]);
        bundle.putString(BundleKeys.TUTORIAL_SUB_TITLE, mSubTitleArray[position]);
        bundle.putInt(BundleKeys.TUTORIAL_POSITION, position);

        return TutorialFragment.getInstance(bundle);
    }

    @Override
    public int getCount() {
        return mTitleArray.length;
    }

}
