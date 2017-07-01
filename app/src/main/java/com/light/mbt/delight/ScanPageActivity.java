package com.light.mbt.delight;

import android.app.Activity;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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

public class ScanPageActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final static String TAG = " Delight / " + ScanPageActivity.class.getSimpleName();
    private static final String BLUETOOTH_DEVICE_NAME = "Delight";  //限制搜尋藍牙的名稱

    //Delay Time out
    private static final long DELAY_PERIOD = 500;

    // Stops scanning after 2 seconds.
    private static final long SCAN_PERIOD_TIMEOUT = 5000;

    // Connection time out after 10 seconds.
    private static final long CONNECTION_TIMEOUT = 10000;

    // Activity request constant
    private static final int REQUEST_ENABLE_BT = 1;

    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;

    // device details
    public static String mDeviceName = "name";
    public static String mDeviceAddress = "address";

    //Bluetooth adapter
    private static BluetoothAdapter mBluetoothAdapter;
    private LeScanDeviceListAdapter mLeScanDeviceListAdapter;

    // Devices list variables
    public static ArrayList<BluetoothDevice> mLeDevices;
    public static ArrayList<DeviceList> mDeviceList;
    public static HashMap<String, String> mDeviceNameList = new HashMap<String, String>();

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
    private boolean stopSerivce = false;
    public static boolean gotoControlPage = false;

    // progress dialog variable
    private ProgressDialog mpdia;

    private Timer mConnectTimer;

    private Runnable runnable;
    private Handler mHandler;

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
                        try {
                            if (device.getName().indexOf(BLUETOOTH_DEVICE_NAME) > -1) {  //以名稱限制搜尋欄牙
                                mLeScanDeviceListAdapter.addDevice(device, rssi);

                                mLeScanDeviceListAdapter.notifyDataSetChanged();
                            }
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
            Logger.i(TAG, "action = " + action);
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
            }else if(BluetoothLeService.GATT_STATUS_133.equals(action)){
                Logger.i(TAG, "GATT_STATUS_133");
                BluetoothLeService.connect(mDeviceAddress, mDeviceName, ScanPageActivity.this);
            }

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
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
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);  //增加左上角返回圖示

        initDrawer();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

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
                Intent intentActivity = getIntent();
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

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeLayout.setColorSchemeResources(
                R.color.dark_blue,
                R.color.medium_blue,
                R.color.light_blue,
                R.color.faint_blue);

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
                            findViewById(R.id.device_nofound).setVisibility(View.GONE);   //Device Not Founf 顯示
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
                        mHandler.removeCallbacks(runnable);
                        mDeviceAddress = device.getAddress();
                        mDeviceName = mDeviceNameList.get(mDeviceAddress);

                        if (mDeviceName == null)
                            mDeviceName = device.getName().toString();

                        scanLeDevice(false);
                        connectDevice(device, true);
                    }
                }
            }
        });

        // Set the Clear cahce on disconnect as true by devfault
        if (!Utils.ifContainsSharedPreference(this, "PREF_PAIR_CACHE_STATUS")) {
            Utils.setBooleanSharedPreference(this, "PREF_PAIR_CACHE_STATUS", true);
        }

        checkBleSupportAndInitialize();
    }

    private void initDrawer() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.products_name);  //設置標題
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);  //增加左上角返回圖示

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
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

        //finish();
        overridePendingTransition(R.anim.slide_right, R.anim.push_right);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left, R.anim.push_left);
        gotoControlPage = true;
    }

    private DeviceList saveDeviceAndSend() {
        DeviceList deviceList;
        if (mDeviceList.size() > 0) {
            for (int i = 0; i < mDeviceList.size(); i++) {
                if (mDeviceList.get(i).getDeviceAddress().equalsIgnoreCase(mDeviceAddress)) {
                    return mDeviceList.get(i);
                }
            }
        }

        deviceList = new DeviceList();
        deviceList.setName(mDeviceName);
        deviceList.setTime(1800000);
        deviceList.setDeviceName(mDeviceName);
        deviceList.setDeviceAddress(mDeviceAddress);

        Utils.setDeviceListArraySharedPreference(ScanPageActivity.this, "DEVICE_LIST", deviceList, false);  //儲存新加入裝置

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
        // Get the connection status of the device
        if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTED) {
            Logger.v(TAG, "BLE DISCONNECTED STATE");
            Logger.v(TAG, "CONNECTED To : " + mDeviceName + "|" + mDeviceAddress);
            // Disconnected,so connect
            Handler delayHandler = new Handler();
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BluetoothLeService.connect(mDeviceAddress, mDeviceName, ScanPageActivity.this);
                }
            }, 2000);
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
                }
            }, DELAY_PERIOD);
            showConnectAlertMessage(mDeviceName, mDeviceAddress);
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
        //setTitle(R.string.profile_scan_fragment);
        // Prepare list view and initiate scanning
        mLeScanDeviceListAdapter = new LeScanDeviceListAdapter(ScanPageActivity.this);
        mProfileListView.setAdapter(mLeScanDeviceListAdapter);
        scanLeDevice(true);
        mSearchEnabled = false;
    }

    @Override
    public void onResume() {
        Logger.i(TAG, "Scanning onResume");
        Logger.i(TAG, "BLE Connection State---->" + BluetoothLeService.getConnectionState());

        if (checkBluetoothStatus()) {
            prepareList();
        }

        Logger.i(TAG, "Registering receiver in Profile scannng");
        registerReceiver(mGattConnectReceiver,
                Utils.makeGattUpdateIntentFilter());

        mDeviceList = new ArrayList<DeviceList>();
        mDeviceList = Utils.getDeviceListArraySharedPreference(ScanPageActivity.this, "DEVICE_LIST");

        BLUETOOTH_STATUS_FLAG = true;

        if (stopSerivce == true) {
            Intent gattServiceIntent = new Intent(getApplicationContext(),
                    BluetoothLeService.class);
            startService(gattServiceIntent);
            stopSerivce = false;
            Logger.i(TAG, "startService");
        }

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

        if (gotoControlPage == false) {
            //將Service 關閉
            Intent gattServiceIntent = new Intent(getApplicationContext(),
                    BluetoothLeService.class);
            stopService(gattServiceIntent);
            stopSerivce = true;
            Logger.i(TAG, "stopService");
        }

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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                initDrawer();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Uri uri;
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.nav_home:
                uri = Uri.parse("http://www.mathbright.com.tw/");
                intent = new Intent(Intent.ACTION_VIEW, uri);
                break;
            case R.id.nav_products:
                uri = Uri.parse("http://www.mathbright.com.tw/Product.html");
                intent = new Intent(Intent.ACTION_VIEW, uri);
                break;
            case R.id.nav_contactus:
                //Uri uri=Uri.parse("http://www.mathbright.com.tw/Contact.html");
                uri = Uri.parse("mailto:info@mathbright.com.tw");
                intent = new Intent(Intent.ACTION_SENDTO, uri);
                break;
            case R.id.nav_about:
                intent = new Intent(this, AboutPageActivity.class);
                break;
        }
        overridePendingTransition(R.anim.slide_left, R.anim.push_left);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_right, R.anim.push_right);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (!mScanning) {
                Logger.i(TAG, "scanLeDevice = " + enable);
                startScanTimer();
                mScanning = true;
                mRefreshText.setText(getResources().getString(
                        R.string.profile_control_device_scanning));
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mSwipeLayout.setRefreshing(true);
            }
        } else {
            Logger.i(TAG, "scanLeDevice = " + enable);
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
        mHandler = new Handler();

        runnable = new Runnable() {
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

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mLeScanDeviceListAdapter.getCount() > 0) {
                            Toast.makeText(ScanPageActivity.this,
                                    getResources().getString(R.string.device_add_message),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            findViewById(R.id.device_nofound).setVisibility(View.VISIBLE);   //Device Not Founf 顯示
                        }
                    }
                });

                mSwipeLayout.setRefreshing(false);
                scanLeDevice(false);
            }
        };

        mHandler.postDelayed(runnable, SCAN_PERIOD_TIMEOUT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
        } else {
            // Check which request we're responding to
            if (requestCode == REQUEST_ENABLE_BT) {

                // Make sure the request was successful
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this,
                            getResources().getString(
                                    R.string.device_bluetooth_on),
                            Toast.LENGTH_SHORT).show();
                            scanLeDevice(true);
                } else {
                    finish();
                }
            }
        }
    }

    public boolean checkBluetoothStatus() {
        /**
         * Ensures Blue tooth is enabled on the device. If Blue tooth is not
         * currently enabled, fire an intent to display a dialog asking the user
         * to grant permission to enable it.
         */
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        return true;
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
