package io.dcloud.uniplugin.delegate;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;

import java.util.List;

public interface WifiDelegate {

    void wifiScan(Activity mActivity);

    List<ScanResult> getWifiScanResult(Context context);

    int getCurrentIndex();

    void stopScan();
}
