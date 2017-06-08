package com.light.mbt.delight.CommonUtils;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.light.mbt.delight.R;
import com.light.mbt.delight.bluetooth.BluetoothLeService;

import java.util.List;
import java.util.UUID;

/**
 * Fragment to display the Device information service
 */
public class DeviceInformationService{

    // GATT Service and Characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mReadCharacteristic;

    // Data view variables
    private static TextView mManufacturerName;
    private static TextView mModelName;
    private static TextView mSerialName;
    private static TextView mHardwareRevisionName;
    private static TextView mFirmwareRevisionName;
    private static TextView mSoftwareRevisionName;
    private static TextView mPnpId;
    private static TextView mSysId;
    private static TextView mRegulatoryCertificationDataList;

    // Flag for data set
    private static boolean mManufacturerSet = false;
    private static boolean mmModelNumberSet = false;
    private static boolean mSerialNumberSet = false;
    private static boolean mHardwareNumberSet = false;
    private static boolean mFirmwareNumberSet = false;
    private static boolean mSoftwareNumberSet = false;
    private static boolean mPnpidSet = false;
    private static boolean mRegulatoryCertificationDataListSet = false;
    private static boolean mSystemidSet = false;

    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    public static BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();

            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // Check Manufacturer Nmae
                if (extras.containsKey(Constants.EXTRA_MNS_VALUE)) {
                    String received_mns_data = intent
                            .getStringExtra(Constants.EXTRA_MNS_VALUE);
                    if (!received_mns_data.equalsIgnoreCase(" ")) {
                        if (!mManufacturerSet) {
                            mManufacturerSet = true;
                            displayManufactureName(received_mns_data);
                            prepareCharacteristics(UUIDDatabase.UUID_MODEL_NUMBER_STRING);
                        }
                    }

                }
                // Check Model number
                if (extras.containsKey(Constants.EXTRA_MONS_VALUE)) {
                    String received_mons_data = intent
                            .getStringExtra(Constants.EXTRA_MONS_VALUE);
                    if (!received_mons_data.equalsIgnoreCase(" ")) {
                        if (!mmModelNumberSet) {
                            mmModelNumberSet = true;
                            displayModelNumber(received_mons_data);
                            prepareCharacteristics(UUIDDatabase.UUID_SERIAL_NUMBER_STRING);
                        }
                    }
                }
                // Check Serial number
                if (extras.containsKey(Constants.EXTRA_SNS_VALUE)) {
                    String received_sns_data = intent
                            .getStringExtra(Constants.EXTRA_SNS_VALUE);
                    if (!received_sns_data.equalsIgnoreCase(" ")) {
                        if (!mSerialNumberSet) {
                            mSerialNumberSet = true;
                            displaySerialNumber(received_sns_data);
                            prepareCharacteristics(UUIDDatabase.UUID_HARDWARE_REVISION_STRING);
                        }
                    }
                }
                // Check Hardware Revision
                if (extras.containsKey(Constants.EXTRA_HRS_VALUE)) {
                    String received_hrs_data = intent
                            .getStringExtra(Constants.EXTRA_HRS_VALUE);
                    if (!received_hrs_data.equalsIgnoreCase(" ")) {
                        if (!mHardwareNumberSet) {
                            mHardwareNumberSet = true;
                            displayhardwareNumber(received_hrs_data);
                            prepareCharacteristics(UUIDDatabase.UUID_FIRMWARE_REVISION_STRING);
                        }
                    }
                }
                // check Firmware revision
                if (extras.containsKey(Constants.EXTRA_FRS_VALUE)) {
                    String received_frs_data = intent
                            .getStringExtra(Constants.EXTRA_FRS_VALUE);
                    if (!received_frs_data.equalsIgnoreCase(" ")) {
                        if (!mFirmwareNumberSet) {
                            mFirmwareNumberSet = true;
                            displayfirmwareNumber(received_frs_data);
                            prepareCharacteristics(UUIDDatabase.UUID_SOFTWARE_REVISION_STRING);
                        }
                    }
                }
                // check Software revision
                if (extras.containsKey(Constants.EXTRA_SRS_VALUE)) {
                    String received_srs_data = intent
                            .getStringExtra(Constants.EXTRA_SRS_VALUE);
                    if (!received_srs_data.equalsIgnoreCase(" ")) {
                        if (!mSoftwareNumberSet) {
                            mSoftwareNumberSet = true;
                            displaySoftwareNumber(received_srs_data);
                            prepareCharacteristics(UUIDDatabase.UUID_PNP_ID);
                        }
                    }
                }
                // Check PNP ID
                if (extras.containsKey(Constants.EXTRA_PNP_VALUE)) {
                    String received_pnpid = intent
                            .getStringExtra(Constants.EXTRA_PNP_VALUE);
                    if (!received_pnpid.equalsIgnoreCase(" ")) {
                        if (!mPnpidSet) {
                            mPnpidSet = true;
                            displayPnpId(received_pnpid);
                            prepareCharacteristics(UUIDDatabase.UUID_IEEE);
                        }
                    }
                }
                // Check IEEE
                if (extras.containsKey(Constants.EXTRA_RCDL_VALUE)) {
                    String received_rcdl_value = intent
                            .getStringExtra(Constants.EXTRA_RCDL_VALUE);
                    if (!received_rcdl_value.equalsIgnoreCase(" ")) {
                        if (!mRegulatoryCertificationDataListSet) {
                            mRegulatoryCertificationDataListSet = true;
                            displayRegulatoryData(received_rcdl_value);
                            prepareCharacteristics(UUIDDatabase.UUID_SYSTEM_ID);
                        }
                    }
                }
                // Check System ID set
               if (extras.containsKey(Constants.EXTRA_SID_VALUE)) {
                    String received_sid_value = intent
                            .getStringExtra(Constants.EXTRA_SID_VALUE);
                    if (!received_sid_value.equalsIgnoreCase(" ")) {
                        if (!mSystemidSet) {
                            displaySystemid(received_sid_value);
                            mReadCharacteristic = null;
                            mSystemidSet = true;
                        }
                    }
                }
            }
        }
    };

    public static void Init(BluetoothGattService service, View item) {
        mManufacturerName = (TextView) item.findViewById(R.id.div_manufacturer);
        mModelName= (TextView) item.findViewById(R.id.div_model);
        mSerialName = (TextView) item.findViewById(R.id.div_serial);
        mHardwareRevisionName = (TextView) item.findViewById(R.id.div_hardware);
        mFirmwareRevisionName = (TextView) item.findViewById(R.id.div_firmware);
        mSoftwareRevisionName = (TextView) item.findViewById(R.id.div_software);
        mSysId = (TextView) item.findViewById(R.id.div_system);
        mRegulatoryCertificationDataList = (TextView) item.findViewById(R.id.div_regulatory);
        mPnpId = (TextView) item.findViewById(R.id.div_pnp);
        mService = service;
        getGattData();
    }

    /**
     * Display RCDL Value
     *
     * @param received_rcdl_value
     */
    private static void displayRegulatoryData(String received_rcdl_value) {
        mRegulatoryCertificationDataList.setText(received_rcdl_value);

    }

    /**
     * Display SystemID
     *
     * @param received_sid_value
     */

    static void displaySystemid(String received_sid_value) {
        mSysId.setText(received_sid_value);

    }

    /**
     * Display PNPID
     *
     * @param received_pnpid
     */
    static void displayPnpId(String received_pnpid) {
        mPnpId.setText(received_pnpid);

    }

    /**
     * Display Software revision number
     *
     * @param received_srs_data
     */
    static void displaySoftwareNumber(String received_srs_data) {
        mSoftwareRevisionName.setText(received_srs_data);

    }

    /**
     * Display hardware revision number
     *
     * @param received_hrs_data
     */
    static void displayhardwareNumber(String received_hrs_data) {
        mHardwareRevisionName.setText(received_hrs_data);

    }

    /**
     * Display firmware revision number
     *
     * @param received_frs_data
     */
    static void displayfirmwareNumber(String received_frs_data) {
        mFirmwareRevisionName.setText(received_frs_data);

    }

    /**
     * Display serial number
     *
     * @param received_sns_data
     */
    static void displaySerialNumber(String received_sns_data) {
        mSerialName.setText(received_sns_data);

    }

    /**
     * Display model number
     *
     * @param received_mons_data
     */
    static void displayModelNumber(String received_mons_data) {
        mModelName.setText(received_mons_data);


    }

    /**
     * Display manufacture name
     *
     * @param received_mns_data
     */

    static void displayManufactureName(String received_mns_data) {
        mManufacturerName.setText(received_mns_data);
    }

    /**
     * Prepares Characteristics
     *
     */
    private static void prepareCharacteristics(UUID characteristic) {
        List<BluetoothGattCharacteristic> mGatt = mService
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : mGatt) {
            UUID uuidchara = gattCharacteristic.getUuid();
            if (uuidchara.equals(characteristic)) {
                Logger.i("Characteristic " + uuidchara);
                prepareBroadcastDataRead(gattCharacteristic);
            }
        }
    }

    /**
     * Prepare Broadcast receiver to broadcast read characteristics
     *
     * @param gattCharacteristic
     */

    static void prepareBroadcastDataRead(
            BluetoothGattCharacteristic gattCharacteristic) {
        if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            BluetoothLeService.readCharacteristic(gattCharacteristic);
        }
    }

    /**
     * Flag up default
     */
    private static void makeDefaultBooleans() {
        mManufacturerSet = false;
        mmModelNumberSet = false;
        mSerialNumberSet = false;
        mHardwareNumberSet = false;
        mFirmwareNumberSet = false;
        mSoftwareNumberSet = false;
        mPnpidSet = false;
        mSystemidSet = false;
        mRegulatoryCertificationDataListSet = false;
    }

    /**
     * Method to get required characteristics from service
     */
    private static void getGattData() {
        makeDefaultBooleans();
        List<BluetoothGattCharacteristic> gattCharacteristics = mService
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            if (uuidchara.equalsIgnoreCase(GattAttributes.MANUFACTURER_NAME_STRING)
                    || uuidchara.equalsIgnoreCase(GattAttributes.SERIAL_NUMBER_STRING)
                    || uuidchara.equalsIgnoreCase(GattAttributes.FIRMWARE_REVISION_STRING)
                    || uuidchara.equalsIgnoreCase(GattAttributes.HARDWARE_REVISION_STRING)
                    || uuidchara.equalsIgnoreCase(GattAttributes.SOFTWARE_REVISION_STRING)
                    || uuidchara.equalsIgnoreCase(GattAttributes.MANUFACTURER_NAME_STRING)
                    || uuidchara.equalsIgnoreCase(GattAttributes.PNP_ID)
                    || uuidchara.equalsIgnoreCase(GattAttributes.IEEE)
                    || uuidchara.equalsIgnoreCase(GattAttributes.SYSTEM_ID)) {
                Logger.i("Characteristic div" + uuidchara);
                prepareBroadcastDataRead(gattCharacteristic);
                break;
            }
        }
    }

}
