/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.light.mbt.delight.CommonUtils;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    private static HashMap<String, String> descriptorAttributes = new HashMap<String, String>();

    /**
     * Services
     */
    public static final String DELIGHT_LAMP_SERVICE = "0000cbbb-0000-1000-8000-00805f9b34fb";
    public static final String DELIGHT_LAMP_SERVICE_CUSTOM = "0003cbbb-0000-1000-8000-00805f9b0131";
    public static final String SERIAL_DATA_SERVIC = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String DEVICE_INFORMATION_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb";

    /**
     * DELIGHT characteristics
     */
    public static final String DELIGHT_LAMP = "0000cbb1-0000-1000-8000-00805f9b34fb";
    public static final String DELIGHT_LAMP_CUSTOM = "0003cbb1-0000-1000-8000-00805f9b0131";

    /**
     * SERIAL_DATA characteristics
     */
    public static final String SERIAL_DATA = "0000ffe1-0000-1000-8000-00805f9b34fb";

    /**
     * Device information characteristics
     */
    public static final String SYSTEM_ID = "00002a23-0000-1000-8000-00805f9b34fb";
    public static final String MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
    public static final String SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb";
    public static final String FIRMWARE_REVISION_STRING = "00002a26-0000-1000-8000-00805f9b34fb";
    public static final String HARDWARE_REVISION_STRING = "00002a27-0000-1000-8000-00805f9b34fb";
    public static final String SOFTWARE_REVISION_STRING = "00002a28-0000-1000-8000-00805f9b34fb";
    public static final String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
    public static final String PNP_ID = "00002a50-0000-1000-8000-00805f9b34fb";
    public static final String IEEE = "00002a2a-0000-1000-8000-00805f9b34fb";

    /**
     * Descriptor UUID's
     */
    public static final String CHARACTERISTIC_EXTENDED_PROPERTIES = "00002900-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_USER_DESCRIPTION = "00002901-0000-1000-8000-00805f9b34fb";
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String SERVER_CHARACTERISTIC_CONFIGURATION = "00002903-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_PRESENTATION_FORMAT = "00002904-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_AGGREGATE_FORMAT = "00002905-0000-1000-8000-00805f9b34fb";
    public static final String VALID_RANGE = "00002906-0000-1000-8000-00805f9b34fb";
    public static final String ENVIRONMENTAL_SENSING_CONFIGURATION = "0000290B-0000-1000-8000-00805f9b34fb";
    public static final String ENVIRONMENTAL_SENSING_MEASUREMENT = "0000290C-0000-1000-8000-00805f9b34fb";
    public static final String ENVIRONMENTAL_SENSING_TRIGGER_SETTING = "0000290D-0000-1000-8000-00805f9b34fb";

    static {
        // DeLight Services.
        attributes.put(DELIGHT_LAMP_SERVICE, "DeLight Service");
        attributes.put(DELIGHT_LAMP_SERVICE_CUSTOM, "DeLight Service");

        // DeLight Lamp Characteristics.
        attributes.put(DELIGHT_LAMP, "DeLight Lamp");
        attributes.put(DELIGHT_LAMP_CUSTOM, "DeLight Lamp CUSTOM");

        attributes.put(DEVICE_INFORMATION_SERVICE, "Device Information Service");

        // Device Information Characteristics
        attributes.put(SYSTEM_ID, "System ID");
        attributes.put(MODEL_NUMBER_STRING, "Model Number String");
        attributes.put(SERIAL_NUMBER_STRING, "Serial Number String");
        attributes.put(FIRMWARE_REVISION_STRING, "Firmware Revision String");
        attributes.put(HARDWARE_REVISION_STRING, "Hardware Revision String");
        attributes.put(SOFTWARE_REVISION_STRING, "Software Revision String");
        attributes.put(MANUFACTURER_NAME_STRING, "Manufacturer Name String");
        attributes.put(PNP_ID, "PnP ID");
        attributes.put(IEEE, "IEEE 11073-20601 Regulatory Certification Data List");

        attributes.put(SERIAL_DATA_SERVIC, "SERIAL Data Service");
        attributes.put(SERIAL_DATA, "SERIAL Data");

        // Descriptors
        attributes.put(CHARACTERISTIC_EXTENDED_PROPERTIES, "Characteristic Extended Properties");
        attributes.put(CHARACTERISTIC_USER_DESCRIPTION, "Characteristic User Description");
        attributes.put(CLIENT_CHARACTERISTIC_CONFIG, "Client Characteristic Configuration");
        attributes.put(SERVER_CHARACTERISTIC_CONFIGURATION, "Server Characteristic Configuration");
        attributes.put(CHARACTERISTIC_PRESENTATION_FORMAT, "Characteristic Presentation Format");
        attributes.put(CHARACTERISTIC_AGGREGATE_FORMAT, "Characteristic Aggregate Format");
        attributes.put(VALID_RANGE, "Valid Range");
        attributes.put(ENVIRONMENTAL_SENSING_CONFIGURATION, "Environmental Sensing Configuration");
        attributes.put(ENVIRONMENTAL_SENSING_MEASUREMENT, "Environmental Sensing Measurement");
        attributes.put(ENVIRONMENTAL_SENSING_TRIGGER_SETTING, "Environmental Sensing Trigger Setting");

        /**
         * Descriptor key value mapping
         */
        descriptorAttributes.put("0", "Reserved For Future Use");
        descriptorAttributes.put("1", "Boolean");
        descriptorAttributes.put("2", "unsigned 2-bit integer");
        descriptorAttributes.put("3", "unsigned 4-bit integer");
        descriptorAttributes.put("4", "unsigned 8-bit integer");
        descriptorAttributes.put("5", "unsigned 12-bit integer");
        descriptorAttributes.put("6", "unsigned 16-bit integer");
        descriptorAttributes.put("7", "unsigned 24-bit integer");
        descriptorAttributes.put("8", "unsigned 32-bit integer");
        descriptorAttributes.put("9", "unsigned 48-bit integer");
        descriptorAttributes.put("10", "unsigned 64-bit integer");
        descriptorAttributes.put("11", "unsigned 128-bit integer");
        descriptorAttributes.put("12", "signed 8-bit integer");
        descriptorAttributes.put("13", "signed 12-bit integer");
        descriptorAttributes.put("14", "signed 16-bit integer");
        descriptorAttributes.put("15", "signed 24-bit integer");
        descriptorAttributes.put("16", "signed 32-bit integer");
        descriptorAttributes.put("17", "signed 48-bit integer");
        descriptorAttributes.put("18", "signed 64-bit integer");
        descriptorAttributes.put("19", "signed 128-bit integer");
        descriptorAttributes.put("20", "IEEE-754 32-bit floating point");
        descriptorAttributes.put("21", "IEEE-754 64-bit floating point");
        descriptorAttributes.put("22", "IEEE-11073 16-bit SFLOAT");
        descriptorAttributes.put("23", "IEEE-11073 32-bit FLOAT");
        descriptorAttributes.put("24", "IEEE-20601 format");
        descriptorAttributes.put("25", "UTF-8 string");
        descriptorAttributes.put("26", "UTF-16 string");
        descriptorAttributes.put("27", "Opaque Structure");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static String lookupUUID(UUID uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static String lookCharacteristicPresentationFormat(String key) {
        String value = descriptorAttributes.get(key);
        return value == null ? "Reserved" : value;
    }

}
