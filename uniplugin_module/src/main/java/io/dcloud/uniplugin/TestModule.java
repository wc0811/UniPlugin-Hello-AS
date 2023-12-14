package io.dcloud.uniplugin;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;
import io.dcloud.uniplugin.Utils.YueWifiHelper;
import io.dcloud.uniplugin.lib.ConfigHelper;
import io.dcloud.uniplugin.lib.SearchDeviceHelper;
import io.dcloud.uniplugin.lib.error.ErrCode;
import io.dcloud.uniplugin.lib.listener.ConfigListener;
import io.dcloud.uniplugin.lib.listener.SearchDeviceListener;
import io.dcloud.uniplugin.lib.service.ConfigService;
import io.dcloud.uniplugin.listener.ScanResultListener;


public class TestModule extends UniModule implements IDeviceHelper, ScanResultListener {

    String TAG = "TestModule";
    public static int REQUEST_CODE = 1000;


    SearchDeviceHelper searchDeviceHelper;

    private String strBleName = "Radar_BLE";

    private List<BluetoothDevice> mLeDevices = new ArrayList<>();
    private List<BluetoothDevice> mAllDevices = new ArrayList<>();
    private UniJSCallback callbackBleInfo = null;
    private UniJSCallback callbackWifiInfo = null;
    private UniJSCallback callbackResult = null;


    private YueWifiHelper helper;
    private List<ScanResult> mWifiList = new ArrayList<>();

    private ConfigHelper configHelper;

    //run ui thread
    @UniJSMethod(uiThread = true)
    public void testAsyncFunc(JSONObject options, UniJSCallback callback) {
        Log.e(TAG, "testAsyncFunc--" + options);
        if (callback != null) {
            JSONObject data = new JSONObject();
            data.put("code", "success");
            callback.invoke(data);
            //callback.invokeAndKeepAlive(data);
        }
    }

    //run ui thread
    @UniJSMethod(uiThread = true)
    public void testAsyncFunc1(JSONObject options, UniJSCallback callback) {
        if (callback != null) {
            callbackBleInfo = callback;
            //callback.invokeAndKeepAlive(data);
            if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
                try {
                    searchDeviceHelper = new SearchDeviceHelper(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.reuestBlePermission(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.searchDevice(searchDevice);

                } catch (Exception e) {
                    JSONObject data = new JSONObject();
                    data.put("code", "success");
                    data.put("messageForBle", "发生异常" + e.getCause());
                    callbackBleInfo.invoke(data);
                }
            }
        }
    }

    @UniJSMethod(uiThread = true)
    public void testAsyncFunc2(JSONObject options, UniJSCallback callback) {
        if (callback != null) {
            callbackWifiInfo = callback;
            //callback.invokeAndKeepAlive(data);
            if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
                try {

                    searchDeviceHelper = new SearchDeviceHelper(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.reuestBlePermission(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.searchDevice(searchDevice);


                    helper = new YueWifiHelper((Activity) mUniSDKInstance.getContext(), this);
                    helper.startScan();
                } catch (Exception e) {
                    JSONObject data = new JSONObject();
                    data.put("code", "success");
                    data.put("messageForBle", "发生异常" + e.getCause());
                    callbackBleInfo.invoke(data);
                }
            }
        }
    }

    @UniJSMethod(uiThread = true)
    public void testAsyncFunc3(JSONObject options, UniJSCallback callback) {

        if (callback != null) {
            callbackResult = callback;
            String bleName = "";
            String wifiSSID = "";
            String wifiPassword = "";
            try {
                bleName = options.getString("bleName");
                wifiSSID = options.getString("wifiName");
                wifiPassword = options.getString("wifiPassword");


                configHelper = new ConfigHelper();

                if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
                    searchDeviceHelper = new SearchDeviceHelper(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.reuestBlePermission(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.searchDevice(searchDevice);

                    configHelper.registerConfigListener(((Activity) mUniSDKInstance.getContext()), configListener);
                    String bleMac = "";
                    for (BluetoothDevice item : mLeDevices) {
                        if (bleName != null && bleName == item.getName()) {
                            bleMac = item.getAddress();
                            break;
                        }
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        byte[] wifiSSIDBytes = null;
                        for (ScanResult wifiItem : mWifiList) {
                            if (wifiSSID != null && wifiSSID == wifiItem.SSID) {
                                wifiSSIDBytes = getSSIDRawData(wifiItem);
                                break;
                            }
                        }
                        configHelper.setPassword(wifiPassword).setSsid(wifiSSIDBytes).setDeviceAddress(bleMac).setTimeoutMilliscond(40 * 1000)//40s
                                .startConfig(((Activity) mUniSDKInstance.getContext()), ConfigService.class);

                    }
                }
            } catch (Exception exception) {
                JSONObject data = new JSONObject();
                data.put("code", "success");
                data.put("message", "异常" + exception.getCause());
                callback.invoke(data);
            }

            //callback.invokeAndKeepAlive(data);
        }
    }

    @UniJSMethod(uiThread = true)
    public void testAsyncFunc4(JSONObject options, UniJSCallback callback) {
        if (callback != null) {
            callbackBleInfo = callback;
            //callback.invokeAndKeepAlive(data);
            if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
                try {
                    strBleName = options.getString("bleName");
                    searchDeviceHelper = new SearchDeviceHelper(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.reuestBlePermission(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.searchDevice(searchDevice);

                } catch (Exception e) {
                    JSONObject data = new JSONObject();
                    data.put("code", "success");
                    data.put("messageForBle", "发生异常" + e.getCause());
                    callbackBleInfo.invoke(data);
                }
            }
        }

    }

    @UniJSMethod(uiThread = true)
    public void getBleInfoByBleName(JSONObject options, UniJSCallback callback) {
        if (callback != null) {
            callbackBleInfo = callback;
            //callback.invokeAndKeepAlive(data);
            if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
                try {
                    strBleName = options.getString("bleName");
                    searchDeviceHelper = new SearchDeviceHelper(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.reuestBlePermission(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.searchDevice(searchDevice);

                } catch (Exception e) {
                    JSONObject data = new JSONObject();
                    data.put("code", "success");
                    data.put("messageForBle", "发生异常" + e.getCause());
                    callbackBleInfo.invoke(data);
                }
            }
        }

    }

    //run ui thread
    @UniJSMethod(uiThread = true)
    public void setBleAndeNet(JSONObject options, UniJSCallback callback) {
        if (callback != null) {
            callbackResult = callback;
            String bleName = "";
            String wifiSSID = "";
            String wifiPassword = "";
            try {
                bleName = options.getString("bleName");
                wifiSSID = options.getString("wifiName");
                wifiPassword = options.getString("wifiPassword");


                configHelper = new ConfigHelper();

                if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
                    searchDeviceHelper = new SearchDeviceHelper(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.reuestBlePermission(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.searchDevice(searchDevice);

                    configHelper.registerConfigListener(((Activity) mUniSDKInstance.getContext()), configListener);
                    String bleMac = "";
                    for (BluetoothDevice item : mLeDevices) {
                        if (bleName != null && bleName == item.getName()) {
                            bleMac = item.getAddress();
                            break;
                        }
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        byte[] wifiSSIDBytes = null;
                        for (ScanResult wifiItem : mWifiList) {
                            if (wifiSSID != null && wifiSSID == wifiItem.SSID) {
                                wifiSSIDBytes = getSSIDRawData(wifiItem);
                                break;
                            }
                        }
                        configHelper.setPassword(wifiPassword).setSsid(wifiSSIDBytes).setDeviceAddress(bleMac).setTimeoutMilliscond(40 * 1000)//40s
                                .startConfig(((Activity) mUniSDKInstance.getContext()), ConfigService.class);

                    }
                }
            } catch (Exception exception) {
                JSONObject data = new JSONObject();
                data.put("code", "success");
                data.put("message", "异常" + exception.getCause());
                callback.invoke(data);
            }

            //callback.invokeAndKeepAlive(data);
        }
    }

    //run ui thread
    @UniJSMethod(uiThread = true)
    public void getBleInfo(JSONObject options, UniJSCallback callback) {
        Log.e(TAG, "testAsyncFunc--" + options);

        if (callback != null) {
            callbackBleInfo = callback;
            //callback.invokeAndKeepAlive(data);
            if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
                try {
                    searchDeviceHelper = new SearchDeviceHelper(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.reuestBlePermission(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.searchDevice(searchDevice);

                } catch (Exception e) {
                    JSONObject data = new JSONObject();
                    data.put("code", "success");
                    data.put("messageForBle", "发生异常" + e.getCause());
                    callbackBleInfo.invoke(data);
                }
            }
        }
    }

    @UniJSMethod(uiThread = true)
    public void getWifiInfo(JSONObject options, UniJSCallback callback) {
        if (callback != null) {
            callbackWifiInfo = callback;
            //callback.invokeAndKeepAlive(data);
            if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
                try {

                    searchDeviceHelper = new SearchDeviceHelper(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.reuestBlePermission(((Activity) mUniSDKInstance.getContext()));
                    searchDeviceHelper.searchDevice(searchDevice);


                    helper = new YueWifiHelper((Activity) mUniSDKInstance.getContext(), this);
                    helper.startScan();
                } catch (Exception e) {
                    JSONObject data = new JSONObject();
                    data.put("code", "success");
                    data.put("messageForBle", "发生异常" + e.getCause());
                    callbackBleInfo.invoke(data);
                }
            }
        }


    }

    //run JS thread
    @UniJSMethod(uiThread = false)
    public JSONObject testSyncFunc() {
        JSONObject data = new JSONObject();
        data.put("code", "success");
        return data;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && data.hasExtra("respond")) {
            Log.e("TestModule", "原生页面返回----" + data.getStringExtra("respond"));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @UniJSMethod(uiThread = true)
    public void gotoNativePage() {
        if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            Intent intent = new Intent(mUniSDKInstance.getContext(), TestActivity.class);
            ((Activity) mUniSDKInstance.getContext()).startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @Override
    public void getWifi() {

    }

    @Override
    public void searchBle() {

    }

    @Override
    public void connectBleAndWifi() {
        if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            searchDeviceHelper = new SearchDeviceHelper(((Activity) mUniSDKInstance.getContext()));
            searchDeviceHelper.reuestBlePermission(((Activity) mUniSDKInstance.getContext()));
            searchDeviceHelper.searchDevice(searchDevice);
        }
    }

    private void filterDevice() {
        mLeDevices.clear();
        mLeDevices.addAll(mAllDevices);
        filterDeviceName();
    }


    SearchDeviceListener searchDevice = new SearchDeviceListener() {
        @Override
        public void onDiscoverDevice(BluetoothDevice device) {
            if (device.getName() != null) {
                if (addDevice(device)) {
                    filterDevice();
                }
            }
        }

        @Override
        public void endSearchDevice() {
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

    private void filterDeviceName() {
        Iterator<BluetoothDevice> iterator = mLeDevices.iterator();
        while (iterator.hasNext()) {
            BluetoothDevice device = iterator.next();
            if (device.getName() != null && device.getName().startsWith(strBleName) == false) {
                iterator.remove();
            } else if (device.getName() == null) {
                iterator.remove();
            }
        }
        if (!mLeDevices.isEmpty() && callbackBleInfo != null) {
            String bleName = mLeDevices.get(mLeDevices.size() - 1).getName();
            JSONObject data = new JSONObject();
            data.put("code", "success");
            data.put("bleName", bleName);
            callbackBleInfo.invoke(data);
        }
    }

    @Override
    public void resultSuc(final List<ScanResult> list, boolean isLastTime) {
        if (list != null && list.size() > 0) {
            if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {

                ((Activity) mUniSDKInstance.getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mWifiList.clear();
                        List<String> nlist = new ArrayList<>();
                        for (ScanResult scanResult : list) {
                            if (nlist.contains(scanResult.SSID) == false && scanResult.SSID.length() > 0) {
                                mWifiList.add(scanResult);
                                if (callbackWifiInfo != null) {
                                    JSONObject data = new JSONObject();
                                    data.put("code", "success");
                                    data.put("wifi", scanResult.SSID);
                                    callbackWifiInfo.invoke(data);
                                }
                                nlist.add(scanResult.SSID);
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public void filterFailure() {

    }

    @Override
    public void connectedWifiCallback(WifiInfo info) {

    }


    ConfigListener configListener = new ConfigListener() {

        @Override
        public void onSuccess() {
            callbackResult.invoke("");

            if (callbackResult != null) {
                JSONObject data = new JSONObject();
                data.put("code", "success");
                data.put("message", "配网成功");
                callbackResult.invoke(data);
            }
        }

        @Override
        public void onFail(ErrCode errCode) {
            if (callbackResult != null) {
                JSONObject data = new JSONObject();
                data.put("code", "success");
                data.put("message", "配网失败,原因:" + errCode.toString());
                callbackResult.invoke(data);
            }
        }
    };

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
