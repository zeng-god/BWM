package com.example.bwm.bluetooth.communication;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.example.bwm.bluetooth.listener.OnRecvDataListener;
import com.example.bwm.bluetooth.listener.OnSendDataListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 通信管理器
 * 负责已建立连接的数据传输
 * 应用单一职责原则
 */
public class CommunicationManager {
    private static final String TAG = "CommunicationManager";
    private static final int HANDLE_RECV = 1;
    private static final int HANDLE_SEND = 2;

    private final BluetoothSocket mSocket;
    private ConnectedThread mConnectedThread;

    private List<OnRecvDataListener> mRecvListeners = new ArrayList<>();
    private OnSendDataListener mSendListener;
    private OnConnectionLostListener mConnectionLostListener;

    public interface OnConnectionLostListener {
        void onConnectionLost();
    }

    /**
     * 主线程消息处理器
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_RECV:
                    byte[] recvData = (byte[]) msg.obj;
                    for (OnRecvDataListener listener : mRecvListeners) {
                        if (listener != null) {
                            listener.onReceived(recvData);
                        }
                    }
                    break;
                case HANDLE_SEND:
                    Exception exception = (Exception) msg.obj;
                    if (mSendListener != null) {
                        if (exception == null) {
                            mSendListener.onSuccess(true);
                        } else {
                            mSendListener.onSuccess(false);
                            mSendListener.onError(exception);
                        }
                    }
                    break;
            }
        }
    };

    public CommunicationManager(BluetoothSocket socket) {
        this.mSocket = socket;
        startCommunication();
    }

    /**
     * 开始通信
     */
    private void startCommunication() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
        }

        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    /**
     * 设置连接丢失监听器
     */
    public void setConnectionLostListener(OnConnectionLostListener listener) {
        this.mConnectionLostListener = listener;
    }

    /**
     * 添加数据接收监听器
     */
    public void addRecvListener(OnRecvDataListener listener) {
        if (!mRecvListeners.contains(listener)) {
            mRecvListeners.add(listener);
        }
    }

    /**
     * 移除数据接收监听器
     */
    public void removeRecvListener(OnRecvDataListener listener) {
        mRecvListeners.remove(listener);
    }

    /**
     * 发送数据
     */
    public void send(byte[] data, OnSendDataListener listener) {
        mSendListener = listener;

        if (mConnectedThread != null) {
            mConnectedThread.write(data);
        } else {
            if (listener != null) {
                listener.onError(new IllegalStateException("Connection not established"));
            }
        }
    }

    /**
     * 关闭通信
     */
    public void close() {
        Log.d(TAG, "close");

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mRecvListeners.clear();
        mSendListener = null;
    }

    /**
     * 数据传输线程
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            Log.d(TAG, "BEGIN ConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);

                    byte[] recvData = new byte[bytes];
                    System.arraycopy(buffer, 0, recvData, 0, bytes);

                    mHandler.obtainMessage(HANDLE_RECV, recvData).sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);

                    // 通知连接丢失
                    if (mConnectionLostListener != null) {
                        mConnectionLostListener.onConnectionLost();
                    }
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            Exception exception = null;

            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                exception = e;
                Log.e(TAG, "Exception during write", e);
            }

            mHandler.obtainMessage(HANDLE_SEND, exception).sendToTarget();
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
