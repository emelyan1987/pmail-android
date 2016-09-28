package com.planckmail.views;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

/**
 * Created by Taras Matolinets on 27.09.15.
 */
public class FooterExpandableListView extends ExpandableListView {

    public FooterExpandableListView(Context context) {
        super(context);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, MeasureSpec.AT_MOST);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
