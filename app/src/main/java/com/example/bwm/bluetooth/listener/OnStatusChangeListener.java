package com.example.bwm.bluetooth.listener;

import com.example.bwm.bluetooth.BluetoothStatus;;

public interface OnStatusChangeListener extends IErrorListener {
    void onChange(BluetoothStatus status);
}