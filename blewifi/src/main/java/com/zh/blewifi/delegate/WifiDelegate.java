package com.yp.delegate;

import android.content.Context;
import android.net.wifi.ScanResult;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public interface WifiDelegate {

    void wifiScan(AppCompatActivity mActivity);
    List<ScanResult> getWifiScanResult(Context context);
    int getCurrentIndex();
    void stopScan();
}
