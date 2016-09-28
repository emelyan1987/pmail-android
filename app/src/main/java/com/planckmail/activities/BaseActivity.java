package com.planckmail.activities;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.planckmail.R;

/**
 * Created by Taras Matolinets on 01.05.15.
 */
public class BaseActivity extends AppCompatActivity {
    protected String mCurrentFragmentClassName;

    public Fragment getCurrentFragment(int fragment_hold) {
        return getFragmentManager().findFragmentById(fragment_hold);
    }

    /**
     * Replace in activity current fragment by another.
     *
     * @param classInfo class info of new fragment
     */
    public void replace(final Class<? extends Fragment> classInfo) {
        replace(classInfo, null);
    }

    /**
     * Replace in activity current fragment by another.
     *
     * @param classInfo class info of new fragment.
     * @param args      arguments for newly created fragment.
     */
    public void replace(final Class<? extends Fragment> classInfo, final Bundle args) {
        replace(classInfo, 0, args, false);
    }

    /**
     * Replace in activity current fragment by another.
     *
     * @param classInfo class info of new fragment.
     * @param useStack  true - add to "back Stack", otherwise false.
     */
    public void replace(final Class<? extends Fragment> classInfo, final boolean useStack) {
        replace(classInfo, 0, null, useStack);
    }

    /**
     * Replace in activity current fragment by another.
     *
     * @param classInfo     class info of new fragment.
     * @param args          arguments for newly created fragment.
     * @param useStack      true - add to "back Stack", otherwise false.
     * @param fragmentPlace - current fragment layout id
     */
    public void replace(Class<? extends Fragment> classInfo, int fragmentPlace, Bundle args, boolean useStack) {
        if (null != classInfo) {
            final Fragment frgCurrent = getCurrentFragment(fragmentPlace);
            final String classCurr = (null == frgCurrent) ? null : frgCurrent.getClass().getName();
            final String className = classInfo.getName();

            //  if (!className.equals(mCurrentFragmentClassName) || !className.equals(classCurr)) {
            final Fragment fragment = Fragment.instantiate(this, classInfo.getName());

            replace(fragment, args, useStack, fragmentPlace);
            //   }
        }
    }

    /**
     * Replace in activity current fragment by another.
     *
     * @param fragment the object that extends the class Fragment
     */
    public void replace(final Fragment fragment, int fragmentPlace) {
        replace(fragment, null, fragmentPlace);

    }

    /**
     * Replace in activity current fragment by another.
     *
     * @param fragment      the object that extends the class Fragment.
     * @param args          arguments for fragment.
     * @param fragmentPlace - current fragment layout id
     */
    public void replace(final Fragment fragment, final Bundle args, int fragmentPlace) {
        replace(fragment, args, false, fragmentPlace);
    }

    /**
     * Replace in activity current fragment by another.
     *
     * @param fragment      the object that extends the class Fragment.
     * @param args          arguments for fragment.
     * @param useStack      true - add to "back Stack", otherwise false.
     * @param fragmentPlace - current fragment layout id
     */
    public void replace(final Fragment fragment, final Bundle args, final boolean useStack, int fragmentPlace) {
        if (null != fragment && !isFinishing()) {
            Fragment frag = getCurrentFragment(fragmentPlace);
            final String classCurr = (null == frag) ? null : frag.getClass().getName();
            final String className = fragment.getClass().getCanonicalName();

            //           if (!className.equals(mCurrentFragmentClassName) || !className.equals(classCurr)) {
            // replace fragment now
            final FragmentManager fm = getFragmentManager();
            final FragmentTransaction ft = fm.beginTransaction();
            ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
            // update activity set fragment
            mCurrentFragmentClassName = className;

            // specify replace parameters for fragment
            if (null != args)
                fragment.setArguments(args);
            ft.replace(fragmentPlace, fragment, mCurrentFragmentClassName);

            if (useStack)
                ft.addToBackStack(className);

            ft.commit();
        }
        //  }
    }

    public interface IntentResultListener {
        /**
         * @see android.app.Activity#onActivityResult
         */
        public void onActivityResult(final int requestCode, final int resultCode, final Intent data);
    }
}
