package com.light.mbt.delight;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.light.mbt.delight.CommonUtils.Logger;

public class AboutPageActivity extends AppCompatActivity {
    private final static String TAG = " Delight / " + AboutPageActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  //增加左上角返回圖示

        TextView delight_version = (TextView) findViewById(R.id.delight_version);

        PackageManager manager = this.getPackageManager();

        try {
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            String appVersion = info.versionName; //版本名
            int appVersionCode = info.versionCode; //版本名
            Log.i(TAG, appVersion + "_" + appVersionCode);
            delight_version.setText(getResources().getString(R.string.app_name) + " " + appVersion);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        getSupportActionBar().setTitle(getResources().getString(R.string.title_about));  //設定標題
    }

    @Override
    public void onResume() {
        super.onResume();
        getSupportActionBar().setTitle(getResources().getString(R.string.title_about));  //設定標題
    }

    /**
     * Handling the back pressed actions
     */
    @Override
    public void onBackPressed() {
        Logger.i(TAG, "About onBackPressed");

        Intent intent = new Intent(this, UseDevicePageActivity.class);
        finish();
        overridePendingTransition(R.anim.slide_right, R.anim.push_right);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left, R.anim.push_left);

        super.onBackPressed();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:

                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
