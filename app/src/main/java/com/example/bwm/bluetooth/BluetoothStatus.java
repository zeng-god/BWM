package com.example.bwm.bluetooth;

/**
 * 蓝牙连接状态枚举
 * 统一的状态定义，避免类型不匹配问题
 */
public enum BluetoothStatus {
    NONE,       // 无状态
    LISTEN,     // 监听状态
    CONNECTING, // 连接中
    CONNECTED,  // 已连接
    LOST,       // 连接丢失
    FAILED      // 连接失败
}