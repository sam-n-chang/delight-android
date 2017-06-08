package com.light.mbt.delight;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.light.mbt.delight.CommonUtils.CountDownTimerUtil;
import com.light.mbt.delight.CommonUtils.DeviceInformationService;
import com.light.mbt.delight.CommonUtils.GattAttributes;
import com.light.mbt.delight.CommonUtils.Logger;
import com.light.mbt.delight.CommonUtils.Utils;
import com.light.mbt.delight.ListAdapters.DeviceList;
import com.light.mbt.delight.ListAdapters.NumberAdapter;
import com.light.mbt.delight.bluetooth.BluetoothLeService;
import com.light.mbt.delight.widget.TosAdapterView;
import com.light.mbt.delight.widget.WheelTextView;
import com.light.mbt.delight.widget.WheelView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.light.mbt.delight.ListAdapters.NumberAdapter.normalColor;
import static com.light.mbt.delight.ListAdapters.NumberAdapter.selectedColor;
import static com.light.mbt.delight.R.id.seekBar;
import static com.light.mbt.delight.UseDevicePageActivity.mLeDeviceListAdapter;
import static com.light.mbt.delight.widget.WheelTextView.hoursArray;
import static com.light.mbt.delight.widget.WheelTextView.minsecsArray;

public class ControlPageActivity extends AppCompatActivity {
    private final static String TAG = " Delight / " + ControlPageActivity.class.getSimpleName();

    private NumberAdapter hourAdapter, minAdapter, secAdapter;
    private WheelView mHours = null, mMins = null, mSecs = null;
    private View mDecorView = null;
    private SeekBar mSeekBar = null;
    private ImageView mImageView1 = null, mImageView2 = null;
    private ImageButton powerButton, timerButton, playButton, stopButton;
    private AlertDialog mAlert;

    // Application
    private ProgressDialog mProgressDialog;
    private Timer mTimer;
    private TextView mNoserviceDiscovered;

    private static final long DELAY_PERIOD = 500;
    private static final long SERVICE_DISCOVERY_TIMEOUT = 10000;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName, mDeviceAddress;

    // GATT service and characteristics
    private static BluetoothGattCharacteristic mReadCharacteristic = null;
    private BluetoothGattService mDeviceInformationService = null;

    public static boolean mTimerStart = false;
    private boolean mPowerStatus = false, mTimerStatus = false, mWheelViewStatus = false,
            mConnected = false, FirstLaunch = false, initWheel = false,
            mOrientation = false, mReturnBack = false;
    private byte mPower = 0, mLAMP_Timer = 0, mTimeOne = 0, mTimeTwo = 0, mINTENSITY = -1;
    private long SaveTime = 0, timer_unit = 1000, countDownTimer = 0;
    private boolean BLUETOOTH_STATUS_FLAG = true;
    private boolean BLUETOOTH_STATUS_FLAG2 = false;

    private DeviceList mDeviceList = null;
    private int countDownTimerStatus = CountDownTimerUtil.PREPARE;
    private Handler handler;

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                Logger.i(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED.");
                BLUETOOTH_STATUS_FLAG2 = true;
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) ==
                        BluetoothAdapter.STATE_OFF) {
                    Logger.i(TAG, "BluetoothAdapter.STATE_OFF");
                    if (BLUETOOTH_STATUS_FLAG) {
                        connectionLostBluetoothalertbox(true);
                    }

                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) ==
                        BluetoothAdapter.STATE_ON) {
                    Logger.i(TAG, "BluetoothAdapter.STATE_ON");
                    if (BLUETOOTH_STATUS_FLAG) {
                        connectionLostBluetoothalertbox(false);
                        if (BluetoothLeService.getConnectionState() == 0)
                            BluetoothLeService.connect(mDeviceAddress, mDeviceName, ControlPageActivity.this);
                    }
                }
            } else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(true);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)
                    || BluetoothLeService.ACL_DISCONNECTED.equals(action)) {
                Logger.i(TAG, "Service DISCONNECTED");
                if (mConnected == true)
                    PowerOff();

                mConnected = false;
                updateConnectionState(false);
                invalidateOptionsMenu();
                if (BLUETOOTH_STATUS_FLAG2 == false)
                    gotoUseDeviceActivity();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Logger.i(TAG, "Service discovered");
                if (mTimer != null)
                    mTimer.cancel();

                // Show all the supported services and characteristics on the user interface.
                getGattServices(BluetoothLeService.getSupportedGattServices());
                stopBroadcastDataNotify(mReadCharacteristic);
                prepareBroadcastDataRead(mReadCharacteristic);
                prepareBroadcastDataNotify(mReadCharacteristic);

                /*
                                / Changes the MTU size to 512 in case LOLLIPOP and above devices
                                */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    BluetoothLeService.exchangeGattMtu(512);
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));  //藍芽回傳顯示
            } else if (BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL
                    .equals(action)) {
                mProgressDialog.dismiss();
                if (mTimer != null)
                    mTimer.cancel();
                showNoServiceDiscoverAlert();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.i(TAG, "Control onCreate");
         setContentView(R.layout.content_control_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  //增加左上角返回圖示

         mAlert = new AlertDialog.Builder(this).create();
        mAlert.setMessage(getResources().getString(
                R.string.alert_message_bluetooth_reconnect));
        mAlert.setCancelable(false);
        mAlert.setTitle(getResources().getString(R.string.app_name));
        mAlert.setButton(Dialog.BUTTON_POSITIVE, getResources().getString(
                R.string.alert_message_exit_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intentActivity = new Intent(ControlPageActivity.this, UseDevicePageActivity.class);
                finish();
                overridePendingTransition(
                        R.anim.slide_left, R.anim.push_left);
                startActivity(intentActivity);
                overridePendingTransition(
                        R.anim.slide_right, R.anim.push_right);
            }
        });
        mAlert.setCanceledOnTouchOutside(false);
        handler = new Handler();

        FirstLaunch = true;

        mDeviceList = (DeviceList) getIntent().getSerializableExtra("DeviceList");  //取得由UseDevicePageActivity 或ScanPageActivity 傳過來DeviceList
        getSupportActionBar().setTitle(mDeviceList.getName());  //設定ActionBar 標題

        initializeBluetooth();
        initializeControl();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // land do nothing is ok
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // port do nothing is ok
        }

        setContentView(R.layout.content_control_main);
        initializeControl();
        if (mPowerStatus == true) {
            mOrientation = true;
            PowerOn(true);
            mSeekBar.setProgress(mINTENSITY & 0xff);   //byte to int
            Logger.i(TAG, "mINTENSITY = " + String.valueOf(mINTENSITY & 0xff));

            if (mTimerStatus == true) {
                TimeOpen();

                if (mTimerStart == true)
                    formateTimer(countDownTimer);

                switch (countDownTimerStatus) {
                    case CountDownTimerUtil.PREPARE:
                        break;
                    case CountDownTimerUtil.START:
                        TimesWheelisEnable(false);
                        playButton.setImageDrawable(getResources().getDrawable(R.mipmap.pause_on));
                        stopButton.setImageDrawable(getResources().getDrawable(R.mipmap.stop_on));
                        break;
                    case CountDownTimerUtil.PASUSE:
                        TimesWheelisEnable(false);
                        playButton.setImageDrawable(getResources().getDrawable(R.mipmap.play_on));
                        stopButton.setImageDrawable(getResources().getDrawable(R.mipmap.stop_on));
                        break;
                }
            }
            mOrientation = false;
        }
    }

    @Override
    public void onResume() {
        //FirstLaunch = true;
        Logger.i(TAG, "Control onResume");
        if(mReadCharacteristic != null) {
            stopBroadcastDataNotify(mReadCharacteristic);
            prepareBroadcastDataRead(mReadCharacteristic);
            prepareBroadcastDataNotify(mReadCharacteristic);
            mReturnBack = true;
        }
        Logger.i(TAG, "BLE Connection State---->" + BluetoothLeService.getConnectionState());
        if (BluetoothLeService.getConnectionState() == 0)
            BluetoothLeService.connect(mDeviceAddress, mDeviceName, ControlPageActivity.this);

        BLUETOOTH_STATUS_FLAG = true;
        registerReceiver(mGattUpdateReceiver, Utils.makeGattUpdateIntentFilter());
        registerReceiver(DeviceInformationService.mGattUpdateReceiver, Utils.makeGattUpdateIntentFilter());
        super.onResume();
    }

    @Override
    protected void onPause() {
        Logger.i(TAG, "Control onPause");
        unregisterReceiver(mGattUpdateReceiver);
        unregisterReceiver(DeviceInformationService.mGattUpdateReceiver);

        if (mTimerStart == true)
            mTimerStart = false;

        //退出程式將Service 關閉
        Intent gattServiceIntent = new Intent(getApplicationContext(),
                BluetoothLeService.class);
        stopService(gattServiceIntent);
        Logger.d(TAG, "stopService");

        SaveDeviceData();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Logger.i(TAG, "Control onDestroy");
        SaveDeviceData();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Logger.i(TAG, "Control onBackPressed");
        SaveDeviceData();
        gotoUseDeviceActivity();

        super.onBackPressed();
    }

    private void gotoUseDeviceActivity() {
        Logger.i(TAG, "Control gotoUseDeviceActivity");
        Logger.i(TAG, "BLE Connection State---->" + BluetoothLeService.getConnectionState());
        if (BluetoothLeService.getConnectionState() == 2 ||
                BluetoothLeService.getConnectionState() == 1 ||
                BluetoothLeService.getConnectionState() == 4) {
            BluetoothLeService.disconnect();
            mReadCharacteristic = null;

            Toast.makeText(this,
                    getResources().getString(R.string.alert_message_bluetooth_disconnect),
                    Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(this, UseDevicePageActivity.class);
        finish();
        overridePendingTransition(R.anim.slide_left, R.anim.push_left);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_right, R.anim.push_right);
    }

    private void SaveDeviceData() {
        Utils.setDeviceListArraySharedPreference(ControlPageActivity.this, "DEVICE_LIST", mDeviceList, false);
        //更新UseDevicePageActivity.mLeDeviceListAdapter
        mLeDeviceListAdapter.mDeviceList.put(mDeviceList.getDeviceAddress(), mDeviceList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.home_main, menu); //menu Setting not use
        getMenuInflater().inflate(R.menu.home_main, menu);
        menu.findItem(R.id.action_info).setVisible(true);
        menu.findItem(R.id.action_edit).setVisible(false);
        menu.findItem(R.id.action_del).setVisible(false);
        menu.findItem(R.id.action_add).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_info:
                DeviceInformationDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void initializeControl() {
        mSeekBar = (SeekBar) findViewById(seekBar);
        mSeekBar.setEnabled(false);

        mImageView1 = (ImageView) findViewById(R.id.imageView4);
        mImageView1.setAlpha(0.3f);
        mImageView2 = (ImageView) findViewById(R.id.imageView5);
        mImageView2.setAlpha(0.3f);

        powerButton = (ImageButton) findViewById(R.id.PowerButton);
        timerButton = (ImageButton) findViewById(R.id.TimerButton);
        timerButton.setEnabled(false);
        timerButton.setAlpha(0.5f);

        playButton = (ImageButton) findViewById(R.id.PlayButton);
        playButton.setEnabled(false);
        playButton.setAlpha(0.5f);
        stopButton = (ImageButton) findViewById(R.id.StopButton);
        stopButton.setEnabled(false);
        stopButton.setAlpha(0.5f);

        initializeWheel(false);  //初始化
        SaveTime = getSaveTime();    //取得記錄時間
        if (SaveTime != 0) {
            setToBluetoothTime(SaveTime);
            formateTimer(SaveTime);    //將儲存的時間顯示在UI
        } else {
            mHours.setSelection(0, true);
            mMins.setSelection(30, true);
            mSecs.setSelection(0, true);
        }

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                mINTENSITY = (byte) progress;
                // Logger.d(TAG, "progress = " + String.valueOf(progress));
                //SendData();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                SendData();
            }
        });
    }

    private void initializeWheel(boolean isEnable) {
        initWheel = true;
        /***Wheel***/
        mHours = (WheelView) findViewById(R.id.wheel1);
        mMins = (WheelView) findViewById(R.id.wheel2);
        mSecs = (WheelView) findViewById(R.id.wheel3);

        mHours.setScrollCycle(true);
        mMins.setScrollCycle(true);
        mSecs.setScrollCycle(true);

        hourAdapter = new NumberAdapter(ControlPageActivity.this, hoursArray);
        minAdapter = new NumberAdapter(ControlPageActivity.this, minsecsArray);
        secAdapter = new NumberAdapter(ControlPageActivity.this, minsecsArray);

        mHours.setAdapter(hourAdapter);
        mMins.setAdapter(minAdapter);
        mSecs.setAdapter(secAdapter);

        mHours.setSpacing(-20);
        mMins.setSpacing(-20);
        mSecs.setSpacing(-20);

        TimesWheelisEnable(isEnable);

        mHours.setSelection(6, true);
        mMins.setSelection(30, true);
        mSecs.setSelection(30, true);

        ((WheelTextView) mHours.getSelectedView()).setTextSize(30);
        ((WheelTextView) mMins.getSelectedView()).setTextSize(30);
        ((WheelTextView) mSecs.getSelectedView()).setTextSize(30);

        mHours.setUnselectedAlpha(0.5f);
        mMins.setUnselectedAlpha(0.5f);
        mSecs.setUnselectedAlpha(0.5f);

        mHours.setOnItemSelectedListener(mListener);
        mMins.setOnItemSelectedListener(mListener);
        mSecs.setOnItemSelectedListener(mListener);

        //mDecorView = getWindow().getDecorView();
        initWheel = false;
    }

    void TimesWheelisEnable(boolean isEnable) {
        mHours.setEnable(isEnable);
        mMins.setEnable(isEnable);
        mSecs.setEnable(isEnable);
    }

    private TosAdapterView.OnItemSelectedListener mListener = new TosAdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
            boolean isEnabled = ((WheelView) parent).isEnable();
            //Logger.i(TAG, "position = " + position);

            ((WheelTextView) view).setTextSize(25);
            ((WheelTextView) view).setGravity(Gravity.CENTER);

            if (isEnabled == true || mTimerStart == true)
                ((WheelTextView) view).setTextColor(selectedColor);

            int index = Integer.parseInt(view.getTag().toString());
            int count = parent.getChildCount();

            if (index < count - 1) {
                ((WheelTextView) parent.getChildAt(index + 1)).setTextSize(25);    //下方字體大小
                if (isEnabled == true || mTimerStart == true)
                    ((WheelTextView) parent.getChildAt(index + 1)).setTextColor(normalColor);    //下方字體顏色
            }

            if (index > 0) {
                ((WheelTextView) parent.getChildAt(index - 1)).setTextSize(25);    //上方字體大小
                if (isEnabled == true || mTimerStart == true)
                    ((WheelTextView) parent.getChildAt(index - 1)).setTextColor(normalColor);    //上方字體顏色
            }

            if (mTimerStatus == true && mTimerStart == false && initWheel == false) {
                get_TotalTime();
            }
        }

        @Override
        public void onNothingSelected(TosAdapterView<?> parent) {
        }
    };

    public void onPower(View view) {
        if (mPowerStatus == false) {
            PowerOn(true);
            mPower = 0;
            SendData();
        } else {
            PowerOff();
        }
    }

    private void PowerOn(boolean isEnable) {
        if (mLAMP_Timer != 1 && mLAMP_Timer != 2 && isEnable == true && mOrientation == false) {
            mINTENSITY = (byte) 255;
            mSeekBar.setProgress(255);
        }

        mSeekBar.setEnabled(true);
        mImageView1.setAlpha(1f);
        mImageView2.setAlpha(1f);
        timerButton.setEnabled(true);
        timerButton.setAlpha(1f);

        mPowerStatus = true;
        powerButton.setImageDrawable(getResources().getDrawable(R.mipmap.power_on));
    }

    private void PowerOff() {
        PowerOffUIUpdate();
        SendData();
    }

    void PowerOffUIUpdate() {
        mINTENSITY = 0;
        mPower = 1;
        if (mTimerStart == true) //關閉電源時，如果計時器有動作則關閉
            mLAMP_Timer = 3;
        else
            mLAMP_Timer = 0;

        mPowerStatus = false;
        mSeekBar.setEnabled(false);
        mImageView1.setAlpha(0.5f);
        mImageView2.setAlpha(0.5f);
        timerButton.setEnabled(false);
        timerButton.setAlpha(0.5f);
        TimeClose();
        mSeekBar.setProgress(0);
        powerButton.setImageDrawable(getResources().getDrawable(R.mipmap.power_off));
    }

    public void onTimer(View view) {
        if (mTimerStatus == false) {
            TimeOpen();
        } else {
            TimeClose();
        }
    }

    private void TimeOpen() {
        mTimerStatus = true;
        mWheelViewStatus = true;

        playButton.setEnabled(true);
        playButton.setAlpha(1f);
        stopButton.setEnabled(true);
        stopButton.setAlpha(1f);

        initializeWheel(mWheelViewStatus);  //初始化
        formateTimer(SaveTime);    //將儲存的時間顯示在UI
        timerButton.setImageDrawable(getResources().getDrawable(R.mipmap.hourglass_on));
    }

    private void TimeClose() {
        if (mTimerStart == true) {
            mLAMP_Timer = 3;
            SendData();
        }

        mTimerStatus = false;
        mWheelViewStatus = false;
        mTimerStart = false;

        playButton.setEnabled(false);
        playButton.setAlpha(0.5f);
        stopButton.setEnabled(false);
        stopButton.setAlpha(0.5f);

        countDownTimerStatus = CountDownTimerUtil.PREPARE;
        initializeWheel(mWheelViewStatus);  //初始化
        formateTimer(SaveTime);    //將儲存的時間顯示在UI
        playButton.setImageDrawable(getResources().getDrawable(R.mipmap.play));
        stopButton.setImageDrawable(getResources().getDrawable(R.mipmap.stop));
        timerButton.setImageDrawable(getResources().getDrawable(R.mipmap.hourglass));
    }

    private long getSaveTime() {
        return mDeviceList.getTime();
    }

    private long get_TotalTime() {
        int pos1 = mHours.getSelectedItemPosition();
        int pos2 = mMins.getSelectedItemPosition();
        int pos3 = mSecs.getSelectedItemPosition();

        long All_Total_Times = ((pos1 * 3600) + (pos2 * 60) + pos3) * timer_unit;
        Logger.d(TAG, "All_Total_Times = " + All_Total_Times);

        if (All_Total_Times > 0) {
            SaveTime = All_Total_Times;
            mDeviceList.setTime(All_Total_Times);
            setToBluetoothTime(All_Total_Times);
        }
        return All_Total_Times;
    }

    void setToBluetoothTime(long Time) {
        byte[] valueByte = new byte[2];

        valueByte = Utils.hexlonggToByteArray(Time / timer_unit);
        mTimeOne = valueByte[0];
        mTimeTwo = valueByte[1];
        Logger.d(TAG, "valueByte = " + mTimeOne + "," + mTimeTwo);
    }

    public void StartCountDownTimer(View view) {

        switch (countDownTimerStatus) {
            case CountDownTimerUtil.PREPARE:
                TimesWheelisEnable(false);
                mTimerStart = true;
                playButton.setImageDrawable(getResources().getDrawable(R.mipmap.pause_on));
                stopButton.setImageDrawable(getResources().getDrawable(R.mipmap.stop_on));
                mLAMP_Timer = 1;
                countDownTimerStatus = CountDownTimerUtil.START;
                break;
            case CountDownTimerUtil.START:
                playButton.setImageDrawable(getResources().getDrawable(R.mipmap.play_on));
                mLAMP_Timer = 2;
                countDownTimerStatus = CountDownTimerUtil.PASUSE;
                break;
            case CountDownTimerUtil.PASUSE:
                playButton.setImageDrawable(getResources().getDrawable(R.mipmap.pause_on));
                mLAMP_Timer = 1;
                countDownTimerStatus = CountDownTimerUtil.START;
                break;
        }
        SendData();
    }

    public void StopCountDownTimer(View view) {
        if (mTimerStart == true) {
            mLAMP_Timer = 3;
            mTimerStart = false;
            TimesWheelisEnable(true);
            formateTimer(SaveTime);    //將儲存的時間顯示在UI
            playButton.setImageDrawable(getResources().getDrawable(R.mipmap.play));
            stopButton.setImageDrawable(getResources().getDrawable(R.mipmap.stop));
            countDownTimerStatus = CountDownTimerUtil.PREPARE;
            SendData();
        }
    }

    private void formateTimer(long time) {
        Logger.d(TAG, "time = " + time);

        int hour = 0;
        if (time >= 1000 * 3600) {
            hour = (int) (time / (1000 * 3600));
            time -= hour * 1000 * 3600;
        }
        int minute = 0;
        if (time >= 1000 * 60) {
            minute = (int) (time / (1000 * 60));
            time -= minute * 1000 * 60;
        }
        int sec = (int) (time / 1000);

        mHours.setSelection(hour, true);
        mMins.setSelection(minute, true);
        mSecs.setSelection(sec, true);
    }


    private void initializeBluetooth() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View rootView = factory.inflate(R.layout.servicediscovery_temp, null);

        mNoserviceDiscovered = (TextView) rootView.findViewById(R.id.no_service_text);
        mProgressDialog = new ProgressDialog(this);
        mTimer = showServiceDiscoveryAlert(false);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //使用此方法呼叫Service 相較於bindService 來的比較穩定
        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        startService(gattServiceIntent);
        Logger.w(TAG, "Start Service");

        Handler delayHandler = new Handler();
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Logger.i(TAG, "Discover service called");
                if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_CONNECTED) {
                    BluetoothLeService.discoverServices();
                    Logger.i(TAG, "Discover service found");
                }
            }
        }, DELAY_PERIOD);
    }

    //No Service Discovery show Alert
    private Timer showServiceDiscoveryAlert(boolean isReconnect) {
        mProgressDialog.setTitle(getString(R.string.progress_tile_service_discovering));
        if (!isReconnect) {
            mProgressDialog.setMessage(getString(R.string.progress_message_service_discovering));
        } else {
            mProgressDialog.setMessage(getString(R.string.progress_message_reconnect));
        }
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.show();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            showNoServiceDiscoverAlert();
                        }
                    });
                }
            }
        }, SERVICE_DISCOVERY_TIMEOUT);
        return timer;
    }

    private void showNoServiceDiscoverAlert() {
        mConnected = false;
        updateConnectionState(false);
        gotoUseDeviceActivity();
    }

    private void updateConnectionState(final Boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (enabled == true) {
                    Toast.makeText(ControlPageActivity.this,
                            getResources().getString(R.string.alert_message_bluetooth_connect),
                            Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(ControlPageActivity.this,
                            getResources().getString(R.string.alert_message_bluetooth_disconnect),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //取得Service
    private void getGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            Logger.w(TAG, "gattServices = NULL");
            return;
        }

        String uuid = null;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            if (uuid.equals(GattAttributes.DELIGHT_LAMP_SERVICE)
                    || uuid.equals(GattAttributes.DELIGHT_LAMP_SERVICE_CUSTOM)) {

                String CharacteristicUUID = null;

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    CharacteristicUUID = gattCharacteristic.getUuid().toString();
                    if (CharacteristicUUID.equals(GattAttributes.DELIGHT_LAMP)
                            || CharacteristicUUID.equals(GattAttributes.DELIGHT_LAMP_CUSTOM))
                        mReadCharacteristic = gattCharacteristic;
                }
            } else if (uuid.equals(GattAttributes.DEVICE_INFORMATION_SERVICE)) {
                mDeviceInformationService = gattService;
           }

        }
        mProgressDialog.dismiss();
    }

    /**
     * Preparing Broadcast receiver to broadcast read characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataRead(
            BluetoothGattCharacteristic gattCharacteristic) {
        if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            BluetoothLeService.readCharacteristic(gattCharacteristic);
        }
    }

    /**
     * Preparing Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            BluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
        }
    }

    /**
     * Stopping Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void stopBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            if (gattCharacteristic != null) {
                BluetoothLeService.setCharacteristicNotification(
                        gattCharacteristic, false);
            }
        }
    }

    void SendData() {
        byte[] valueByte = new byte[6];
        valueByte[0] = (byte) 1;    //Function
        valueByte[1] = mPower;   //LAMP ON/OFF
        valueByte[2] = mINTENSITY; //INTENSITY
        valueByte[3] = mLAMP_Timer;    //Timer
        valueByte[4] = mTimeOne;    //Time1
        valueByte[5] = mTimeTwo;    //Time2
        BluetoothLeService.writeCharacteristicNoresponse(mReadCharacteristic, valueByte);
    }

    private void displayData(String data) {
        if (data != null) {
            Logger.i(TAG, "Bluetooth Received = " + data);
            String[] AfterSplit = data.split("[,\\s]+"); //切割字元
            //Logger.i(TAG, "AfterSplit[0] = " + AfterSplit[0]);
            //Logger.i(TAG, "AfterSplit[1] = " + AfterSplit[1]);
            //Logger.i(TAG, "AfterSplit[2] = " + AfterSplit[2]);
            //Logger.i(TAG, "AfterSplit[3] = " + AfterSplit[3]);

            mINTENSITY = (byte) Integer.parseInt(AfterSplit[2]);    //String to int
            mSeekBar.setProgress(mINTENSITY & 0xff);   //byte to int

            if ((Integer.parseInt(AfterSplit[1]) == 0 && mPowerStatus == false && FirstLaunch == true) || mReturnBack == true) {
                if (Integer.parseInt(AfterSplit[3]) == 1 || Integer.parseInt(AfterSplit[3]) == 2) {
                    TimeOpen();
                    mTimerStart = true;
                    if (Integer.parseInt(AfterSplit[3]) == 1) {
                        TimesWheelisEnable(false);
                        playButton.setImageDrawable(getResources().getDrawable(R.mipmap.pause_on));
                        stopButton.setImageDrawable(getResources().getDrawable(R.mipmap.stop_on));
                        mLAMP_Timer = 1;
                        countDownTimerStatus = CountDownTimerUtil.START;
                    } else if (Integer.parseInt(AfterSplit[3]) == 2) {
                        playButton.setImageDrawable(getResources().getDrawable(R.mipmap.play_on));
                        stopButton.setImageDrawable(getResources().getDrawable(R.mipmap.stop_on));
                        mLAMP_Timer = 2;
                        countDownTimerStatus = CountDownTimerUtil.PASUSE;
                    }
                }
                FirstLaunch = false;
                PowerOn(false);
            }

            if ((mTimerStart == true && (Integer.parseInt(AfterSplit[3]) == 1 || Integer.parseInt(AfterSplit[3]) == 2)) || mReturnBack == true) {
                countDownTimer = (Integer.parseInt(AfterSplit[4]) * 256 + Integer.parseInt(AfterSplit[5])) * 1000;
                Logger.i(TAG, "countDownTimer = " + countDownTimer);
                formateTimer(countDownTimer);
                if (countDownTimer == 0)
                    PowerOffUIUpdate();
                mReturnBack = false;
            }
        }
    }

    public void connectionLostBluetoothalertbox(Boolean status) {
        //Disconnected
        if (status) {
            mAlert.show();
        } else {
            if (mAlert != null && mAlert.isShowing())
                mAlert.dismiss();
        }
        BLUETOOTH_STATUS_FLAG2 = false;
    }

    private void DeviceInformationDialog() {
        final View item = LayoutInflater.from(this).inflate(R.layout.device_information_measurement, null);
        DeviceInformationService.Init(mDeviceInformationService, item); //初始化

        new AlertDialog.Builder(this)
                .setTitle(R.string.alertdialog_device_info_title)
                .setView(item)
                .setPositiveButton(R.string.alertdialog_rename_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                     }
                })
                .show();
    }

}
