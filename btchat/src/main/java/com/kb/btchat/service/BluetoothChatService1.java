package com.kb.btchat.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kb.btchat.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothChatService1 {

    private final String TAG = this.getClass().getSimpleName();
    private static final UUID uuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread = null;
    private ConnectedThread mConnectedThread = null;
    private AcceptThread mAcceptThread = null;

    private Handler mHandler;
    private int mState;

    public int getState() {
        return mState;
    }

    public BluetoothChatService1(Context mContext, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        mState = STATE_NONE;
    }

    public synchronized void start() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    public synchronized void write(byte[] message) {
        synchronized (BluetoothChatService1.this) {
            if (mConnectedThread != null) {
                mConnectedThread.write(message);
            }
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    private class ConnectThread extends Thread {
        private BluetoothDevice mDevice;
        private BluetoothSocket mSocket = null;

        public ConnectThread(BluetoothDevice mDevice) {
            BluetoothSocket temp = null;
            this.mDevice = mDevice;
            try {
                temp = mDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            mSocket = temp;
            mState = STATE_CONNECTING;
        }

        @Override
        public void run() {
            mAdapter.cancelDiscovery();
            try {
                mSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                mState = STATE_NONE;
                BluetoothChatService1.this.start();
                return;
            }

            synchronized (BluetoothChatService1.this) {
                mAcceptThread = null;
            }

            //在这里去开启连接上后，监听读写操作的线程
            startReadWriteThread(mSocket, mDevice);
        }

        private void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket mServiceSocket;
        public AcceptThread() {
            BluetoothServerSocket mTemp = null;
            try {
                mTemp = mAdapter.listenUsingRfcommWithServiceRecord("accept_thread", uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mServiceSocket = mTemp;
            mState = STATE_LISTEN;
        }

        @Override
        public void run() {
            BluetoothSocket mSocket = null;
            while (mState != STATE_CONNECTED) {
                Log.e("TAG", "accept thread run");
                try {
                    mSocket = mServiceSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                if (mSocket != null) {
                    synchronized (BluetoothChatService1.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                //在这里启动连接上之后，监听读写操作的线程
                                startReadWriteThread(mSocket, mSocket.getRemoteDevice());
                                break;

                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    mSocket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }
            }
        }

        private void cancel() {
            try {
                mServiceSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {

        private InputStream mIS;
        private OutputStream mOS;
        private BluetoothSocket mSocket;

        public ConnectedThread(BluetoothSocket mSocket) {
            this.mSocket = mSocket;
            InputStream isTemp = null;
            OutputStream osTemp = null;
            Log.e(TAG, "deviceName=" + mSocket.getRemoteDevice().getName());
            try {
                isTemp = mSocket.getInputStream();
                osTemp = mSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG,"getIOException");
                e.printStackTrace();
            }

            mIS = isTemp;
            mOS = osTemp;

            mState = STATE_CONNECTED;
        }

        @Override
        public void run() {
            int length;
            byte[] buffer = new byte[1024];
            while (mState == STATE_CONNECTED) {
                try {
                    length = mIS.read(buffer);
                    mHandler.obtainMessage(Constants.MESSAGE_READ, length, -1, buffer).sendToTarget();
                } catch (Exception e) {
                    e.printStackTrace();
                    connectionLost();
                    Log.e(TAG, "READ Exception");
                    break;
                }
            }
        }

        private void write(byte[] buffer) {
            try {
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                mOS.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void startReadWriteThread(BluetoothSocket mSocket, BluetoothDevice mDevice) {
        /*if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }*/
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();

        Message message = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, mDevice.getName());
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;

        // Start the service over to restart listening mode
        BluetoothChatService1.this.start();
    }

    public void connectionFailed() {
        Message message = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        message.setData(bundle);
        mHandler.sendMessage(message);

        mState = STATE_NONE;


        BluetoothChatService1.this.start();
    }
}
