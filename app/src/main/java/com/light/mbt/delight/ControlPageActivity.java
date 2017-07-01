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
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.light.mbt.delight.widget.EditTextView;
import com.light.mbt.delight.widget.TosAdapterView;
import com.light.mbt.delight.widget.WheelTextView;
import com.light.mbt.delight.widget.WheelView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.light.mbt.delight.ListAdapters.NumberAdapter.normalColor;
import static com.light.mbt.delight.R.id.MinsEdit;
import static com.light.mbt.delight.R.id.seekBar;
import static com.light.mbt.delight.UseDevicePageActivity.mLeDeviceListAdapter;
import static com.light.mbt.delight.widget.WheelTextView.hoursArray;
import static com.light.mbt.delight.widget.WheelTextView.minsecsArray;

public class ControlPageActivity extends AppCompatActivity {
    private final static String TAG = " Delight / " + ControlPageActivity.class.getSimpleName();

    // Application
    private NumberAdapter hourAdapter, minAdapter, secAdapter;
    private WheelView mHours = null, mMins = null, mSecs = null;
    private View mDecorView = null;
    private SeekBar mSeekBar = null;
    private ImageView mImageView1 = null, mImageView2 = null;
    private ImageButton powerButton, timerButton, playButton, stopButton;
    private AlertDialog mAlert;
    private ProgressDialog mProgressDialog;
    private Timer mTimer;
    private EditTextView mHoursEdit, mMinsEdit, mSecsEdit;
    private MenuItem Device_Info;

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
    private boolean ItemSelectChange = false, changeFocus = false;
    private int currSelectItem = 0;

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
                updateConnectionState(true);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)
                    || BluetoothLeService.ACL_DISCONNECTED.equals(action)) {
                Logger.i(TAG, "Service DISCONNECTED ----->" + BluetoothLeService.getConnectionState());

                mProgressDialog.dismiss();
                if (mTimer != null)
                    mTimer.cancel();

                if (mConnected)
                    PowerOff(false);

                mConnected = false;
                invalidateOptionsMenu();
                if (!BLUETOOTH_STATUS_FLAG2)
                    gotoUseDeviceActivity();
                //showNoServiceDiscoverAlert();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Logger.i(TAG, "Service discovered");
                mConnected = true;

                Device_Info.setEnabled(true);

                if (mTimer != null)
                    mTimer.cancel();

                // Show all the supported services and characteristics on the user interface.
                getGattServices(BluetoothLeService.getSupportedGattServices());
                if (mReadCharacteristic != null) {
                    stopBroadcastDataNotify(mReadCharacteristic);
                    prepareBroadcastDataRead(mReadCharacteristic);
                    prepareBroadcastDataNotify(mReadCharacteristic);
                }

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
                Logger.i(TAG, "Service discovered unsuccessful");

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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
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

        if (!mConnected)
            findViewById(R.id.noservice).setVisibility(View.VISIBLE);

        initializeControl();

        if (mPowerStatus) {
            mOrientation = true;
            PowerOn(true);
            mSeekBar.setProgress(mINTENSITY & 0xff);   //byte to int
            Logger.i(TAG, "mINTENSITY = " + String.valueOf(mINTENSITY & 0xff));

            if (mTimerStatus) {
                TimeOpen();

                if (mTimerStart)
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
        if (mReadCharacteristic != null) {
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

        if (mTimerStart)
            mTimerStart = false;

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

        mReadCharacteristic = null;

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
        getMenuInflater().inflate(R.menu.home_main, menu);
        menu.findItem(R.id.action_info).setVisible(true);
        menu.findItem(R.id.action_edit).setVisible(false);
        menu.findItem(R.id.action_del).setVisible(false);
        menu.findItem(R.id.action_add).setVisible(false);
        Device_Info = menu.findItem(R.id.action_info);
        Device_Info.setEnabled(false);

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

        mHours.setSelection(0, true);
        mMins.setSelection(0, true);
        mSecs.setSelection(0, true);

        ((WheelTextView) mHours.getSelectedView()).setTextSize(25);
        ((WheelTextView) mMins.getSelectedView()).setTextSize(25);
        ((WheelTextView) mSecs.getSelectedView()).setTextSize(25);

        mHours.setUnselectedAlpha(0.5f);
        mMins.setUnselectedAlpha(0.5f);
        mSecs.setUnselectedAlpha(0.5f);

        mHours.setOnItemSelectedListener(mListener);
        mMins.setOnItemSelectedListener(mListener);
        mSecs.setOnItemSelectedListener(mListener);

        mHours.setOnItemClickListener(mOnClickListener);
        mMins.setOnItemClickListener(mOnClickListener);
        mSecs.setOnItemClickListener(mOnClickListener);

        mHoursEdit = (EditTextView) findViewById(R.id.HoursEdit);
        mMinsEdit = (EditTextView) findViewById(MinsEdit);
        mSecsEdit = (EditTextView) findViewById(R.id.SecsEdit);

        mHoursEdit.setOnFocusChangeListener(mOnFocusChangeListener);
        mMinsEdit.setOnFocusChangeListener(mOnFocusChangeListener);
        mSecsEdit.setOnFocusChangeListener(mOnFocusChangeListener);

        mHoursEdit.setBackListener(mOnBackListener);
        mMinsEdit.setBackListener(mOnBackListener);
        mSecsEdit.setBackListener(mOnBackListener);

        //mHoursEdit.setOnClickListener(mOnClick);
        //mMinsEdit.setOnClickListener(mOnClick);
        //mSecsEdit.setOnClickListener(mOnClick);

        mHoursEdit.setOnTouchListener(mOnTouchListener);
        mMinsEdit.setOnTouchListener(mOnTouchListener);
        mSecsEdit.setOnTouchListener(mOnTouchListener);

        //加入文字監聽
        mHoursEdit.addTextChangedListener(mTextWatcher);
        mMinsEdit.addTextChangedListener(mTextWatcher);
        mSecsEdit.addTextChangedListener(mTextWatcher);

        mHoursEdit.setOnEditorActionListener(mOnEditorActionListener);
        mMinsEdit.setOnEditorActionListener(mOnEditorActionListener);
        mSecsEdit.setOnEditorActionListener(mOnEditorActionListener);

        //mDecorView = getWindow().getDecorView();
        initWheel = false;
    }

    void TimesWheelisEnable(boolean isEnable) {
        mHours.setEnable(isEnable);
        mMins.setEnable(isEnable);
        mSecs.setEnable(isEnable);
    }

    private EditText.OnClickListener mOnClick = new EditText.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == mHoursEdit.getId()) {
                Logger.i(TAG, "mHoursEdit onClick = " + v.getId() + "_" + mHoursEdit.getId());
            } else if (v.getId() == mMinsEdit.getId()) {
                Logger.i(TAG, "mMinsEdit onClick = " + v.getId() + "_" + mMinsEdit.getId());
            } else if (v.getId() == mSecsEdit.getId()) {
                Logger.i(TAG, "mSecsEdit onClick = " + v.getId() + "_" + mSecsEdit.getId());
            }
        }
    };

    private TextWatcher mTextWatcher = new TextWatcher() {
        private String memChar[] = {"", "", ""};

        @Override
        public void afterTextChanged(Editable s) {
            Logger.i(TAG, "afterTextChanged = " + s.toString());
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            Logger.i(TAG, "beforeTextChanged = " + s.toString() + "_" + start + "_" + count + "_" + after);
            if (mHoursEdit.hasFocus()) {
                Logger.i(TAG, "mHoursEdit beforeTextChanged mHoursEdit hasFocus");
                memChar[0] = s.toString();
            }
            if (mMinsEdit.hasFocus()) {
                Logger.i(TAG, "mMinsEdit beforeTextChanged mMinsEdit hasFocus");
                memChar[1] = s.toString();
            }
            if (mSecsEdit.hasFocus()) {
                Logger.i(TAG, "mSecsEdit beforeTextChanged mSecsEdit hasFocus");
                memChar[2] = s.toString();
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Logger.i(TAG, "onTextChanged = " + s.toString() + "_" + start + "_" + before + "_" + count);
            if (mHoursEdit.hasFocus()) {
                Logger.i(TAG, "mHoursEdit onTextChanged mHoursEdit hasFocus");
                int len = mHoursEdit.getText().toString().length();
                if (len > 1) {
                    if ((Integer.parseInt(mHoursEdit.getText().toString()) > 12)) {
                        mHoursEdit.setText(memChar[0]);
                        mHoursEdit.setSelection(1);
                    }
                }
            } else if (mMinsEdit.hasFocus()) {
                Logger.i(TAG, "mMinsEdit onTextChanged mMinsEdit hasFocus");
                int len = mMinsEdit.getText().toString().length();
                if (len > 1) {
                    if ((Integer.parseInt(mMinsEdit.getText().toString()) > 59)) {
                        mMinsEdit.setText(memChar[1]);
                        mMinsEdit.setSelection(1);
                    }
                }
            } else if (mSecsEdit.hasFocus()) {
                Logger.i(TAG, "mSecsEdit onTextChanged mSecsEdit hasFocus");
                int len = mSecsEdit.getText().toString().length();
                if (len > 1) {
                    if ((Integer.parseInt(mSecsEdit.getText().toString()) > 59)) {
                        mSecsEdit.setText(memChar[2]);
                        mSecsEdit.setSelection(1);
                    }
                }
            }
        }
    };

    private EditTextView.BackListener mOnBackListener = new EditTextView.BackListener() {
        @Override
        public void back(TextView textView) {
            Logger.i(TAG, "back = " + textView.getId() + "_" + mHoursEdit.getId());
            delayCleanTextView();
        }
    };

    private EditText.OnFocusChangeListener mOnFocusChangeListener = new EditText.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            Logger.i(TAG, "onFocusChange = " + v.getId() + " / hasFocus =" + hasFocus);
            if (!hasFocus && !changeFocus) {
                if (v.getId() == mHoursEdit.getId()) {
                    Logger.i(TAG, "mHoursEdit onFocusChange = " + v.getId() + "_" + mHoursEdit.getId());
                    ((WheelTextView) mHours.getSelectedView()).setText(String.format("%02d", mHours.getSelectedItemPosition()));
                    mHoursEdit.setVisibility(View.INVISIBLE);
                    if (!mHoursEdit.getText().toString().equals(""))
                        mHours.setSelection(Integer.parseInt(mHoursEdit.getText().toString()), true);
                } else if (v.getId() == mMinsEdit.getId()) {
                    Logger.i(TAG, "mMinsEdit onFocusChange = " + v.getId() + "_" + mMinsEdit.getId());
                    mMinsEdit.setVisibility(View.INVISIBLE);
                    ((WheelTextView) mMins.getSelectedView()).setText(String.format("%02d", mMins.getSelectedItemPosition()));
                    if (!mMinsEdit.getText().toString().equals(""))
                        mMins.setSelection(Integer.parseInt(mMinsEdit.getText().toString()), true);
                } else if (v.getId() == mSecsEdit.getId()) {
                    Logger.i(TAG, "mSecsEdit onFocusChange = " + v.getId() + "_" + mSecsEdit.getId());
                    ((WheelTextView) mSecs.getSelectedView()).setText(String.format("%02d", mSecs.getSelectedItemPosition()));
                    mSecsEdit.setVisibility(View.INVISIBLE);
                    if (!mSecsEdit.getText().toString().equals(""))
                        mSecs.setSelection(Integer.parseInt(mSecsEdit.getText().toString()), true);
                }
            }
        }
    };

    private EditText.OnEditorActionListener mOnEditorActionListener = new EditText.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                if (v.getId() == mHoursEdit.getId()) {
                    Logger.i(TAG, "mHoursEdit onEditorAction = " + v.getId() + "_" + mHoursEdit.getId());
                    mHoursEdit.setVisibility(View.INVISIBLE);
                    mMinsEdit.setVisibility(View.VISIBLE);
                    WheelTextView mMinsView = (WheelTextView) mMins.getSelectedView();
                    mMinsEdit.setText(mMinsView.getText());
                    mMinsView.setText("");
                    changeFocus = false;
                    setEditFocus(mMinsEdit);
                } else if (v.getId() == mMinsEdit.getId()) {
                    Logger.i(TAG, "mMinsEdit onEditorAction = " + v.getId() + "_" + mMinsEdit.getId());
                    mMinsEdit.setVisibility(View.INVISIBLE);
                    mSecsEdit.setVisibility(View.VISIBLE);
                    WheelTextView mSecsView = (WheelTextView) mSecs.getSelectedView();
                    mSecsEdit.setText(mSecsView.getText());
                    mSecsView.setText("");
                    changeFocus = false;
                    setEditFocus(mSecsEdit);
                }
                return true;
            } else if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (v.getId() == mSecsEdit.getId()) {
                    Logger.i(TAG, "mSecsEdit onEditorAction = " + v.getId() + "_" + mSecsEdit.getId());
                    mSecsEdit.setVisibility(View.INVISIBLE);

                    //hide soft keyboard
                    hideInputMethod();
                }
                return true;
            }
            return false;
        }
    };

    private EditText.OnTouchListener mOnTouchListener = new EditText.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                changeFocus = true;
                mHours.setFocusable(true);
                mHours.setFocusableInTouchMode(true);
                mHours.requestFocus();
                if (v.getId() == mHoursEdit.getId() && mHoursEdit.getVisibility() == View.VISIBLE) {
                    Logger.i(TAG, "mHoursEdit onTouch = " + v.getId() + "_" + mHoursEdit.getId());
                    setEditFocus(mHoursEdit);
                } else if (v.getId() == mMinsEdit.getId() && mMinsEdit.getVisibility() == View.VISIBLE) {
                    Logger.i(TAG, "mMinsEdit onTouch = " + v.getId() + "_" + mMinsEdit.getId());
                    setEditFocus(mMinsEdit);
                } else if (v.getId() == mSecsEdit.getId() && mSecsEdit.getVisibility() == View.VISIBLE) {
                    Logger.i(TAG, "mSecsEdit onTouch = " + v.getId() + "_" + mSecsEdit.getId());
                    setEditFocus(mSecsEdit);
                }
                changeFocus = false;
                delayCleanTextView();
            }
            return false;
        }
    };

    private TosAdapterView.OnItemClickListener mOnClickListener = new TosAdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(TosAdapterView<?> parent, View view, int position, long id) {
            changeFocus = true;
            currSelectItem = position;
            Logger.i(TAG, "onItemClick position = " + position + "_" + parent.getId() + " / changeFocus = " + changeFocus);

            if (!ItemSelectChange) {
                if (parent.getId() == mHours.getId()) {
                    Logger.i(TAG, "mHours onItemClick position = " + position + "_" + parent.getId() + "_" + mHours.getId());
                    mHoursEdit.setVisibility(View.VISIBLE);
                    WheelTextView mHoursView = (WheelTextView) mHours.getSelectedView();
                    mHoursEdit.setText(mHoursView.getText());
                    changeFocus = false;
                    setEditFocus(mHoursEdit);
                } else if (parent.getId() == mMins.getId()) {
                    Logger.i(TAG, "mMins onItemClick position = " + position + "_" + parent.getId() + "_" + mMins.getId());
                    mMinsEdit.setVisibility(View.VISIBLE);
                    WheelTextView mMinsView = (WheelTextView) mMins.getSelectedView();
                    mMinsEdit.setText(mMinsView.getText());
                    changeFocus = false;
                    setEditFocus(mMinsEdit);
                } else if (parent.getId() == mSecs.getId()) {
                    Logger.i(TAG, "mSecs onItemClick position = " + position + "_" + parent.getId() + "_" + mSecs.getId());
                    mSecsEdit.setVisibility(View.VISIBLE);
                    WheelTextView mSecsView = (WheelTextView) mSecs.getSelectedView();
                    mSecsEdit.setText(mSecsView.getText());
                    changeFocus = false;
                    setEditFocus(mSecsEdit);
                }
                delayCleanTextView();
            }
            changeFocus = false;
            ItemSelectChange = false;
        }
    };

    private TosAdapterView.OnItemSelectedListener mListener = new TosAdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(TosAdapterView<?> parent, View view, int position, long id) {
            Logger.i(TAG, "onItemSelected position = " + position);
            boolean isEnabled = ((WheelView) parent).isEnable();
            ItemSelectChange = true;

            if (((WheelTextView) view).getText().toString().equals(""))
                ((WheelTextView) view).setText(String.format("%02d", position));

            ((WheelTextView) view).setTextSize(25);
            ((WheelTextView) view).setGravity(Gravity.CENTER);

            int index = Integer.parseInt(view.getTag().toString());
            int count = parent.getChildCount();
            Logger.i(TAG, "onItemSelected position = " + index + "_" + count);

            if (index < count - 1) {
                ((WheelTextView) parent.getChildAt(index + 1)).setTextSize(25);    //下方字體大小
                if (isEnabled || mTimerStart)
                    ((WheelTextView) parent.getChildAt(index + 1)).setTextColor(normalColor);    //下方字體顏色
            }

            if (index > 0) {
                ((WheelTextView) parent.getChildAt(index - 1)).setTextSize(25);    //上方字體大小
                if (isEnabled || mTimerStart)
                    ((WheelTextView) parent.getChildAt(index - 1)).setTextColor(normalColor);    //上方字體顏色
            }

            if (mTimerStatus && !mTimerStart && !initWheel) {
                get_TotalTime();
            }

            checkEditVisibility();

            if (currSelectItem == position)
                ItemSelectChange = false;

        }

        @Override
        public void onNothingSelected(TosAdapterView<?> parent) {
        }
    };

    private void setEditFocus(EditText editText) {
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.requestFocus();
        editText.setSelectAllOnFocus(true);
        editText.selectAll();
        //show soft keyboard
        showInputMethod();
    }


    /**
     * 顯示鍵盤
     */
    protected void showInputMethod() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != imm) {
            View v = getWindow().getDecorView();
            if (!isKeyboardShown(v))
                imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
        }
    }

    /**
     * 隱藏鍵盤
     */
    protected void hideInputMethod() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != imm) {
            View v = getWindow().getDecorView();
            if (isKeyboardShown(v))
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    /**
     * 檢查鍵盤是否顯示
     */
    private boolean isKeyboardShown(View rootView) {
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Logger.i(TAG, "landscape");
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Logger.i(TAG, "portrait");
        }
        final int softKeyboardHeight = 100;
        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        int heightDiff = rootView.getBottom() - r.bottom;
        return heightDiff > softKeyboardHeight * dm.density;
    }

    private void delayCleanTextView() {
        Handler delayHandler = new Handler();
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mHoursEdit.hasFocus()) {
                    ((WheelTextView) mHours.getSelectedView()).setText("");
                } else if (mMinsEdit.hasFocus()) {
                    ((WheelTextView) mMins.getSelectedView()).setText("");
                } else if (mSecsEdit.hasFocus()) {
                    ((WheelTextView) mSecs.getSelectedView()).setText("");
                }
            }
        }, 300);
    }

    private void checkEditVisibility() {
        if (mHoursEdit.getVisibility() == View.VISIBLE || mMinsEdit.getVisibility() == View.VISIBLE ||
                mSecsEdit.getVisibility() == View.VISIBLE)
            hideInputMethod();

        if (mHoursEdit.getVisibility() == View.VISIBLE) {
            mHoursEdit.setVisibility(View.INVISIBLE);
        } else if (mMinsEdit.getVisibility() == View.VISIBLE) {
            mMinsEdit.setVisibility(View.INVISIBLE);
        } else if (mSecsEdit.getVisibility() == View.VISIBLE) {
            mSecsEdit.setVisibility(View.INVISIBLE);
        }
    }

    public void onPower(View view) {
        if (!mPowerStatus) {
            PowerOn(true);
            mPower = 0;
            SendData();
        } else {
            PowerOff(true);
        }
    }

    private void PowerOn(boolean isEnable) {
        if (mLAMP_Timer != 1 && mLAMP_Timer != 2 && isEnable && !mOrientation) {
            mINTENSITY = (byte) 255;
            mSeekBar.setProgress(mINTENSITY & 0xff);   //byte to int
        }

        mSeekBar.setEnabled(true);
        mImageView1.setAlpha(1f);
        mImageView2.setAlpha(1f);
        timerButton.setEnabled(true);
        timerButton.setAlpha(1f);

        mPowerStatus = true;
        powerButton.setImageDrawable(getResources().getDrawable(R.mipmap.power_on));
    }

    private void PowerOff(boolean sendData) {
        PowerOffUIUpdate();

        if (sendData)
            SendData();
    }

    void PowerOffUIUpdate() {
        mINTENSITY = 0;
        mPower = 1;
        if (mTimerStart) //關閉電源時，如果計時器有動作則關閉
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
        if (!mTimerStatus) {
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
        if (mTimerStart) {
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
        //Logger.d(TAG, "All_Total_Times = " + All_Total_Times);

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
        //Logger.d(TAG, "valueByte = " + mTimeOne + "," + mTimeTwo);
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
        if (mTimerStart) {
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

    public void reConnect(View view) {
        if (findViewById(R.id.noservice).getVisibility() == View.VISIBLE)
            findViewById(R.id.noservice).setVisibility(View.GONE);

        Logger.i(TAG, "BLE Connection State---->" + BluetoothLeService.getConnectionState());
        if (BluetoothLeService.getConnectionState() == 0)
            BluetoothLeService.connect(mDeviceAddress, mDeviceName, ControlPageActivity.this);
        else if (BluetoothLeService.getConnectionState() > 0)
            BluetoothLeService.reconnect();

        mTimer = showServiceDiscoveryAlert(false);

        DiscoverServices();
    }

    private void DiscoverServices() {
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

    private void initializeBluetooth() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View rootView = factory.inflate(R.layout.servicediscovery_temp, null);

        UseDevicePageActivity.gotoControlPage = false;

        mProgressDialog = new ProgressDialog(this);
        mTimer = showServiceDiscoveryAlert(false);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        DiscoverServices();
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
        findViewById(R.id.noservice).setVisibility(View.VISIBLE);   //No Serivce 顯示
    }

    private void updateConnectionState(final Boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (enabled) {
                    Toast.makeText(ControlPageActivity.this,
                            getResources().getString(R.string.alert_message_bluetooth_connect),
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
        if (!mConnected)
            return;

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

            if ((Integer.parseInt(AfterSplit[1]) == 0 && !mPowerStatus && FirstLaunch) || mReturnBack) {
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
                if (Integer.parseInt(AfterSplit[1]) == 0)
                    PowerOn(false);
            }

            if ((mTimerStart && (Integer.parseInt(AfterSplit[3]) == 1 || Integer.parseInt(AfterSplit[3]) == 2)) || mReturnBack) {
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
