package com.light.mbt.delight.ListAdapters;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.light.mbt.delight.CommonUtils.Utils;
import com.light.mbt.delight.ControlPageActivity;
import com.light.mbt.delight.widget.TosGallery;
import com.light.mbt.delight.widget.WheelTextView;
import com.light.mbt.delight.widget.WheelView;

import static com.light.mbt.delight.ControlPageActivity.mTimerStart;

/**
 * Created by RED on 2017/5/26.
 */

public class NumberAdapter extends BaseAdapter{
    private final static String TAG = " Delight / " + ControlPageActivity.class.getSimpleName();
    private static Context mContext;

    int mHeight = 45;
    String[] mData = null;

    /**
     * 默認字體顏色
     */
    public static int normalColor = 0xffffffff;
    /**
     * 選中時候的字體顏色
     */
    public static int selectedColor = 0xff4CAF50;
    /**
     * Disable時候的字體顏色
     */
    public static int disableColor = 0x5A000000;

    public NumberAdapter(Context context, String[] data) {
        mContext = context;
        mHeight = (int) Utils.dipToPx(context, mHeight);
        this.mData = data;
    }

    @Override
    public int getCount() {
        return (null != mData) ? mData.length : 0;
    }

    @Override
    public View getItem(int arg0) {
        return getView(arg0, null, null);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        WheelTextView textView = null;
        boolean isEnabled = ((WheelView) parent).isEnable();
        //Logger.i(TAG, "position2 = " + position);

        if (null == convertView) {
            convertView = new WheelTextView(mContext);
            convertView.setLayoutParams(new TosGallery.LayoutParams(-1, mHeight));
            textView = (WheelTextView) convertView;

            if (isEnabled == true || mTimerStart == true)
                textView.setTextColor(normalColor);
            else
                textView.setTextColor(disableColor);

            if (position == 0) {        //修正分鐘為0 時字型大小錯誤
                //Logger.i(TAG, "getSelectedItemPosition = " + ((WheelView) parent).getSelectedItemPosition());
                if (isEnabled == true || mTimerStart == true)
                    textView.setTextColor(selectedColor);
            }
                textView.setTextSize(25);
                textView.setGravity(Gravity.CENTER);
        }

        String text = mData[position];
        if (null == textView) {
            textView = (WheelTextView) convertView;
        }

        textView.setText(text);
        return convertView;
    }
}
