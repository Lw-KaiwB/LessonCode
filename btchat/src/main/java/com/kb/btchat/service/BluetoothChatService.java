package com.kb.btchat.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.kb.btchat.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothChatService {
    private final String TAG = this.getClass().getSimpleName();

    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final UUID uuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private BluetoothAdapter mBTAdapter;
    private Handler mHandler;
    private int mState;
    private int mNewState;
    private AcceptThread mSecureThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public BluetoothChatService(Context context, Handler mHandler) {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        this.mHandler = mHandler;
    }

    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        mNewState = mState;
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
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
        if (mSecureThread == null) {
            mSecureThread = new AcceptThread();
            mSecureThread.start();
        }

        updateUserInterfaceTitle();
    }

    public synchronized void connectToDevice(BluetoothDevice bluetoothDevice) {
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

        mConnectThread = new ConnectThread(bluetoothDevice);
        mConnectThread.start();
        updateUserInterfaceTitle();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureThread != null) {
            mSecureThread.cancel();
            mSecureThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        Message message = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        message.setData(bundle);
        mHandler.sendMessage(message);

        updateUserInterfaceTitle();
    }

    public synchronized void stop() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mSecureThread != null) {
            mSecureThread.cancel();
            mSecureThread = null;
        }
        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

    public void write(byte[] buffer) {
        ConnectedThread r = null;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(buffer);
    }

    public void connectionFailed() {
        Message message = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        message.setData(bundle);
        mHandler.sendMessage(message);

        mState = STATE_NONE;

        updateUserInterfaceTitle();

        BluetoothChatService.this.start();
    }

    public void connectedLost() {
        Message message = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "connected lost");
        message.setData(bundle);
        mHandler.sendMessage(message);
        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServiceSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBTAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mmServiceSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket mSocket;

            while (mState != STATE_CONNECTED) {
                try {
                    mSocket = mmServiceSocket.accept();
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
                if (mSocket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_CONNECTING:
                            case STATE_LISTEN:
                                connected(mSocket, mSocket.getRemoteDevice());
                                break;

                            case STATE_CONNECTED:
                            case STATE_NONE:
                                try {
                                    mSocket.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }


            }
        }

        public void cancel() {
            try {
                mmServiceSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            this.mDevice = device;
            BluetoothSocket temp = null;

            try {
                temp = mDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mmSocket = temp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            setName("ConnectThread");
            mBTAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    mmSocket.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                connectionFailed();
                return;
            }

            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream mIS;
        private OutputStream mOS;

        private ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream is = null;
            OutputStream os = null;

            try {
                is = socket.getInputStream();
                os = socket.getOutputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mIS = is;
            mOS = os;
            mState = STATE_CONNECTED;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (mState == STATE_CONNECTED) {
                try {
                    bytes = mIS.read(buffer);
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (Exception e) {
                    e.printStackTrace();
                    connectedLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mOS.write(buffer);
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
