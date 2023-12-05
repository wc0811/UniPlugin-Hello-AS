package io.dcloud.uniplugin.lib;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;


import io.dcloud.uniplugin.lib.listener.SearchDeviceListener;
import io.dcloud.uniplugin.util.BLog;

import java.util.Set;

public class SearchDeviceHelper {

    private static final String TAG = "SearchDeviceHelper";
    private static final int REQUEST_PERMISSIONS_CODE = 0x99;

    private  Context mContext;
    private  BluetoothAdapter mBluetoothAdapter;
    private SearchDeviceListener mDeviceListener;
    private  int mTimemoutMillisecond;
    private boolean mScanning;
    Handler mHandler = new Handler();

    public SearchDeviceHelper(Context context){
        this.mContext = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    public void cancel(){
        if(mScanning){
            mScanning = false;
            mHandler.removeCallbacks(scanTimeoutRunable);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    public boolean searchDevice(SearchDeviceListener deviceListener){
        return searchDevice(10*1000,deviceListener);
    }

    public boolean searchDevice(final int timemoutMillisecond, SearchDeviceListener deviceListener){

        if(mBluetoothAdapter == null){
            BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        if(mBluetoothAdapter == null){
            return false;
        }
        if(mScanning){
            mHandler.removeCallbacks(scanTimeoutRunable);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }


        this.mTimemoutMillisecond = timemoutMillisecond;
        this.mDeviceListener = deviceListener;

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device : pairedDevices) {
            if(mDeviceListener !=null ){
                Log.d(TAG, "pairedDevices:" + device);
                mDeviceListener.onDiscoverDevice(device);
            }
        }

        final int timeout = timemoutMillisecond;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mScanning = true;
                mHandler.postDelayed(scanTimeoutRunable,timeout);
            }
        }).start();


        return true;
    }

    public boolean hasBLEPermission(Context context){

        //使用BLE需要蓝牙、位置权限，录音存文件需要读、写文件的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_PRIVILEGED) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                )
        )
        {
            return false;
        }
        return true;
    }

    public void reuestBlePermission(Activity activity){

        if(hasBLEPermission(activity) == false) {

            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.BLUETOOTH_PRIVILEGED,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_PERMISSIONS_CODE
            );
        }
    }

    Runnable scanTimeoutRunable = new Runnable() {
        @Override
        public void run() {
            if(mScanning){
                BLog.d(TAG, "scan timeout....");
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
            mScanning = false;
            if(mDeviceListener != null){
                mDeviceListener.endSearchDevice();
            }
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

           // BLog.d(TAG, "onLeScan:" + device);
            if(mDeviceListener != null){
                mDeviceListener.onDiscoverDevice(device);
            }
        }
    };

}
