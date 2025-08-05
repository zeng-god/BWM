package com.example.bwm.bluetooth.discover;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.example.bwm.bluetooth.listener.OnSearchDeviceListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备发现管理器
 * 负责蓝牙设备的搜索和发现功能
 * 应用单一职责原则和信息专家模式
 */
public class DeviceDiscoveryManager {
    private static final String TAG = "DeviceDiscoveryManager";

    private final BluetoothAdapter mAdapter;
    private final Context mContext;
    private OnSearchDeviceListener mSearchListener;

    private List<BluetoothDevice> mBondedList = new ArrayList<>();
    private List<BluetoothDevice> mNewList = new ArrayList<>();
    private DeviceDiscoveryReceiver mReceiver;
    private boolean mNeedUnregister = false;

    public DeviceDiscoveryManager(Context context, BluetoothAdapter adapter) {
        this.mContext = context;
        this.mAdapter = adapter;
    }

    /**
     * 开始搜索设备
     */
    public void startDiscovery(OnSearchDeviceListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("OnSearchDeviceListener cannot be null");
        }

        mSearchListener = listener;

        if (mAdapter == null) {
            mSearchListener.onError(new NullPointerException("Device has no bluetooth module!"));
            return;
        }

        // 注册广播接收器
        registerReceiver();

        // 清空列表
        mBondedList.clear();
        mNewList.clear();

        // 取消之前的搜索
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }

        // 开始搜索
        mAdapter.startDiscovery();

        if (mSearchListener != null) {
            mSearchListener.onStartDiscovery();
        }
    }

    /**
     * 取消搜索
     */
    public void cancelDiscovery() {
        if (mAdapter != null) {
            mAdapter.cancelDiscovery();
        }
        unregisterReceiver();
    }

    /**
     * 注册广播接收器
     */
    private void registerReceiver() {
        if (mReceiver == null) {
            mReceiver = new DeviceDiscoveryReceiver();
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mContext.registerReceiver(mReceiver, filter);
        mNeedUnregister = true;
    }

    /**
     * 注销广播接收器
     */
    private void unregisterReceiver() {
        if (mNeedUnregister && mReceiver != null) {
            try {
                mContext.unregisterReceiver(mReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver was not registered");
            }
            mNeedUnregister = false;
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        cancelDiscovery();
        mBondedList.clear();
        mNewList.clear();
        mSearchListener = null;
    }

    /**
     * 设备发现广播接收器
     */
    private class DeviceDiscoveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (mSearchListener != null) {
                    mSearchListener.onNewDeviceFound(device);
                }

                // 根据配对状态分类设备
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    if (!mNewList.contains(device)) {
                        mNewList.add(device);
                    }
                } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    if (!mBondedList.contains(device)) {
                        mBondedList.add(device);
                    }
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mSearchListener != null) {
                    mSearchListener.onSearchCompleted(mBondedList, mNewList);
                }
                unregisterReceiver();
            }
        }
    }
}

