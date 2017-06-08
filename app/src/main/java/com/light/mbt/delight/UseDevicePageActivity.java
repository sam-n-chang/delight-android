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
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.light.mbt.delight.CommonUtils.Logger;
import com.light.mbt.delight.CommonUtils.Utils;
import com.light.mbt.delight.ListAdapters.DeviceList;
import com.light.mbt.delight.ListAdapters.LeUseDeviceListAdapter;
import com.light.mbt.delight.bluetooth.BluetoothLeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class UseDevicePageActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final static String TAG = " Delight / " + UseDevicePageActivity.class.getSimpleName();
    private static final String BLUETOOTH_DEVICE_NAME = "Delight";  //限制搜尋欄牙的名稱
    //Delay Time out
    private static final long DELAY_PERIOD = 500;
    // Connection time out after 10 seconds.
    private static final long CONNECTION_TIMEOUT = 10000;
    // Stops scanning after 2 seconds.
    private static final long SCAN_PERIOD_TIMEOUT = 1000;
    // Activity request constant
    private static final int REQUEST_ENABLE_BT = 1;

    private Timer mConnectTimer;
    private boolean mConnectTimerON = false;

    // Devices list variables
    public static LeUseDeviceListAdapter mLeDeviceListAdapter;

    //GUI elements
    private ListView mProfileListView;
    private ProgressDialog mProgressdialog;
    private AlertDialog mAlert;

    // device details
    public static String mDeviceName = "name";
    public static String mDeviceAddress = "address";

    private DeviceList mDeviceList;
    private ArrayList<DeviceList> mDeviceListArray;
    private boolean mScanning = false, mSelectItem = false, mScanDeviceFind = true;
    private boolean BLUETOOTH_STATUS_FLAG = true;
    public HashMap<String, BluetoothDevice> mScanDevice = new HashMap<String, BluetoothDevice>();

    private EditText editText;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    //Bluetooth adapter
    private static BluetoothAdapter mBluetoothAdapter;

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
                    String DeviceAddress = device.getAddress().toString();
                    BluetoothDevice mDevice = mScanDevice.get(DeviceAddress);
                    if (device.getName().indexOf(BLUETOOTH_DEVICE_NAME) > -1 && mDevice == null) {  //以名稱限制搜尋欄牙
                        mScanDevice.put(DeviceAddress, device);
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
                mProgressdialog.dismiss();

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
                    Toast.makeText(UseDevicePageActivity.this,
                            getResources().getString(R.string.alert_message_bluetooth_disconnect),
                            Toast.LENGTH_SHORT).show();
                }
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
        setContentView(R.layout.activity_use_device);

        checkBleSupportAndInitialize();

        initDrawer();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mProfileListView = (ListView) findViewById(R.id.listView_profiles);
        mLeDeviceListAdapter = new LeUseDeviceListAdapter(UseDevicePageActivity.this);
        mProfileListView.setAdapter(mLeDeviceListAdapter);
        mProfileListView.setTextFilterEnabled(true);
        mProfileListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        mProgressdialog = new ProgressDialog(this);
        mProgressdialog.setCancelable(false);

        //設定顯示開啟藍芽視窗
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

        mDeviceListArray = Utils.getDeviceListArraySharedPreference(UseDevicePageActivity.this, "DEVICE_LIST");
        if (mDeviceListArray.size() > 0) {
            for (DeviceList mDeviceList : mDeviceListArray) {
                mLeDeviceListAdapter.addDevice(mDeviceList);
            }
        }

        Intent gattServiceIntent = new Intent(getApplicationContext(),
                BluetoothLeService.class);
        startService(gattServiceIntent);
        Logger.i(TAG, "Start Service");

        mProfileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (mLeDeviceListAdapter.getCount() > 0) {
                    if (mSelectItem == false) {
                        mScanDeviceFind = false;
                        scanLeDevice(false);
                        mDeviceName = mLeDeviceListAdapter.getDevice(position).getDeviceName();
                        mDeviceAddress = mLeDeviceListAdapter.getDevice(position).getDeviceAddress();
                        if (mLeDeviceListAdapter.DeviceReady.get(mDeviceAddress) == true) {
                            mDeviceList = new DeviceList();
                            mDeviceList = mLeDeviceListAdapter.mDeviceList.get(mDeviceAddress);
                            connectDevice(true);
                        }
                    } else {
                        ResetDeviceListSelectItem();
                        initDrawer();
                    }
                }
            }
        });

        mProfileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int arg2, long arg3) {
                // TODO Auto-generated method stub
                Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                //toolbar.setTitle("MBT Delight");  //設置標題
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                setSupportActionBar(toolbar);

                mDeviceList = new DeviceList();
                mDeviceList = mLeDeviceListAdapter.getDevice(arg2);

                mLeDeviceListAdapter.setSelectedItem(arg2);     //設定選擇的Item
                mLeDeviceListAdapter.notifyDataSetChanged();    //更新DeviceList
                mSelectItem = true;
                invalidateOptionsMenu();    //重新载入Menu
                Logger.e(TAG, "setOnItemLongClickListener");
                return true;
            }
        });

    }

    private void initDrawer() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.products_name);  //設置標題
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (!mScanning) {
                startScanTimer();
                mScanning = true;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                Logger.i(TAG, "scanLeDevice = " + enable);
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            Logger.i(TAG, "scanLeDevice = " + enable);
            if (mDeviceListArray.size() > 0 && mScanDeviceFind == true) {
                for (DeviceList mDeviceList : mDeviceListArray) {
                    String DeviceAddress = mDeviceList.getDeviceAddress().toString();
                    BluetoothDevice mDevice = mScanDevice.get(DeviceAddress);
                    Boolean DeviceReady = false;
                    if (mDevice != null)
                        DeviceReady = true;

                    mLeDeviceListAdapter.setDeviceReadyItem(DeviceAddress, DeviceReady);
                }

                mLeDeviceListAdapter.notifyDataSetChanged();
                mScanDevice.clear();
            }
            if (mScanDeviceFind == true)
                scanLeDevice(true);
        }
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

    public void startScanTimer() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                scanLeDevice(false);
            }
        }, SCAN_PERIOD_TIMEOUT);
    }

    private void connectDevice(boolean isFirstConnect) {
        int mConnectionState = BluetoothLeService.getConnectionState();
        Logger.i(TAG, "BLE Connection State---->" + BluetoothLeService.getConnectionState());
        // Get the connection status of the device
        if (mConnectionState == BluetoothLeService.STATE_DISCONNECTED) {
            Logger.v(TAG, "BLE DISCONNECTED STATE, CONNECTED To : " + mDeviceName + "|" + mDeviceAddress);
            // Disconnected,so connect
            BluetoothLeService.connect(mDeviceAddress, mDeviceName, UseDevicePageActivity.this);
            showConnectAlertMessage(mDeviceName, mDeviceAddress);
        } else {
            Logger.v(TAG, "BLE OTHER STAT");
            // Connecting to some devices,so disconnect and then connect
             BluetoothLeService.disconnect();
            Handler delayHandler = new Handler();
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BluetoothLeService.connect(mDeviceAddress, mDeviceName, UseDevicePageActivity.this);
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

    private void gotoControlActivity() {

        final Intent intent = new Intent(this, ControlPageActivity.class);
        intent.putExtra(ControlPageActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(ControlPageActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        Bundle bundle = new Bundle();
        bundle.putSerializable("DeviceList", mDeviceList);  //將DeviceList 傳到ControlPageActivity
        intent.putExtras(bundle);
        //finish();
        overridePendingTransition(R.anim.slide_right, R.anim.push_right);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_left, R.anim.push_left);
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
                        Toast.makeText(UseDevicePageActivity.this,
                                getResources().getString(R.string.profile_cannot_connect_message),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, CONNECTION_TIMEOUT);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // land do nothing is ok
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // port do nothing is ok
        }
    }

    @Override
    public void onStart() {
        Logger.i(TAG, "UseDevice onStart");

        super.onStart();
    }

    @Override
    public void onResume() {
        Logger.i(TAG, "UseDevice onResume");
        Logger.i(TAG, "BLE Connection State---->" + BluetoothLeService.getConnectionState());

        if (checkBluetoothStatus()) {
            mScanDeviceFind = true;
            scanLeDevice(true);
        }

        BLUETOOTH_STATUS_FLAG = true;
        Logger.i(TAG, "Registering receiver in Profile scannng");
        registerReceiver(mGattConnectReceiver,
                Utils.makeGattUpdateIntentFilter());

        super.onResume();
    }

    @Override
    public void onPause() {
        Logger.i(TAG, "UseDevice onPause");
        if (mProgressdialog != null && mProgressdialog.isShowing()) {
            mProgressdialog.dismiss();
        }

        Logger.i(TAG, "UN Registering receiver in Profile scannng");
        unregisterReceiver(mGattConnectReceiver);
        BLUETOOTH_STATUS_FLAG = false;
        scanLeDevice(false);
        mScanDeviceFind = false;

        Intent gattServiceIntent = new Intent(getApplicationContext(),
                BluetoothLeService.class);
        stopService(gattServiceIntent);
        Logger.i(TAG, "stopService");

        super.onPause();
    }

    @Override
    public void onDestroy() {
        Logger.i(TAG, "UseDevice onDestroy");
        mScanDeviceFind = false;

        super.onDestroy();
    }

    /**
     * Handling the back pressed actions
     */
    @Override
    public void onBackPressed() {
        Logger.i(TAG, "UseDevice onBackPressed");
        if (mSelectItem == false) {
            mScanDeviceFind = false;
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        } else {
            ResetDeviceListSelectItem();
            initDrawer();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.home_main, menu); //menu Setting not use
        getMenuInflater().inflate(R.menu.home_main, menu);
        menu.findItem(R.id.action_info).setVisible(false);
        if (mLeDeviceListAdapter.getSelectedItem() != -1) {
            menu.findItem(R.id.action_edit).setVisible(true);
            menu.findItem(R.id.action_del).setVisible(true);
            menu.findItem(R.id.action_add).setVisible(false);
        } else {
            menu.findItem(R.id.action_edit).setVisible(false);
            menu.findItem(R.id.action_del).setVisible(false);
            menu.findItem(R.id.action_add).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_add:
                scanLeDevice(false);
                mScanDeviceFind = false;
                Logger.i(TAG, "BLE DISCONNECT---->" + BluetoothLeService.getConnectionState());
                Intent intent = new Intent(this, ScanPageActivity.class);
                //finish();
                overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                return true;
            case R.id.action_edit:
                customDialog();
                return true;
            case R.id.action_del:
                Utils.setDeviceListArraySharedPreference(UseDevicePageActivity.this, "DEVICE_LIST", mDeviceList, true);
                int SelectItem = mLeDeviceListAdapter.getSelectedItem();
                mLeDeviceListAdapter.removeDevice(SelectItem);
                ResetDeviceListSelectItem();
                initDrawer();
                return true;
            case android.R.id.home:
                ResetDeviceListSelectItem();
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
        }
        startActivity(intent);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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

    private void customDialog() {
        final View item = LayoutInflater.from(UseDevicePageActivity.this).inflate(R.layout.rename, null);
        editText = (EditText) item.findViewById(R.id.edittext);
        editText.setText(mDeviceList.getName());

        new AlertDialog.Builder(UseDevicePageActivity.this)
                .setTitle(R.string.alertdialog_rename_title)
                .setMessage(R.string.alertdialog_rename_meggage)
                .setView(item)
                .setPositiveButton(R.string.alertdialog_rename_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String DeviceName = editText.getText().toString();
                        if (!DeviceName.equals("")) {
                            mDeviceList.setName(DeviceName);
                            String Address = mDeviceList.getDeviceAddress();
                            mLeDeviceListAdapter.mDeviceList.remove(Address);
                            mLeDeviceListAdapter.mDeviceList.put(Address, mDeviceList);
                            Utils.setDeviceListArraySharedPreference(UseDevicePageActivity.this, "DEVICE_LIST", mDeviceList, false);
                        }
                        ResetDeviceListSelectItem();
                    }
                })
                .setNegativeButton(R.string.alertdialog_rename_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ResetDeviceListSelectItem();
                    }
                })
                .show();
    }

    void ResetDeviceListSelectItem() {
        mLeDeviceListAdapter.setSelectedItem(-1);
        mLeDeviceListAdapter.notifyDataSetChanged();
        mSelectItem = false;
        invalidateOptionsMenu();    //重新载入Menu
     }

    public void cleanReName(View view) {
        Logger.i(TAG, editText.getText().toString());
        editText.setText("");
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

    //自動隱藏虛擬按鍵
    private void hideSystemNavigationBar() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View view = this.getWindow().getDecorView();
            view.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
}
