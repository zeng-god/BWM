package com.example.bwm.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.bwm.R;

import java.util.List;

/**
 * 蓝牙设备列表适配器
 * 用于在ListView中显示蓝牙设备信息
 */
public class DeviceAdapter extends BaseAdapter {

    // ========================================
    // 成员变量定义
    // ========================================

    Context context; // 上下文对象
    List<BluetoothDevice> listdata; // 设备数据列表

    // ========================================
    // 构造方法
    // ========================================

    /**
     * 构造函数
     * @param c 上下文
     * @param list 蓝牙设备列表
     */
    public DeviceAdapter(Context c, List<BluetoothDevice> list) {
        this.context = c;
        this.listdata = list;
    }

    // ========================================
    // 适配器接口实现
    // ========================================

    /**
     * 获取数据项数量
     */
    @Override
    public int getCount() {
        return this.listdata.size();
    }

    /**
     * 获取指定位置的数据项
     */
    @Override
    public Object getItem(int position) {
        return this.listdata.get(position);
    }

    /**
     * 获取指定位置的数据项ID
     */
    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    /**
     * 获取指定位置的视图
     * 设置设备名称和MAC地址显示
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(this.context).inflate(R.layout.item_list_device, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        BluetoothDevice device = this.listdata.get(position);
        @SuppressLint("MissingPermission")
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            name = "未知设备";
        }
        viewHolder.device_name.setText(name);
        viewHolder.device_address.setText(device.getAddress());

        return convertView;
    }

    // ========================================
    // 视图持有者类
    // ========================================

    /**
     * 视图持有者
     * 用于优化ListView性能
     */
    public static class ViewHolder {
        public View rootView; // 根视图
        public TextView device_name; // 设备名称文本框
        public TextView device_address; // 设备地址文本框

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.device_name = (TextView) rootView.findViewById(R.id.device_name);
            this.device_address = (TextView) rootView.findViewById(R.id.device_address);
        }
    }
}
