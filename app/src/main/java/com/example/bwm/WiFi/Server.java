package com.example.bwm.WiFi;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/** <p>服务器</p>*/
public class Server implements Runnable {
    private static final String TAG = "TEST_Server";
    public Server(){

    }
    @Override
    public void run() {
        ServerSocket ss = null;
        Socket s = null;

        try {
            ss = new ServerSocket(8888);
            Log.d(TAG, "服务器已启动，等待客户端连接...");
            // 获取本机IP地址
            String ipAddress=null;
            Log.d(TAG, "本机IP地址：" +ipAddress);
            while (true) {
                Socket clientSocket = ss.accept();
                Log.d(TAG, "本机IP地址：" +"客户端已连接：" + clientSocket.getInetAddress().getHostAddress());

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
