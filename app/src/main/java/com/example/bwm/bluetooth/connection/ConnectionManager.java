package com.example.bwm.bluetooth.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.text.TextUtils;
import android.util.Log;

import com.example.bwm.bluetooth.BluetoothStatus;
import com.example.bwm.bluetooth.listener.OnStatusChangeListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 连接管理器
 * 负责蓝牙连接的建立、断开和状态管理
 * 应用单一职责原则和策略模式
 */
public class ConnectionManager {
    private static final String TAG = "ConnectionManager";
    private static final String NAME = "Bluetooth";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mAdapter;
    private volatile BluetoothStatus mState = BluetoothStatus.NONE;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;

    private List<OnStatusChangeListener> mStatusListeners = new ArrayList<>();
    private OnConnectionEstablishedListener mConnectionListener;

    public interface OnConnectionEstablishedListener {
        void onConnectionEstablished(BluetoothSocket socket, BluetoothDevice device);
        void onConnectionFailed();
        void onConnectionLost();
    }

    public ConnectionManager(BluetoothAdapter adapter) {
        this.mAdapter = adapter;
    }

    /**
     * 设置连接建立监听器
     */
    public void setConnectionEstablishedListener(OnConnectionEstablishedListener listener) {
        this.mConnectionListener = listener;
    }

    /**
     * 添加状态监听器
     */
    public void addStatusListener(OnStatusChangeListener listener) {
        if (!mStatusListeners.contains(listener)) {
            mStatusListeners.add(listener);
        }
    }

    /**
     * 移除状态监听器
     */
    public void removeStatusListener(OnStatusChangeListener listener) {
        mStatusListeners.remove(listener);
    }

    /**
     * 获取当前状态
     */
    public synchronized BluetoothStatus getState() {
        return mState;
    }

    /**
     * 设置状态并通知监听器
     */
    private synchronized void setState(BluetoothStatus state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // 通知所有状态监听器
        for (OnStatusChangeListener listener : mStatusListeners) {
            if (listener != null) {
                listener.onChange(state);
            }
        }
    }

    /**
     * 开始监听连接（服务端模式）
     */
    public synchronized void startListening() {
        Log.d(TAG, "startListening");

        // 取消现有连接
        cancelConnectThread();

        // 启动监听线程
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(BluetoothStatus.LISTEN);
    }

    /**
     * 连接到指定设备（客户端模式）
     */
    public synchronized void connect(String macAddress) {
        Log.d(TAG, "connect to: " + macAddress);

        if (TextUtils.isEmpty(macAddress)) {
            throw new IllegalArgumentException("MAC address cannot be null or empty");
        }

        if (!BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            throw new IllegalArgumentException("Invalid MAC address format");
        }

        if (mAdapter == null) {
            throw new NullPointerException("BluetoothAdapter is null");
        }

        BluetoothDevice device = mAdapter.getRemoteDevice(macAddress);
        mAdapter.cancelDiscovery();

        // 断开现有连接
        disconnect();

        // 启动连接线程
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(BluetoothStatus.CONNECTING);
    }

    /**
     * 断开连接
     */
    public synchronized void disconnect() {
        cancelConnectThread();
    }

    /**
     * 关闭所有连接
     */
    public synchronized void close() {
        Log.d(TAG, "close");

        cancelConnectThread();

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mStatusListeners.clear();
        setState(BluetoothStatus.NONE);
    }

    /**
     * 取消连接线程
     */
    private void cancelConnectThread() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    /**
     * 连接建立后的处理
     */
    private synchronized void onConnected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "onConnected");

        // 取消所有连接线程
        cancelConnectThread();

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(BluetoothStatus.CONNECTED);

        // 通知连接建立
        if (mConnectionListener != null) {
            mConnectionListener.onConnectionEstablished(socket, device);
        }
    }

    /**
     * 连接失败处理
     */
    private void onConnectionFailed() {
        setState(BluetoothStatus.FAILED);
        if (mConnectionListener != null) {
            mConnectionListener.onConnectionFailed();
        }
    }

    /**
     * 连接丢失处理
     */
    private void onConnectionLost() {
        setState(BluetoothStatus.LOST);
        if (mConnectionListener != null) {
            mConnectionListener.onConnectionLost();
        }
    }

    /**
     * 服务端监听线程
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            Log.d(TAG, "BEGIN AcceptThread");
            setName("AcceptThread");

            BluetoothSocket socket;

            while (mState != BluetoothStatus.CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (ConnectionManager.this) {
                        switch (mState) {
                            case LISTEN:
                            case CONNECTING:
                                onConnected(socket, socket.getRemoteDevice());
                                break;
                            case NONE:
                            case CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.d(TAG, "END AcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel AcceptThread");
            try {
                if (mmServerSocket != null) {
                    mmServerSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    /**
     * 客户端连接线程
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            Log.d(TAG, "BEGIN ConnectThread");
            setName("ConnectThread");

            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                onConnectionFailed();
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }

            synchronized (ConnectionManager.this) {
                mConnectThread = null;
            }

            onConnected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
