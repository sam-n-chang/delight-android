package com.light.mbt.delight.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.TextView;

/**
 * Created by RED on 2017/6/26.
 */

public class EditTextView  extends AppCompatEditText {
    public EditTextView (Context context) {
        super(context);
    }

    public EditTextView (Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextView (Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public interface BackListener {
        void back(TextView textView);
    }

    private BackListener listener;

    public void setBackListener(BackListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (listener != null) {
                listener.back(this);
            }
        }
        return false;
    }
}
