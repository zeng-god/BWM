package com.example.bwm.bluetooth.listener;

import android.bluetooth.BluetoothDevice;

import java.util.List;

public interface OnSearchDeviceListener extends IErrorListener {
    /**
     * Call before discovery devices.
     */
    void onStartDiscovery();

    /**
     * Call when found a new device.
     *
     * @param device the new device
     */
    void onNewDeviceFound(BluetoothDevice device);

    /**
     * Call when the discovery process completed.
     *
     * @param bondedList the remote devices those are bonded(paired).
     * @param newList    the remote devices those are not bonded(paired).
     */
    void onSearchCompleted(List<BluetoothDevice> bondedList, List<BluetoothDevice> newList);

}
