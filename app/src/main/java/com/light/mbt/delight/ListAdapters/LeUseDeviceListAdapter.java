package com.light.mbt.delight.ListAdapters;

/**
 * Created by RED on 2017/5/26.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.light.mbt.delight.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * List Adapter for holding devices found through scanning.
 */
public class LeUseDeviceListAdapter extends BaseAdapter {

    private LayoutInflater mInflator;
   // public List<DeviceList> DeviceList;
    public HashMap<String, DeviceList> mDeviceList = new HashMap<String, DeviceList>();
    public HashMap<String, Boolean> DeviceReady = new HashMap<String, Boolean>();
    private List<DeviceList> memDeviceList;
    public Button mEditButton;
    private int selectedItem = -1;
    private Context mContext;

    public LeUseDeviceListAdapter(Context context) {
        super();
        mContext = context;
        memDeviceList = new ArrayList<DeviceList>();
        mInflator = LayoutInflater.from(context);
    }

    public void addDevice(DeviceList Devicelist) {
        String DeviceAddress = Devicelist.getDeviceAddress();
        mDeviceList.put(DeviceAddress, Devicelist);
        DeviceReady.put(DeviceAddress, false);
        memDeviceList.add(Devicelist);
    }

    public void removeDevice(int position) {
        String Address = memDeviceList.get(position).getDeviceAddress();
        mDeviceList.remove(Address);
        DeviceReady.remove(Address);
        memDeviceList.remove(position);
    }

    public void cleanDevice() {
        mDeviceList.clear();
    }

    public DeviceList getDevice(int position) {
         return memDeviceList.get(position);
    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public Object getItem(int i) {
        return mDeviceList.get(i);
    }

    public void setSelectedItem(int selectedItem) {
        this.selectedItem = selectedItem;
    }

    public void setDeviceReadyItem(String Address, Boolean deviceReadItem) {
        DeviceReady.put(Address, deviceReadItem);
    }

    public boolean getDeviceReadyItem(String Address) {
        return DeviceReady.get(Address);
    }

    public int getSelectedItem() {
        return this.selectedItem;
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
            view = mInflator.inflate(R.layout.listitem_use_device, viewGroup, false);

            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view
                    .findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view
                    .findViewById(R.id.device_name);
            viewHolder.deviceEdit = (Button) view.findViewById(R.id.device_edit);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        try {
            viewHolder.deviceName.setText(memDeviceList.get(position).getName());
            viewHolder.deviceAddress.setText(memDeviceList.get(position).getDeviceAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }

        viewHolder.deviceEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEditButton = (Button) view;
            }
        });

        if (position == selectedItem) { //長按選擇變色
            view.setBackgroundResource(R.drawable.ic_list_bg_blue);
        } else {
            view.setBackgroundResource(R.drawable.ic_list_bg_gray);
            String Address = memDeviceList.get(position).getDeviceAddress();
            viewHolder.deviceName.setTextColor(mContext.getResources().getColor(R.color.list_device_not_read));
            if (DeviceReady.get(Address) == true) {    //掃描到Device 變為白底
                view.setBackgroundResource(R.mipmap.list_bg);
                viewHolder.deviceName.setTextColor(mContext.getResources().getColor(R.color.main_bg_color));
            }
        }

        return view;
    }

    private class ViewHolder {
        TextView Name;
        TextView deviceName;
        TextView deviceAddress;
        Button deviceEdit;
    }
}
