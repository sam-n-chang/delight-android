package com.light.mbt.delight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;

import com.light.mbt.delight.CommonUtils.Logger;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;


/**
 * Activity to display the initial splash screen
 */

public class SplashPageActivity extends Activity {
    private final static String TAG = " Delight / " + SplashPageActivity.class.getSimpleName();
    private static final int REQUEST_FINE_LOCATION = 0;
    /**
     * Page display time
     */
    private final int SPLASH_DISPLAY_LENGTH = 3000;
    /**
     * Flag to handle the handler
     */
    private boolean mHandlerFlag = true;

    private Handler mHandler = new Handler();
    private Runnable mRun = new Runnable() {

        @Override
        public void run() {

            if (mHandlerFlag) {
                // Finish the current Activity and start HomePage Activity
                Intent home = new Intent(SplashPageActivity.this,
                        ScanPageActivity.class);
                startActivity(home);
                SplashPageActivity.this.finish();
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Logger.disableLog();

        if (isTablet(this)) {
            Logger.d(TAG, "Tablet");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            Logger.d(TAG, "Phone");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_splash);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        //Android 5.0 需要取得定位權限
        int permission = ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // 無權限，向使用者請求
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {ACCESS_FINE_LOCATION,
                            ACCESS_COARSE_LOCATION},
                    REQUEST_FINE_LOCATION
            );
        }else{
            //已有權限，進入ScanPageActivity
            gotoScanPageActivity();
        }
    }

    private void gotoScanPageActivity(){
        /**
         * Run the code inside the runnable after the display time is finished
         * using a handler
         */
        mHandler.postDelayed(mRun, SPLASH_DISPLAY_LENGTH);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case REQUEST_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //取得權限，進入ScanPageActivity
                    gotoScanPageActivity();
                } else {
                    //使用者拒絕權限，退出程式
                    finish();
                }
            return;
        }
    }

    @Override
    public void onBackPressed() {
        /**
         * Disable the handler to execute when user presses back when in
         * SplashPage Activity
         */
        mHandlerFlag = false;
        super.onBackPressed();
    }

    /**
     * Method to detect whether the device is phone or tablet
     */
    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

}
