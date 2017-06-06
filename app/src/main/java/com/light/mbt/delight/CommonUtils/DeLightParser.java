package com.light.mbt.delight.CommonUtils;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Class to parse the DeLight service related information
 */
public class DeLightParser {
    /**
     * Parsing the DeLight value from the characteristic
     *
     * @param characteristic
     * @return {@link String}
     */
    public static String getDeLightValue(BluetoothGattCharacteristic characteristic) {
        int FUNCTION_INDEX = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        int LAMP_INDEX = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 1);
        int LAMP_INTENSITY_INDEX = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 2);
        int TIMER_INDEX = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 3);
        int TIME1_INDEX = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 4);
        int TIME2_INDEX = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 5);
        return String.valueOf(FUNCTION_INDEX + "," + LAMP_INDEX + "," + LAMP_INTENSITY_INDEX + ","
                + TIMER_INDEX + "," + TIME1_INDEX + "," + TIME2_INDEX);
    }
}
