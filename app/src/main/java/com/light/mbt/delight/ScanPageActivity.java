package com.light.mbt.delight;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.light.mbt.delight.CommonUtils.Logger;
import com.light.mbt.delight.CommonUtils.Utils;
import com.light.mbt.delight.ListAdapters.DeviceList;
import com.light.mbt.delight.ListAdapters.LeScanDeviceListAdapter;
import com.light.mbt.delight.bluetooth.BluetoothLeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ScanPageActivity extends AppCompatActivity {

    private final static String TAG = " Delight / " + ScanPageActivity.class.getSimpleName();
    private static final String BLUETOOTH_DEVICE_NAME = "Delight";  //限制搜尋欄牙的名稱

    private Handler mHandler;

    //Delay Time out
    private static final long DELAY_PERIOD = 500;

    // Stops scanning after 2 seconds.
    private static final long SCAN_PERIOD_TIMEOUT = 2000;

    // Connection time out after 10 seconds.
    private static final long CONNECTION_TIMEOUT = 10000;

    private Timer mConnectTimer;

    // Activity request constant
    private static final int REQUEST_ENABLE_BT = 1;

    // device details
    public static String mDeviceName = "name";
    public static String mDeviceAddress = "address";

    //Pair status button and variables
    public static Button mPairButton;
    private String Paired;
    private String Unpaired;

    //Bluetooth adapter
    private static BluetoothAdapter mBluetoothAdapter;
    private LeScanDeviceListAdapter mLeScanDeviceListAdapter;

    // Devices list variables
    public static ArrayList<BluetoothDevice> mLeDevices;
    private static ArrayList<DeviceList> mDeviceList;

    private SwipeRefreshLayout mSwipeLayout;
    public static Map<String, Integer> mDevRssiValues;

    //GUI elements
    private ListView mProfileListView;
    private TextView mRefreshText;
    public static ProgressDialog mProgressdialog;
    private AlertDialog mAlert;

    //  Flags
    private boolean mSearchEnabled = false;
    private boolean BLUETOOTH_STATUS_FLAG = true;
    private boolean mConnectTimerON = false;
    private boolean mScanning;

    // progress dialog variable
    private ProgressDialog mpdia;

    /**
     * Call back for BLE Scan
     * This call back is called when a BLE device is found near by.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!mSearchEnabled) {
                        boolean FirstAdd = true;

                        //已加入Device List 清單不再加入掃描列表
                        if (mDeviceList.size() > 0) {
                            for (int i = 0; i < mDeviceList.size(); i++) {
                                if (mDeviceList.get(i).getDeviceAddress().equalsIgnoreCase(device.getAddress())) {
                                    FirstAdd = false;
                                    break;
                                }
                            }
                        }

                        if (FirstAdd == true && device.getName().indexOf(BLUETOOTH_DEVICE_NAME) > -1)  //以名稱限制搜尋欄牙
                            mLeScanDeviceListAdapter.addDevice(device, rssi);
                        //Logger.w(TAG, "Device Name = "+device.getName());
                        try {
                            mLeScanDeviceListAdapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    };

    /**
     * BroadcastReceiver for receiving the GATT communication status
     */
    private final BroadcastReceiver mGattConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // Status received when connected to GATT Server
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mProgressdialog.setMessage(getString(R.string.alert_message_bluetooth_connect));
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                mProgressdialog.dismiss();
                mLeDevices.clear();
                if (mConnectTimer != null)
                    mConnectTimer.cancel();
                mConnectTimerON = false;
                gotoControlActivity();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                /**
                 * Disconnect event.When the connect timer is ON,Reconnect the device
                 * else show disconnect message
                 */
                if (mConnectTimerON) {
                    BluetoothLeService.reconnect();
                } else {
                    Toast.makeText(ScanPageActivity.this,
                            getResources().getString(R.string.alert_message_bluetooth_disconnect),
                            Toast.LENGTH_SHORT).show();
                }
            }

            //Received when the bond state is changed
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
                    String dataLog2 = "[" + mDeviceName + "|"
                            + mDeviceAddress + "] " +
                            "Pairing request received";
                    Logger.i(TAG, dataLog2);
                    Utils.bondingProgressDialog(ScanPageActivity.this, mpdia, true);
                } else if (state == BluetoothDevice.BOND_BONDED) {
                    Logger.i(TAG, "ScanPageActivity--->Bonded");
                    Utils.stopDialogTimer();
                    // Bonded...
                    if (mPairButton != null) {
                        mPairButton.setText(Paired);
                        if (bondState == BluetoothDevice.BOND_BONDED && previousBondState == BluetoothDevice.BOND_BONDING) {
                            Toast.makeText(ScanPageActivity.this, "Device paired successfully", Toast.LENGTH_SHORT).show();
                        }
                    }
                    String dataLog = "[" + mDeviceName + "|"
                            + mDeviceAddress + "] " +
                            "Paired";
                    Logger.i(TAG, dataLog);
                    Utils.bondingProgressDialog(ScanPageActivity.this, mpdia, false);

                } else if (state == BluetoothDevice.BOND_NONE) {
                    // Not bonded...
                    Logger.i(TAG, "ScanPageActivity--->Not Bonded");
                    Utils.stopDialogTimer();
                    if (mPairButton != null) {
                        mPairButton.setText(Unpaired);
                        if (bondState == BluetoothDevice.BOND_NONE && previousBondState == BluetoothDevice.BOND_BONDED) {
                            Toast.makeText(ScanPageActivity.this, "Device unpaired successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ScanPageActivity.this,
                                    "Unable to pair. Check whether the device is advertising and supports pairing.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                    String dataLog = "[" + mDeviceName + "|"
                            + mDeviceAddress + "] " +
                            "Unable to pair. Check whether the device is advertising and supports pairing.";
                    Logger.i(TAG, dataLog);
                    Utils.bondingProgressDialog(ScanPageActivity.this, mpdia, false);
                } else {
                    Logger.e(TAG, "Error received in pair-->" + state);
                }
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                Logger.i(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED.");
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
                    }

                }

            } else if (action.equals(BluetoothLeService.ACTION_PAIR_REQUEST)) {
                Logger.i(TAG, "Pair request received");
                Logger.i(TAG, "ScanPageActivity--->Pair Request");
                Utils.stopDialogTimer();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_scan_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  //增加左上角返回圖示
        //getActionBar().setTitle(R.string.title_devices);  //設定ActionBar 標題

        mHandler = new Handler();

        Paired = getResources().getString(R.string.bluetooth_pair);
        Unpaired = getResources().getString(R.string.bluetooth_unpair);
        mpdia = new ProgressDialog(this);
        mpdia.setCancelable(false);

        mAlert = new AlertDialog.Builder(this).create();
        mAlert.setMessage(getResources().getString(
                R.string.alert_message_bluetooth_reconnect));
        mAlert.setCancelable(false);
        mAlert.setTitle(getResources().getString(R.string.app_name));
        mAlert.setButton(Dialog.BUTTON_POSITIVE, getResources().getString(
                R.string.alert_message_exit_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intentActivity = new Intent(ScanPageActivity.this, UseDevicePageActivity.class);
                finish();
                overridePendingTransition(
                        R.anim.slide_left, R.anim.push_left);
                startActivity(intentActivity);
                overridePendingTransition(
                        R.anim.slide_right, R.anim.push_right);
            }
        });
        mAlert.setCanceledOnTouchOutside(false);

        mDevRssiValues = new HashMap<String, Integer>();
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);

        mDeviceList = new ArrayList<DeviceList>();
        mDeviceList = Utils.getDeviceListArraySharedPreference(ScanPageActivity.this, "DEVICE_LIST");

        mSwipeLayout.setColorSchemeResources(
                R.color.dark_blue,
                R.color.medium_blue,
                R.color.light_blue,
                R.color.faint_blue);

        mProfileListView = (ListView) findViewById(R.id.listView_profiles);
        mRefreshText = (TextView) findViewById(R.id.no_dev);
        mLeScanDeviceListAdapter = new LeScanDeviceListAdapter(ScanPageActivity.this);
        mProfileListView.setAdapter(mLeScanDeviceListAdapter);
        mProfileListView.setTextFilterEnabled(true);

        mProgressdialog = new ProgressDialog(this);
        mProgressdialog.setCancelable(false);

        Intent gattServiceIntent = new Intent(getApplicationContext(),
                BluetoothLeService.class);
        startService(gattServiceIntent);
        Logger.i(TAG, "Start Service");

        checkBleSupportAndInitialize();
        prepareList();

        /**
         * Swipe listener,initiate a new scan on refresh. Stop the swipe refresh
         * after 5 seconds
         */
        mSwipeLayout
                .setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

                    @Override
                    public void onRefresh() {
                        if (!mScanning) {
                            // Prepare list view and initiate scanning
                            if (mLeScanDeviceListAdapter != null) {
                                mLeScanDeviceListAdapter.clear();
                                try {
                                    mLeScanDeviceListAdapter.notifyDataSetChanged();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            scanLeDevice(true);
                            mScanning = true;
                            mSearchEnabled = false;
                            mRefreshText.setText(getResources().getString(
                                    R.string.profile_control_device_scanning));
                        }

                    }

                });


        /**
         * Creating the dataLogger file and
         * updating the datalogger history
         */
        mProfileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (mLeScanDeviceListAdapter.getCount() > 0) {
                    final BluetoothDevice device = mLeScanDeviceListAdapter
                            .getDevice(position);
                    if (device != null) {
                        connectDevice(device, true);
                        scanLeDevice(false);
                    }
                }
            }
        });

    }

    private void checkBleSupportAndInitialize() {
        // Use this check to determine whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.device_ble_not_supported,
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Blue tooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            // Device does not support Blue tooth
            Toast.makeText(this,
                    R.string.device_bluetooth_not_supported, Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
    }

    private void gotoControlActivity() {
        if (mLeScanDeviceListAdapter != null) {
            mLeScanDeviceListAdapter.clear();
            try {
                mLeScanDeviceListAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        final Intent intent = new Intent(this, ControlPageActivity.class);
        intent.putExtra(ControlPageActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(ControlPageActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);

        Bundle bundle = new Bundle();
        bundle.putSerializable("DeviceList", saveDeviceAndSend());  //將DeviceList 傳到ControlPageActivity
        intent.putExtras(bundle);

        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }

        finish();
        overridePendingTransition(R.anim.slide_right, R.anim.push_right);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left, R.anim.push_left);
    }

    private DeviceList saveDeviceAndSend() {
        DeviceList deviceList;

        deviceList = new DeviceList();
        deviceList.setName(mDeviceName);
        deviceList.setTime(1800000);
        deviceList.setDeviceName(mDeviceName);
        deviceList.setDeviceAddress(mDeviceAddress);

        Utils.setDeviceListArraySharedPreference(ScanPageActivity.this, "DEVICE_LIST", deviceList, false);  //儲存新加入裝置
        UseDevicePageActivity.mLeDeviceListAdapter.addDevice(deviceList);   //新加入裝置更新

        return deviceList;
    }

    /**
     * Method to connect to the device selected. The time allotted for having a
     * connection is 8 seconds. After 8 seconds it will disconnect if not
     * connected and initiate scan once more
     *
     * @param device
     */
    private void connectDevice(BluetoothDevice device, boolean isFirstConnect) {
        mDeviceAddress = device.getAddress();
        mDeviceName = device.getName();
        // Get the connection status of the device
        if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTED) {
            Logger.v(TAG, "BLE DISCONNECTED STATE");
            Logger.v(TAG, "CONNECTED To : " + mDeviceName + "|" + mDeviceAddress);
            // Disconnected,so connect
            BluetoothLeService.connect(mDeviceAddress, mDeviceName, ScanPageActivity.this);
            showConnectAlertMessage(mDeviceName, mDeviceAddress);
        } else {
            Logger.v(TAG, "BLE OTHER STATE-->" + BluetoothLeService.getConnectionState());
            // Connecting to some devices,so disconnect and then connect
            BluetoothLeService.disconnect();
            Handler delayHandler = new Handler();
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BluetoothLeService.connect(mDeviceAddress, mDeviceName, ScanPageActivity.this);
                    showConnectAlertMessage(mDeviceName, mDeviceAddress);
                }
            }, DELAY_PERIOD);

        }
        if (isFirstConnect) {
            startConnectTimer();
            mConnectTimerON = true;
        }

    }

    private void showConnectAlertMessage(String devicename, String deviceaddress) {
        mProgressdialog.setTitle(getResources().getString(
                R.string.alert_message_connect_title));
        mProgressdialog.setMessage(getResources().getString(
                R.string.alert_message_connect)
                + "\n"
                + devicename
                + "\n"
                + deviceaddress
                + "\n"
                + getResources().getString(R.string.alert_message_wait));

        if (!isDestroyed() && mProgressdialog != null) {
            mProgressdialog.show();
        }
    }

    /**
     * Preparing the BLE Devicelist
     */
    public void prepareList() {
        // 修改抬頭名稱
        setTitle(R.string.profile_scan_fragment);
        // Prepare list view and initiate scanning
        mLeScanDeviceListAdapter = new LeScanDeviceListAdapter(ScanPageActivity.this);
        mProfileListView.setAdapter(mLeScanDeviceListAdapter);
        scanLeDevice(true);
        mSearchEnabled = false;
    }

    @Override
    public void onResume() {
        Logger.i(TAG, "Scanning onResume");
        Logger.i(TAG, "BLE DISCONNECT---->" + BluetoothLeService.getConnectionState());

        Logger.i(TAG, "Registering receiver in Profile scannng");
        registerReceiver(mGattConnectReceiver,
                Utils.makeGattUpdateIntentFilter());

        BLUETOOTH_STATUS_FLAG = true;

        super.onResume();
    }

    @Override
    public void onPause() {
        Logger.i(TAG, "Scanning onPause");
        if (mProgressdialog != null && mProgressdialog.isShowing()) {
            mProgressdialog.dismiss();
        }
        Logger.i(TAG, "UN Registering receiver in Profile scannng");
        unregisterReceiver(mGattConnectReceiver);
        BLUETOOTH_STATUS_FLAG = false;

        //退出程式將Service 關閉
        Intent gattServiceIntent = new Intent(getApplicationContext(),
                BluetoothLeService.class);
        stopService(gattServiceIntent);
        Logger.i(TAG, "stopService");

        super.onPause();
    }

    @Override
    public void onDestroy() {
        scanLeDevice(false);
        if (mLeScanDeviceListAdapter != null)
            mLeScanDeviceListAdapter.clear();
        if (mLeScanDeviceListAdapter != null) {
            try {
                mLeScanDeviceListAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mSwipeLayout.setRefreshing(false);

        super.onDestroy();
    }

    /**
     * Handling the back pressed actions
     */
    @Override
    public void onBackPressed() {
        Logger.i(TAG, "Scanning onBackPressed");
        Logger.i(TAG, "BLE DISCONNECT---->" + BluetoothLeService.getConnectionState());
        if (BluetoothLeService.getConnectionState() == 2 ||
                BluetoothLeService.getConnectionState() == 1 ||
                BluetoothLeService.getConnectionState() == 4) {
            BluetoothLeService.disconnect();
            Toast.makeText(this,
                    getResources().getString(R.string.alert_message_bluetooth_disconnect),
                    Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(this, UseDevicePageActivity.class);
        finish();
        overridePendingTransition(R.anim.slide_right, R.anim.push_right);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left, R.anim.push_left);

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.home_main, menu); //menu Setting not use
        return true;
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

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (!mScanning) {
                startScanTimer();
                mScanning = true;
                mRefreshText.setText(getResources().getString(
                        R.string.profile_control_device_scanning));
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                Logger.i(TAG, "scanLeDevice = " + enable);
                mSwipeLayout.setRefreshing(true);
            }
        } else {
            mScanning = false;
            mSwipeLayout.setRefreshing(false);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    /**
     * Connect Timer
     */
    private void startConnectTimer() {
        mConnectTimer = new Timer();
        mConnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mProgressdialog.dismiss();
                Logger.v(TAG, "CONNECTION TIME OUT");
                mConnectTimerON = false;
                BluetoothLeService.disconnect();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ScanPageActivity.this,
                                getResources().getString(R.string.profile_cannot_connect_message),
                                Toast.LENGTH_SHORT).show();
                        if (mLeScanDeviceListAdapter != null)
                            mLeScanDeviceListAdapter.clear();
                        if (mLeScanDeviceListAdapter != null) {
                            try {
                                mLeScanDeviceListAdapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        scanLeDevice(true);
                        mScanning = true;
                    }
                });
            }
        }, CONNECTION_TIMEOUT);
    }

    /**
     * Swipe refresh timer
     */
    public void startScanTimer() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mRefreshText.post(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshText.setText(getResources().getString(
                                R.string.profile_control_no_device_message));
                    }
                });
                mSwipeLayout.setRefreshing(false);
                scanLeDevice(false);
            }
        }, SCAN_PERIOD_TIMEOUT);
    }

    public void connectionLostBluetoothalertbox(Boolean status) {
        //Disconnected
        if (status) {
            mAlert.show();
        } else {
            if (mAlert != null && mAlert.isShowing())
                mAlert.dismiss();
        }
    }
}
