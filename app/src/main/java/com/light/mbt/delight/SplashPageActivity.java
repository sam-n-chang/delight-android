/*
 * Copyright Cypress Semiconductor Corporation, 2014-2015 All rights reserved.
 * 
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign), United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate license agreement between you and Cypress, this Software
 * must be treated like any other copyrighted material. Reproduction,
 * modification, translation, compilation, or representation of this
 * Software in any other form (e.g., paper, magnetic, optical, silicon)
 * is prohibited without Cypress's express written permission.
 * 
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes
 * to the Software without notice. Cypress does not assume any liability
 * arising out of the application or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or application assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 * 
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 * 
 * 
 */

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
                        UseDevicePageActivity.class);
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
            gotoUseDeviceActivity();
        }
    }

    private void gotoUseDeviceActivity(){
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
                    gotoUseDeviceActivity();
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
