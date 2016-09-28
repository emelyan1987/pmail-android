package com.planckmail.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class MainTextView extends TextView {

    public static final String ANDROID_SCHEMA = "http://schemas.android.com/apk/res/android";

    public MainTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        applyCustomFont(context, attrs);
    }

    public MainTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyCustomFont(context, attrs);
    }

    private void applyCustomFont(Context context, AttributeSet attrs) {
        int textStyle = attrs.getAttributeIntValue(ANDROID_SCHEMA, "textStyle", Typeface.NORMAL);

        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/Helvetica.ttf");
        setTypeface(typeface, textStyle);
    }
}