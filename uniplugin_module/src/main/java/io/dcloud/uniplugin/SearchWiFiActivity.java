package io.dcloud.uniplugin;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.dcloud.uniplugin.Utils.YueWifiHelper;
import io.dcloud.uniplugin.listener.ScanResultListener;
import uni.dcloud.io.uniplugin_module.R;

public class SearchWiFiActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, ScanResultListener {

    public static final String TAG = "SearchWiFiActivity";
    private List<ScanResult> mWifiList = new ArrayList<>();

    RecyclerView recyclerView;
    SwipeRefreshLayout refreshLayout;
    RecyclerViewAdapter adapter;

    private YueWifiHelper helper;
    FloatingActionButton btn_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_wi_fi);


        recyclerView = findViewById(R.id.recyclerView);
        refreshLayout = findViewById(R.id.refreshLayout);
        refreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        refreshLayout.setOnRefreshListener(this);
        btn_back = findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));


        adapter = new RecyclerViewAdapter();
        recyclerView.setAdapter(adapter);


        helper = new YueWifiHelper(this, this);
        helper.startScan();
        refreshLayout.setRefreshing(true);
    }

    @Override
    public void onRefresh() {
        refreshLayout.setRefreshing(true);
        helper.stop();
        helper.startScan();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mWifiList.clear();
            mWifiList = null;
            helper.destroy();
        } catch (Exception e) {

        }
    }

    @Override
    public void resultSuc(final List<ScanResult> list, boolean isLastTime) {
        if (list != null && list.size() > 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(false);
                    mWifiList.clear();
                    List<String> nlist = new ArrayList<>();

                    for (ScanResult scanResult : list) {
                        if (nlist.contains(scanResult.SSID) == false && scanResult.SSID.length() > 0) {
                            mWifiList.add(scanResult);
                            nlist.add(scanResult.SSID);
                        }
                    }

                    adapter.notifyDataSetChanged();
                }
            });

        }
    }

    @Override
    public void filterFailure() {

    }

    @Override
    public void connectedWifiCallback(WifiInfo info) {

    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvSSIDName;
        TextView tvSSIDMAC;

        public ViewHolder(View itemView) {
            super(itemView);
            tvSSIDName = itemView.findViewById(R.id.dev_name);
            tvSSIDMAC = itemView.findViewById(R.id.dev_mac);

        }
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
            final ScanResult scanResult = mWifiList.get(position);
            viewHolder.tvSSIDName.setText(scanResult.SSID);
            viewHolder.tvSSIDMAC.setText("RSSI:" + scanResult.level);


            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "select dev:" + scanResult.SSID);
                    Intent intent = new Intent();
                    intent.putExtra("SSID", scanResult.SSID);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        intent.putExtra("SSID_DATA", getSSIDRawData(scanResult));
                    }
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });

        }

        @Override
        public int getItemCount() {
            return mWifiList.size();
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static byte[] getSSIDRawData(ScanResult scanResult) {
        try {
            Field field = scanResult.getClass().getField("wifiSsid");
            field.setAccessible(true);
            Object wifiSsid = field.get(scanResult);
            if (wifiSsid == null) {
                return null;
            }
            Method method = wifiSsid.getClass().getMethod("getOctets");
            method.setAccessible(true);
            return (byte[]) method.invoke(wifiSsid);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

}
