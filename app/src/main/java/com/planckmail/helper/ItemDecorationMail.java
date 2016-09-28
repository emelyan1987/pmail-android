package com.planckmail.helper;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by Taras Matolinets on 23.08.15.
 */
public class ItemDecorationMail extends RecyclerView.ItemDecoration {
    private int space;

    public ItemDecorationMail(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if(parent.getChildPosition(view) == 0)
            outRect.top = space;
    }
}