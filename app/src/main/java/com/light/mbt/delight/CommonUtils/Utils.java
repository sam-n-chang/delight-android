package com.light.mbt.delight.CommonUtils;

import android.R.integer;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.light.mbt.delight.CommonUtils.gson.Gson;
import com.light.mbt.delight.CommonUtils.gson.reflect.TypeToken;
import com.light.mbt.delight.ListAdapters.DeviceList;
import com.light.mbt.delight.R;
import com.light.mbt.delight.bluetooth.BluetoothLeService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.MODE_PRIVATE;

/**
 * Class for commonly used methods in the project
 */
public class Utils {
    private final static String TAG = " Delight / " + Utils.class.getSimpleName();

    // Shared preference constant
    private static final String SHARED_PREF_NAME = "Delight Shared Preference";
    private static ProgressDialog mProgressDialog;
    private static Timer mTimer;

    /**
     * Returns the manufacture name from the given characteristic
     *
     * @param characteristic
     * @return manfacture_name_string
     */
    public static String getManufacturerNameString(
            BluetoothGattCharacteristic characteristic) {
        String manfacture_name_string = characteristic.getStringValue(0);
        return manfacture_name_string;
    }

    /**
     * Returns the model number from the given characteristic
     *
     * @param characteristic
     * @return model_name_string
     */

    public static String getModelNumberString(
            BluetoothGattCharacteristic characteristic) {
        String model_name_string = characteristic.getStringValue(0);

        return model_name_string;
    }

    /**
     * Returns the serial number from the given characteristic
     *
     * @param characteristic
     * @return serial_number_string
     */
    public static String getSerialNumberString(
            BluetoothGattCharacteristic characteristic) {
        String serial_number_string = characteristic.getStringValue(0);

        return serial_number_string;
    }

    /**
     * Returns the hardware number from the given characteristic
     *
     * @param characteristic
     * @return hardware_revision_name_string
     */
    public static String getHardwareRevisionString(
            BluetoothGattCharacteristic characteristic) {
        String hardware_revision_name_string = characteristic.getStringValue(0);

        return hardware_revision_name_string;
    }

    /**
     * Returns the Firmware number from the given characteristic
     *
     * @param characteristic
     * @return hardware_revision_name_string
     */
    public static String getFirmwareRevisionString(
            BluetoothGattCharacteristic characteristic) {
        String firmware_revision_name_string = characteristic.getStringValue(0);

        return firmware_revision_name_string;
    }

    /**
     * Returns the software revision number from the given characteristic
     *
     * @param characteristic
     * @return hardware_revision_name_string
     */
    public static String getSoftwareRevisionString(
            BluetoothGattCharacteristic characteristic) {
        String hardware_revision_name_string = characteristic.getStringValue(0);

        return hardware_revision_name_string;
    }

    /**
     * Returns the PNP ID from the given characteristic
     *
     * @param characteristic
     * @return {@link String}
     */
    public static String getPNPID(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        if (data != null && data.length > 0) {
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
        }

        return String.valueOf(stringBuilder);
    }

    /**
     * Returns the SystemID from the given characteristic
     *
     * @param characteristic
     * @return {@link String}
     */
    public static String getSYSID(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        if (data != null && data.length > 0) {
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
        }

        return String.valueOf(stringBuilder);
    }

    /**
     * Adding the necessary INtent filters for Broadcast receivers
     *
     * @return {@link IntentFilter}
     */
    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED_CAROUSEL);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_ERROR);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_SUCCESS);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_FAILED);
        intentFilter.addAction(BluetoothLeService.ACTION_PAIR_REQUEST);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.EXTRA_BOND_STATE);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_COMPLETED);
        return intentFilter;
    }

    public static String ByteArraytoHex(byte[] bytes) {
        if (bytes != null) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString();
        }
        return "";
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            s = "0" + s;
            len++;
        }

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] hexlonggToByteArray(long s) {
        //String strHEX = Long.toHexString(s);
        String hex = String.format("%04X", s);

        return hexStringToByteArray(hex);
    }

    public static String getMSB(String string) {
        StringBuilder msbString = new StringBuilder();

        for (int i = string.length(); i > 0; i -= 2) {
            String str = string.substring(i - 2, i);
            msbString.append(str);
        }
        return msbString.toString();
    }

    /**
     * Converting the Byte to binary
     *
     * @param bytes
     * @return {@link String}
     */
    public static String BytetoBinary(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
        for (int i = 0; i < Byte.SIZE * bytes.length; i++)
            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0'
                    : '1');
        return sb.toString();
    }

    /**
     * Method to convert hex to byteArray
     */
    public static byte[] convertingTobyteArray(String result) {
        String[] splited = result.split("\\s+");
        byte[] valueByte = new byte[splited.length];
        for (int i = 0; i < splited.length; i++) {
            if (splited[i].length() > 2) {
                String trimmedByte = splited[i].split("x")[1];
                valueByte[i] = (byte) convertstringtobyte(trimmedByte);
            }

        }
        return valueByte;
    }


    /**
     * Convert the string to byte
     *
     * @param string
     * @return
     */
    private static int convertstringtobyte(String string) {
        return Integer.parseInt(string, 16);
    }

    public static String byteToASCII(byte[] array) {

        StringBuffer sb = new StringBuffer();
        for (byte byteChar : array) {
            if (byteChar >= 32 && byteChar < 127) {
                sb.append(String.format("%c", byteChar));
            } else {
                sb.append(String.format("%d ", byteChar & 0xFF)); // to convert
                // >127 to
                // positive
                // value
            }
        }
        return sb.toString();
    }

    /**
     * Returns the battery level information from the characteristics
     *
     * @param characteristics
     * @return {@link String}
     */
    public static String getBatteryLevel(
            BluetoothGattCharacteristic characteristics) {
        int battery_level = characteristics.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        return String.valueOf(battery_level);
    }

    /**
     * Returns the Alert level information from the characteristics
     *
     * @param characteristics
     * @return {@link String}
     */
    public static String getAlertLevel(
            BluetoothGattCharacteristic characteristics) {
        int alert_level = characteristics.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        return String.valueOf(alert_level);
    }

    /**
     * Returns the Transmission power information from the characteristic
     *
     * @param characteristics
     * @return {@link integer}
     */
    public static int getTransmissionPower(
            BluetoothGattCharacteristic characteristics) {
        int power_level = characteristics.getIntValue(
                BluetoothGattCharacteristic.FORMAT_SINT8, 0);
        return power_level;
    }


    /**
     * Returns the Date from the long milliseconds
     *
     * @param date in millis
     * @return {@link String}
     */
    public static String GetDateFromLong(long date) {
        Date currentDate = new Date(date);
        DateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
        //formatted value of current Date
        // System.out.println("Milliseconds to Date: " + formatter.format(currentDate));
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);
        //System.out.println("Milliseconds to Date using Calendar:"
        //        + formatter.format(cal.getTime()));
        return currentDate.toString();

    }

    /**
     * Get the data from milliseconds
     *
     * @return {@link String}
     */
    public static String GetDateFromMilliseconds() {
        DateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
        Calendar calendar = Calendar.getInstance();
        return formatter.format(calendar.getTime());

    }

    /**
     * Get the date
     *
     * @return {@link String}
     */
    public static String GetDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
        Calendar calendar = Calendar.getInstance();
        return formatter.format(calendar.getTime());

    }

    /**
     * Get the time in seconds
     *
     * @return {@link String}
     */
    public static int getTimeInSeconds() {
        int seconds = (int) System.currentTimeMillis();
        return seconds;
    }

    /**
     * Get the seven days before date
     *
     * @return {@link String}
     */

    public static String GetDateSevenDaysBack() {
        DateFormat formatter = new SimpleDateFormat("dd_MMM_yyyy");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        return formatter.format(calendar.getTime());

    }

    /**
     * Get the time from milliseconds
     *
     * @return {@link String}
     */
    public static String GetTimeFromMilliseconds() {
        DateFormat formatter = new SimpleDateFormat("HH:mm ss SSS");
        Calendar calendar = Calendar.getInstance();
        return formatter.format(calendar.getTime());

    }

    /**
     * Get time and date
     *
     * @return {@link String}
     */

    public static String GetTimeandDate() {
        DateFormat formatter = new SimpleDateFormat("[dd-MMM-yyyy|HH:mm:ss]");
        Calendar calendar = Calendar.getInstance();
        return formatter.format(calendar.getTime());

    }

    /**
     * Get time and date without datalogger format
     *
     * @return {@link String}
     */

    public static String GetTimeandDateUpdate() {
        DateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        return formatter.format(calendar.getTime());

    }

    /**
     * Setting the shared preference with values provided as parameters
     *
     * @param context
     * @param key
     * @param value
     */
    public static final void setStringSharedPreference(Context context,
                                                       String key, String value) {
        SharedPreferences goaPref = context.getSharedPreferences(
                SHARED_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = goaPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Returning the stored values in the shared preference with values provided
     * as parameters
     *
     * @param context
     * @param key
     * @return
     */
    public static final String getStringSharedPreference(Context context,
                                                         String key) {
        if (context != null) {

            SharedPreferences Pref = context.getSharedPreferences(
                    SHARED_PREF_NAME, MODE_PRIVATE);
            String value = Pref.getString(key, "");
            return value;

        } else {
            return "";
        }
    }

    /**
     * Setting the shared preference with values provided as parameters
     *
     * @param context
     * @param key
     * @param value
     */
    public static final void setIntSharedPreference(Context context,
                                                    String key, int value) {
        SharedPreferences goaPref = context.getSharedPreferences(
                SHARED_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = goaPref.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Returning the stored values in the shared preference with values provided
     * as parameters
     *
     * @param context
     * @param key
     * @return
     */
    public static final int getIntSharedPreference(Context context,
                                                   String key) {
        if (context != null) {

            SharedPreferences Pref = context.getSharedPreferences(
                    SHARED_PREF_NAME, MODE_PRIVATE);
            int value = Pref.getInt(key, 0);
            return value;

        } else {
            return 0;
        }
    }

    /**
     * Setting the shared preference with values provided as parameters
     *
     * @param context
     * @param key
     * @param value
     */
    public static final void setLongSharedPreference(Context context,
                                                     String key, long value) {
        SharedPreferences goaPref = context.getSharedPreferences(
                SHARED_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = goaPref.edit();
        editor.putLong(key, value);
        editor.apply();
    }


    /**
     * Returning the stored values in the shared preference with values provided
     * as parameters
     *
     * @param context
     * @param key
     * @return
     */
    public static final long getLongSharedPreference(Context context,
                                                     String key) {
        if (context != null) {

            SharedPreferences Pref = context.getSharedPreferences(
                    SHARED_PREF_NAME, MODE_PRIVATE);
            Long value = Pref.getLong(key, 0);
            return value;

        } else {
            return 0;
        }
    }

    public static final void setDeviceListArraySharedPreference(Context context,
                                                                String key, DeviceList mDeviceList, boolean remove) {
        ArrayList<DeviceList> deviceList = new ArrayList<DeviceList>();
        boolean FirstAdd = true;
        deviceList = getDeviceListArraySharedPreference(context, key);

        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getDeviceAddress().equalsIgnoreCase(mDeviceList.getDeviceAddress())) {
                //如果之前有此Device 就先移除再加入
                deviceList.remove(i);
                if (remove == false)
                    deviceList.add(i, mDeviceList);

                FirstAdd = false;
                break;
            }
        }

        if (FirstAdd == true) {
            mDeviceList.setName(mDeviceList.getName().toString() + "_" + (deviceList.size() + 1));
            deviceList.add(mDeviceList);
        }

        SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE).edit();
        Gson gson = new Gson();
        String json = gson.toJson(deviceList);
        Logger.d(TAG, "saved json is " + json);
        editor.putString(key, json);
        editor.commit();
    }

    public static final ArrayList<DeviceList> getDeviceListArraySharedPreference(Context context,
                                                                                 String key) {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        String json = preferences.getString(key, null);
        ArrayList<DeviceList> deviceList = new ArrayList<DeviceList>();

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<DeviceList>>() {
            }.getType();
            deviceList = gson.fromJson(json, type);
        /*    for (int i = 0; i < deviceList.size(); i++) {
                            Logger.d(TAG, deviceList.get(i).getName() + ":" + deviceList.get(i).getDeviceName() + "," + deviceList.get(i).getDeviceAddress());
                    }*/
        }
        return deviceList;
    }

    public static final void setBooleanSharedPreference(Context context,
                                                        String key, boolean value) {
        SharedPreferences Preference = context.getSharedPreferences(
                SHARED_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = Preference.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static final boolean getBooleanSharedPreference(Context context,
                                                           String key) {
        boolean value;
        SharedPreferences Preference = context.getSharedPreferences(
                SHARED_PREF_NAME, MODE_PRIVATE);
        value = Preference.getBoolean(key, false);
        return value;
    }

    public static final void setInitialBooleanSharedPreference(Context context,
                                                               String key, boolean value) {
        SharedPreferences Preference = context.getSharedPreferences(
                SHARED_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = Preference.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static final boolean getInitialBooleanSharedPreference(Context context,
                                                                  String key) {
        boolean value;
        SharedPreferences Preference = context.getSharedPreferences(
                SHARED_PREF_NAME, MODE_PRIVATE);
        value = Preference.getBoolean(key, true);
        return value;
    }

    public static final boolean ifContainsSharedPreference(Context context,
                                                           String key) {
        boolean value;
        SharedPreferences Preference = context.getSharedPreferences(
                SHARED_PREF_NAME, MODE_PRIVATE);
        value = Preference.contains(key);
        return value;
    }

    /**
     * Take the screen shot of the device
     *
     * @param view
     */
    public static void screenShotMethod(View view) {
        Bitmap bitmap;
        if (view != null) {
            View v1 = view;
            v1.setDrawingCacheEnabled(true);
            v1.buildDrawingCache(true);
            bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + "CySmart" + File.separator + "file.jpg");
            try {
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(bytes.toByteArray());
                fo.flush();
                fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Method to detect whether the device is phone or tablet
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    //    /**
    //     * Alert dialog to display when the GATT Server is disconnected from the
    //     * client
    //     *
    //     * @param context
    //     */
    //
    //    public static void connectionLostalertbox(final Activity context) {
    //        if(BluetoothLeService.getConnectionState()==0){
    //            //Disconnected
    //            AlertDialog alert;
    //            AlertDialog.Builder builder = new AlertDialog.Builder(context);
    //            builder.setMessage(
    //                    context.getResources().getString(
    //                            R.string.alert_message_reconnect))
    //                    .setCancelable(false)
    //                    .setTitle(context.getResources().getString(R.string.app_name))
    //                    .setPositiveButton(
    //                            context.getResources().getString(
    //                                    R.string.alert_message_exit_ok),
    //                            new DialogInterface.OnClickListener() {
    //                                public void onClick(DialogInterface dialog, int id) {
    //                                    Intent intentActivity = context.getIntent();
    //                                    context.finish();
    //                                    context.overridePendingTransition(
    //                                            R.anim.slide_left, R.anim.push_left);
    //                                    context.startActivity(intentActivity);
    //                                    context.overridePendingTransition(
    //                                            R.anim.slide_right, R.anim.push_right);
    //                                }
    //                            });
    //            alert = builder.create();
    //            alert.setCanceledOnTouchOutside(false);
    //            if (!context.isDestroyed()&&context!=null)
    //                alert.show();
    //
    //
    //        }
    //    }

    public static void bondingProgressDialog(final Activity context, ProgressDialog pDialog,
                                             boolean status) {
        mProgressDialog = pDialog;
        if (status) {
            mProgressDialog.setTitle(context.getResources().getString(
                    R.string.alert_message_bonding_title));
            mProgressDialog.setMessage((context.getResources().getString(
                    R.string.alert_message_bonding_message)));
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            mTimer = setDialogTimer();

        } else {
            mProgressDialog.dismiss();
        }

    }

    public static Timer setDialogTimer() {
        Logger.e(TAG, "Started Timer");
        long delayInMillis = 20000;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mProgressDialog != null)
                    mProgressDialog.dismiss();
            }
        }, delayInMillis);
        return timer;
    }

    public static void stopDialogTimer() {
        if (mTimer != null) {
            Logger.e(TAG, "Stopped Timer");
            mTimer.cancel();
        }
    }

    /**
     * Setting up the action bar with values provided as parameters
     *
     * @param context
     * @param title
     */
    public static void setUpActionBar(Activity context, String title) {
        ActionBar actionBar = context.getActionBar();
        actionBar.setIcon(new ColorDrawable(context.getResources().getColor(
                android.R.color.transparent)));
        actionBar.setTitle(title);
    }

    /**
     * Check whether Internet connection is enabled on the device
     *
     * @param context
     * @return
     */
    public static final boolean checkNetwork(Context context) {
        if (context != null) {
            boolean result = true;
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager
                    .getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
                result = false;
            }
            return result;
        } else {
            return false;
        }
    }

    public void toast(Activity context, String text) {
        Toast.makeText(context, text.toString(), Toast.LENGTH_LONG).show();
    }


    /**
     * Hides the input method.
     *
     * @param context context
     * @param view    The currently focused view
     * @return success or not.
     */
    public static boolean hideInputMethod(Context context, View view) {
        if (context == null || view == null) {
            return false;
        }

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            return imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        return false;
    }

    /**
     * Show the input method.
     *
     * @param context context
     * @param view    The currently focused view, which would like to receive soft keyboard input
     * @return success or not.
     */
    public static boolean showInputMethod(Context context, View view) {
        if (context == null || view == null) {
            return false;
        }

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            return imm.showSoftInput(view, 0);
        }

        return false;
    }

    public static float pixelToDp(Context context, float val) {
        float density = context.getResources().getDisplayMetrics().density;
        return val * density;
    }

    public static int dipToPx(Context context, int dipValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, context
                .getResources().getDisplayMetrics());
    }

    public static String getHashedFileName(String url) {
        if (url == null || url.endsWith("/")) {
            return null;
        }

        String suffix = getSuffix(url);
        StringBuilder sb = null;

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] dstbytes = digest.digest(url.getBytes("UTF-8")); // GMaFroid uses UTF-16LE
            sb = new StringBuilder();
            for (int i = 0; i < dstbytes.length; i++) {
                sb.append(Integer.toHexString(dstbytes[i] & 0xff));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null != sb && null != suffix) {
            return sb.toString() + "." + suffix;
        }

        return null;
    }

    private static String getSuffix(String fileName) {
        int dot_point = fileName.lastIndexOf(".");
        int sl_point = fileName.lastIndexOf("/");
        if (dot_point < sl_point) {
            return "";
        }

        if (dot_point != -1) {
            return fileName.substring(dot_point + 1);
        }

        return null;
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param context The application's environment.
     * @param intent  The Intent action to check for availability.
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();

        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return list.size() > 0;
    }

}
