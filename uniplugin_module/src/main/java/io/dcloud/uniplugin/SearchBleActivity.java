package io.dcloud.uniplugin;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.dcloud.uniplugin.lib.SearchDeviceHelper;
import io.dcloud.uniplugin.lib.listener.SearchDeviceListener;
import uni.dcloud.io.uniplugin_module.R;

public class SearchBleActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, CompoundButton.OnCheckedChangeListener {


    private static final String TAG = "SearchBleDevActivity";

    RecyclerView recyclerView;
    SwipeRefreshLayout refreshLayout;

    SearchDeviceHelper searchDeviceHelper;
    RecyclerViewAdapter adapter;
    CheckBox cbUnkonwDevice;
    EditText etFilterDevName;
    FloatingActionButton btn_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_ble_dev);


        recyclerView = findViewById(R.id.recyclerView);
        cbUnkonwDevice = findViewById(R.id.cb_unkonw_device);
        etFilterDevName = findViewById(R.id.et_filter_dev_name);
        btn_back = findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        refreshLayout = findViewById(R.id.refreshLayout);
        refreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        refreshLayout.setOnRefreshListener(this);

        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));


        adapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(adapter);

        searchDeviceHelper = new SearchDeviceHelper(this);
        searchDeviceHelper.reuestBlePermission(this);
        searchDeviceHelper.searchDevice(searchDevice);
        refreshLayout.setRefreshing(true);

        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View v, BluetoothDevice dev, int position) {
                Intent intent = new Intent();
                intent.putExtra("DeviceName", dev.getName());
                intent.putExtra("DeviceMac", dev.getAddress());
                setResult(RESULT_OK, intent);
                finish();
                searchDeviceHelper.cancel();
            }
        });

        cbUnkonwDevice.setOnCheckedChangeListener(this);
        cbUnkonwDevice.setChecked(true);

        etFilterDevName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                filterDevice();

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideInputKeyboard(etFilterDevName);
    }


    /**
     * 隐藏键盘
     * 弹窗弹出的时候把键盘隐藏掉
     */
    protected void hideInputKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void filterDevice() {
        mLeDevices.clear();
        mLeDevices.addAll(mAllDevices);
        filterUnknowDevice(cbUnkonwDevice.isChecked());
        filterDeviceName();
        adapter.notifyDataSetChanged();
    }

    SearchDeviceListener searchDevice = new SearchDeviceListener() {
        @Override
        public void onDiscoverDevice(BluetoothDevice device) {
            //if(device.getName() != null){
            if (addDevice(device)) {
                filterDevice();
            }
            // }
        }

        @Override
        public void endSearchDevice() {
            refreshLayout.setRefreshing(false);
        }
    };

    public boolean addDevice(BluetoothDevice device) {
        for (BluetoothDevice dev : mAllDevices) {
            if (dev.getAddress().equals(device.getAddress())) {
                return false;
            }
        }
        mAllDevices.add(device);
        // mLeDevices.add(device);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLeDevices.clear();
        mAllDevices.clear();
        searchDeviceHelper.cancel();
        searchDeviceHelper = null;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (cbUnkonwDevice == buttonView) {
            if (mLeDevices.size() == 0) {
                return;
            }
            filterDevice();
        }
    }

    private void filterDeviceName() {
        if ((etFilterDevName.getText() + "").length() == 0) {
            return;
        }

        Iterator<BluetoothDevice> iterator = mLeDevices.iterator();
        while (iterator.hasNext()) {
            BluetoothDevice device = iterator.next();
            if (device.getName() != null && device.getName().startsWith(etFilterDevName.getText() + "") == false) {
                iterator.remove();
            } else if (device.getName() == null) {
                iterator.remove();
            }
        }
    }

    private void filterUnknowDevice(boolean allowUnknowDeviceName) {
        if (allowUnknowDeviceName) {
            return;
        }
        Iterator<BluetoothDevice> iterator = mLeDevices.iterator();
        while (iterator.hasNext()) {
            BluetoothDevice device = iterator.next();
            if (device.getName() == null) {
                iterator.remove();
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvDevName;
        TextView tvDevMac;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDevName = itemView.findViewById(R.id.dev_name);
            tvDevMac = itemView.findViewById(R.id.dev_mac);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Log.d(TAG, "itemView dev....");
                }
            });
        }
    }

    //回调接口
    public interface OnItemClickListener {
        void onItemClick(View v, BluetoothDevice device, int position);
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.dev_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

            final ViewHolder viewHolder = (ViewHolder) holder;
            final BluetoothDevice dev = mLeDevices.get(position);
            viewHolder.tvDevName.setText(dev.getName() == null ? "Unknow Device" : dev.getName());
            viewHolder.tvDevMac.setText(dev.getAddress() + "");


            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "select dev:" + dev.getName());
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(viewHolder.itemView, dev, position);
                    }
                }
            });

        }

        @Override
        public int getItemCount() {
            return mLeDevices.size();
        }

        //私有属性
        private OnItemClickListener onItemClickListener = null;

        //setter方法
        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }
    }

    @Override
    public void onRefresh() {

        Log.d("SearchBleDevActivity", "onRefresh:");
        mLeDevices.clear();
        mAllDevices.clear();
        boolean rs = searchDeviceHelper.searchDevice(searchDevice);


    }


    private List<BluetoothDevice> mLeDevices = new ArrayList<>();
    private List<BluetoothDevice> mAllDevices = new ArrayList<>();
}
