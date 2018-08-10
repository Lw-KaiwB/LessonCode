package com.kb.btchat.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kb.btchat.R;
import com.kb.btchat.model.DeviceInfo;

import java.util.ArrayList;

public class ScanDeviceActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();
    private static final int REQUEST_ENABLE_BT = 0x001;
    private static final int REQUEST_FIND_LOCATION_PERMISSION = 0x001;

    private static final int SCROLL_LISTVIEW = 0x010;

    private ListView mListView;
    private Button mBtn;
    private MyAdapter myAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private Context mContext;
    private int requestCode;

    private ArrayList<DeviceInfo> deviceInfos;

    private MyHandler mHandler = new MyHandler();
    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SCROLL_LISTVIEW:
                    mListView.scrollListBy(deviceInfos.size());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_scan_device);
        mListView = findViewById(R.id.device_list_view);
        mBtn = findViewById(R.id.scan_btn);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceInfos.clear();
                myAdapter.notifyDataSetChanged();
                if (mBluetoothAdapter.isEnabled()) {
                    if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        startDiscovery();
                    } else {
                        ActivityCompat.requestPermissions(ScanDeviceActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_FIND_LOCATION_PERMISSION);
                    }
                } else {
                    Toast.makeText(ScanDeviceActivity.this, R.string.turn_on_bt, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
        deviceInfos = new ArrayList<>();
        myAdapter = new MyAdapter();
        mListView.setAdapter(myAdapter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            deviceInfos.clear();
            myAdapter.notifyDataSetChanged();
            startDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FIND_LOCATION_PERMISSION) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDiscovery();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(ScanDeviceActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Log.e(TAG, "showDialog");
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                mBluetoothAdapter.startDiscovery();
            } else {
                Toast.makeText(this, R.string.turn_on_bt, Toast.LENGTH_LONG).show();
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!TextUtils.isEmpty(device.getAddress())) {
                    deviceInfos.add(new DeviceInfo(device.getName(), device.getAddress()));
                    myAdapter.notifyDataSetChanged();
                    mHandler.sendEmptyMessageDelayed(SCROLL_LISTVIEW,300);
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.e(TAG, "discovery finished");
            }
        }
    };

    private class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return deviceInfos == null ? 0 : deviceInfos.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            HolderView mHolder;
            if (convertView == null) {
                mHolder = new HolderView();
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_view, null);
                mHolder.name = convertView.findViewById(R.id.item_device_name);
                mHolder.address = convertView.findViewById(R.id.item_device_address);
                mHolder.view = convertView.findViewById(R.id.item_device_view);
                convertView.setTag(mHolder);
            } else {
                mHolder = (HolderView) convertView.getTag();
            }
            mHolder.name.setText(deviceInfos.get(position).getName());
            mHolder.address.setText(deviceInfos.get(position).getAddress());
            mHolder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    DeviceInfo mDevice = deviceInfos.get(position);
                    Intent intent = new Intent();
                    intent.putExtra("name", mDevice.getName());
                    intent.putExtra("address", mDevice.getAddress());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });

            return convertView;
        }

        class HolderView {
            private LinearLayout view;
            private TextView name, address;
        }
    }

    private void startDiscovery() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            mBluetoothAdapter.startDiscovery();
        }
    }
}
