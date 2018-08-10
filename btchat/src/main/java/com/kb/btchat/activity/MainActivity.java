package com.kb.btchat.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kb.btchat.Constants;
import com.kb.btchat.R;
import com.kb.btchat.service.BluetoothChatService;
import com.kb.btchat.service.BluetoothChatService1;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();
    private static final int SCAN_BLUETOOTH = 0x001;
    private static final int REQUEST_ENABLE_BT = 0x002;
    private static final int REQUEST_DISCOVER_BT = 0x003;
    private static final int SCROLL_LISTVIEW = 0x010;
    private BluetoothChatService1 mChatService = null;
    private BluetoothAdapter mAdapter = null;
    private Context mContext;

    private String mConnectName;
    private ListView mListView;
    private EditText mEditText;
    private Button mBtn;
    private ArrayList<String> mMessageList;
    private MyAdapter mListViewAdapter;
    private MyHandler mHandler = new MyHandler();

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:

                            break;

                        case BluetoothChatService.STATE_CONNECTING:

                            break;

                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:

                            break;

                        default:
                            break;
                    }
                    break;

                case Constants.MESSAGE_WRITE:
                    byte[] mb = (byte[]) msg.obj;
                    mMessageList.add("Me:" + new String(mb));
                    mListViewAdapter.notifyDataSetChanged();
                    mHandler.sendEmptyMessageDelayed(SCROLL_LISTVIEW, 500);
                    break;

                case Constants.MESSAGE_READ:
                    byte[] rb = (byte[]) msg.obj;
                    mMessageList.add(mConnectName + ":" + new String(rb, 0, msg.arg1));
                    mListViewAdapter.notifyDataSetChanged();

                    mHandler.sendEmptyMessageDelayed(SCROLL_LISTVIEW, 500);

                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    mConnectName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != mContext) {
                        Toast.makeText(mContext, "Connected to "
                                + mConnectName, Toast.LENGTH_SHORT).show();
                    }
                    break;

                case Constants.MESSAGE_TOAST:
                    if (mContext != null) {
                        Toast.makeText(mContext, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;

                case SCROLL_LISTVIEW:
                    mListView.scrollListBy(mMessageList.size());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mMessageList = new ArrayList<>();
        mListViewAdapter = new MyAdapter();
        mListView = findViewById(R.id.message_list_view);
        mEditText = findViewById(R.id.message_edit);
        mBtn = findViewById(R.id.message_send);

        mListView.setAdapter(mListViewAdapter);

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChatService == null || mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
                    Toast.makeText(mContext, "No Connected", Toast.LENGTH_LONG).show();
                    return;
                }
                String str = mEditText.getText().toString();
                if (TextUtils.isEmpty(str)) {
                    Toast.makeText(mContext, "No Test", Toast.LENGTH_LONG).show();
                    return;
                }

                if (mChatService != null) {
                    mChatService.write(str.getBytes());
                    mEditText.setText("");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        } else if (mChatService == null) {
            setUpChat();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_secure_connect_scan:
            case R.id.menu_insecure_connect_scan:
                Intent intent = new Intent(MainActivity.this, ScanDeviceActivity.class);
                startActivityForResult(intent, SCAN_BLUETOOTH);
                break;

            case R.id.menu_discoverable:
                Intent intent1 = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                intent1.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                startActivityForResult(intent1, REQUEST_DISCOVER_BT);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCAN_BLUETOOTH && resultCode == RESULT_OK) {
            if (data != null) {
                String name = data.getStringExtra("name");
                String address = data.getStringExtra("address");
                Log.e(TAG, "name=" + name + " address=" + address);
                BluetoothDevice mDevice = mAdapter.getRemoteDevice(address);
                //mChatService.connectToDevice(mDevice);
                mChatService.connect(mDevice);
            }
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            setUpChat();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setUpChat() {
        if (mChatService == null) {
            //mChatService = new BluetoothChatService(this, mHandler);
            mChatService = new BluetoothChatService1(this, mHandler);
        }
    }

    private class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mMessageList == null ? 0 : mMessageList.size();
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
        public View getView(int position, View convertView, ViewGroup parent) {
            HolderView mHolderView;
            if (convertView == null) {
                mHolderView = new HolderView();
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_view, null);
                mHolderView.message = convertView.findViewById(R.id.item_message_text);
                convertView.setTag(mHolderView);
            } else {
                mHolderView = (HolderView) convertView.getTag();
            }

            mHolderView.message.setText(mMessageList.get(position));
            return convertView;
        }

        private class HolderView {
            private TextView message;
        }
    }
}
