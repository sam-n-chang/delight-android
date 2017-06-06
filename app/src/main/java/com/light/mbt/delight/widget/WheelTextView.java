
package com.light.mbt.delight.widget;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

public class WheelTextView extends TextView {
    public static String[] hoursArray = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10",
            "11", "12"};
    public static  String[] minsecsArray = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23",
            "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37",
            "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50",
            "51", "52", "53", "54", "55", "56", "57", "58", "59"};

    public WheelTextView(Context context) {
        super(context);
    }

    public WheelTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setTextSize(float size) {
        Context c = getContext();
        Resources r;

        if (c == null)
            r = Resources.getSystem();
        else
            r = c.getResources();
        float rawSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, size, r.getDisplayMetrics());
        if (rawSize != getPaint().getTextSize()) {
            getPaint().setTextSize(rawSize);

            if (getLayout() != null) {
                invalidate();
            }
        }

    }

}
