package com.example.bwm.bluetooth.listener;

public interface OnRecvDataListener extends IErrorListener {
    void onReceived(byte[] datas);
}