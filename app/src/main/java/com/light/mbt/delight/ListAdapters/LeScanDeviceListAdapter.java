package com.light.mbt.delight.ListAdapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.light.mbt.delight.R;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static com.light.mbt.delight.ScanPageActivity.mDevRssiValues;
import static com.light.mbt.delight.ScanPageActivity.mDeviceAddress;
import static com.light.mbt.delight.ScanPageActivity.mDeviceName;
import static com.light.mbt.delight.ScanPageActivity.mLeDevices;
import static com.light.mbt.delight.ScanPageActivity.mPairButton;
import static com.light.mbt.delight.ScanPageActivity.mProgressdialog;

/**
 * Created by RED on 2017/5/26.
 */

public class LeScanDeviceListAdapter extends BaseAdapter implements Filterable {

    ArrayList<BluetoothDevice> mFilteredDevices = new ArrayList<BluetoothDevice>();
    private LayoutInflater mInflator;
    private static int rssiValue;
    private ItemFilter mFilter = new ItemFilter();
    private static Context mContext;

    public LeScanDeviceListAdapter(Context context) {
        super();
        mLeDevices = new ArrayList<BluetoothDevice>();
        mInflator = LayoutInflater.from(context);
        mContext = context;
    }

    public static void addDevice(BluetoothDevice device, int rssi) {
        rssiValue = rssi;
        // New device found
        if (!mLeDevices.contains(device)) {
            mDevRssiValues.put(device.getAddress(), rssi);
            mLeDevices.add(device);
        } else {
            mDevRssiValues.put(device.getAddress(), rssi);
        }
    }

    public static int getRssiValue() {
        return rssiValue;
    }

    /**
     * Getter method to get the blue tooth device
     *
     * @param position
     * @return BluetoothDevice
     */
    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    /**
     * Clearing all values in the device array list
     */
    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }


    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }


    @Override
    public View getView(final int position, View view, ViewGroup viewGroup) {
        final ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.listitem_scan_device, viewGroup, false);

            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view
                    .findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view
                    .findViewById(R.id.device_name);
            viewHolder.deviceRssi = (TextView) view
                    .findViewById(R.id.device_rssi);
            viewHolder.pairStatus = (Button) view.findViewById(R.id.btn_pair);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        /**
         * Setting the name and the RSSI of the BluetoothDevice. provided it
         * is a valid one
         */
        final BluetoothDevice device = mLeDevices.get(position);
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0) {
            try {
                viewHolder.deviceName.setText(deviceName);
                viewHolder.deviceAddress.setText(device.getAddress());
                byte rssival = (byte) mDevRssiValues.get(device.getAddress())
                        .intValue();
                if (rssival != 0) {
                    viewHolder.deviceRssi.setText(String.valueOf(rssival));
                }
                String pairStatus = (device.getBondState() == BluetoothDevice.BOND_BONDED) ? mContext.getResources().getString(R.string.bluetooth_pair) : mContext.getResources().getString(R.string.bluetooth_unpair);
                viewHolder.pairStatus.setText(pairStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            viewHolder.deviceName.setText(R.string.device_unknown);
            viewHolder.deviceName.setSelected(true);
            viewHolder.deviceAddress.setText(device.getAddress());
        }

        viewHolder.pairStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPairButton = (Button) view;
                mDeviceAddress = device.getAddress();
                mDeviceName = device.getName();
                String status = mPairButton.getText().toString();
                if (status.equalsIgnoreCase(mContext.getResources().getString(R.string.bluetooth_pair))) {
                    unpairDevice(device);
                } else {
                    pairDevice(device);
                }

            }
        });
        return view;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String mFilterString = constraint.toString().toLowerCase();

            FilterResults mResults = new FilterResults();

            final ArrayList<BluetoothDevice> list = mLeDevices;

            int count = list.size();
            final ArrayList<BluetoothDevice> nlist = new ArrayList<BluetoothDevice>(count);

            for (int i = 0; i < count; i++) {
                if (list.get(i).getName() != null && list.get(i).getName().toLowerCase().contains(mFilterString)) {
                    nlist.add(list.get(i));
                }
            }

            mResults.values = nlist;
            mResults.count = nlist.size();
            return mResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredDevices = (ArrayList<BluetoothDevice>) results.values;
            clear();
            int count = mFilteredDevices.size();
            for (int i = 0; i < count; i++) {
                BluetoothDevice mDevice = mFilteredDevices.get(i);
                LeScanDeviceListAdapter.addDevice(mDevice, LeScanDeviceListAdapter.getRssiValue());
                notifyDataSetChanged(); // notifies the data with new filtered values
            }
        }
    }

    private class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        Button pairStatus;
    }

    //For Pairing
    private void pairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

        } catch (Exception e) {
            if (mProgressdialog != null && mProgressdialog.isShowing()) {
                mProgressdialog.dismiss();
            }
        }

    }

    //For UnPairing
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

        } catch (Exception e) {
            if (mProgressdialog != null && mProgressdialog.isShowing()) {
                mProgressdialog.dismiss();
            }
        }
    }
}
