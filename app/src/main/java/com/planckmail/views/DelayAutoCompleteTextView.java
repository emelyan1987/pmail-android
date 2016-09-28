package com.planckmail.views;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;

/**
 * Created by Taras Matolinets on 25.05.15.
 */
public class DelayAutoCompleteTextView extends MultiAutoCompleteTextView {
    private static final int MESSAGE_TEXT_CHANGED = 100;
    private static final int DEFAULT_AUTOCOMPLETE_DELAY = 350;
    private final Context mContext;

    private int mAutoCompleteDelay = DEFAULT_AUTOCOMPLETE_DELAY;
    private ProgressBar mLoadingIndicator;

    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            DelayAutoCompleteTextView.super.performFiltering((CharSequence) msg.obj, msg.arg1);
            return true;
        }
    });

    public DelayAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setTokenizer(new CustomCommaTokenizer());
    }

    public void setLoadingIndicator(ProgressBar progressBar) {
        mLoadingIndicator = progressBar;
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        if (mLoadingIndicator != null) {
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        mHandler.removeMessages(MESSAGE_TEXT_CHANGED);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_TEXT_CHANGED, text), mAutoCompleteDelay);
    }

    private class CustomCommaTokenizer extends MultiAutoCompleteTextView.CommaTokenizer {
        @Override
        public CharSequence terminateToken(@NonNull CharSequence text) {

            CharSequence charSequence = super.terminateToken(text);
            return charSequence.subSequence(0, charSequence.length() - 1);
        }
    }
}
