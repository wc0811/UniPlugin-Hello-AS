package io.dcloud.listener;

import android.bluetooth.BluetoothDevice;

public interface SearchDeviceListener {

    void onDiscoverDevice(BluetoothDevice device);
    void endSearchDevice();
}
