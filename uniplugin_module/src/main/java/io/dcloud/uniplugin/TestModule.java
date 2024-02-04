package io.dcloud.uniplugin;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.dcloud.ConfigHelper;
import io.dcloud.erro.ErrCode;
import io.dcloud.feature.uniapp.UniSDKInstance;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;
import io.dcloud.lib.SearchDeviceHelper;
import io.dcloud.listener.ConfigListener;
import io.dcloud.listener.LogListener;
import io.dcloud.listener.ScanResultListener;
import io.dcloud.listener.SearchDeviceListener;
import io.dcloud.service.ConfigService;
import io.dcloud.util.DateUtil;
import io.dcloud.util.YueWifiHelper;


public class TestModule extends UniModule {

    String TAG = "TestModule";
    public static int REQUEST_CODE = 1000;

    UniJSCallback cb;

    SearchDeviceHelper searchDeviceHelper;

    JSONObject jsonObject;

    //蓝牙查找
    @UniJSMethod(uiThread = true)
    public void searchLanya(JSONObject options, UniJSCallback callback) {
        Log.e(TAG, "testAsyncFunc--"+options);
        if(callback != null) {
            cb = callback;
                        //callback.invokeAndKeepAlive(data);
        }
        if (options != null){
            jsonObject = options;
            Log.e(TAG, "蓝牙名称: "+jsonObject.getString("lanyaName") );
        }
        searchDeviceHelper = new SearchDeviceHelper((Activity)mUniSDKInstance.getContext() );
        searchDeviceHelper.reuestBlePermission((Activity)mUniSDKInstance.getContext());
        searchDeviceHelper.searchDevice(searchDevice);
    }

    //WIFI查找
    @UniJSMethod(uiThread = true)
    public void searchWifi(JSONObject options, UniJSCallback callback) {
        Log.e(TAG, "searchWifi--"+options);
        if(callback != null) {
            cb = callback;
                        //callback.invokeAndKeepAlive(data);
        }
        if (options != null){
            jsonObject = options;
        }
        wifiGet();
    }

    //连接
    @UniJSMethod(uiThread = true)
    public void lianjie(JSONObject options, UniJSCallback callback) {
        Log.e(TAG, "lianjie--"+options);
        if(callback != null) {
            cb = callback;
            //callback.invokeAndKeepAlive(data);
        }
        if (options != null){
            jsonObject = options;
        }
//        wifiGet();

        configHelper = new ConfigHelper();
        configHelper.registerLogListener(mUniSDKInstance.getContext(),logListener);
        configHelper.registerConfigListener(mUniSDKInstance.getContext(),configListener);
//        configHelper.setPassword("pye971108.")
//        .setSsid(("104").getBytes(StandardCharsets.UTF_8))
//                .setDeviceAddress("70:1D:08:08:CF:C9")
        configHelper.setPassword(jsonObject.getString("wifiPassword"))
                .setSsid(jsonObject.getString("SSID").getBytes(StandardCharsets.UTF_8))
                .setDeviceAddress(jsonObject.getString("Mac"))
                .setTimeoutMilliscond(40*1000)//40s
                .startConfig((Activity)mUniSDKInstance.getContext(), ConfigService.class);
    }

    //run JS thread
    @UniJSMethod (uiThread = false)
    public JSONObject testSyncFunc(){
        JSONObject data = new JSONObject();
        data.put("code", "success");
        return data;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE && data.hasExtra("respond")) {
            Log.e("TestModule", "原生页面返回----"+data.getStringExtra("respond"));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }



    @UniJSMethod (uiThread = true)
    public void gotoNativePage(){
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            Intent intent = new Intent(mUniSDKInstance.getContext(), NativePageActivity.class);
            ((Activity)mUniSDKInstance.getContext()).startActivityForResult(intent, REQUEST_CODE);
        }
    }

    /*-----------蓝牙查找------------*/
    private List<BluetoothDevice> mLeDevices = new ArrayList<>();
    private List<BluetoothDevice> mAllDevices = new ArrayList<>();
    int a = 0;
    SearchDeviceListener searchDevice = new SearchDeviceListener() {
        @Override
        public void onDiscoverDevice(BluetoothDevice device) {
            a = 0;
            //if(device.getName() != null){
            if(addDevice(device)){
                mLeDevices.clear();
                mLeDevices.addAll(mAllDevices);
                for (BluetoothDevice check:
                        mLeDevices) {
                    a += 1;
                    Log.e(TAG, "扫描到的蓝牙名称: "+check.getName());
                    if(Objects.nonNull(check.getName()) && a < 100){
                        if (check.getName().equals(jsonObject.getString("lanyaName"))){
                            Log.e(TAG, "Mac地址: "+check.getAddress());
                            JSONObject data = new JSONObject();
                            data.put("Mac", check.getAddress());
                            cb.invoke(data);
                            searchDeviceHelper.cancel();
                        }
                    }else if (a >= 100){
                        JSONObject data = new JSONObject();
                        data.put("Mac", "");
                        cb.invoke(data);
                        searchDeviceHelper.cancel();
                        return;
                    }
                    Log.e(TAG, "次数: "+a);
                }
            }
        }

        @Override
        public void endSearchDevice() {
        }
    };

    public boolean addDevice(BluetoothDevice device) {
        for (BluetoothDevice dev:mAllDevices){
            if(dev.getAddress().equals(device.getAddress())){
                return false;
            }
        }
        mAllDevices.add(device);
        // mLeDevices.add(device);
        return true;
    }
    /*-----------------------*/

    /**/
    byte[] SSID;
    private static final String SP_FILE = "SP_FILE";
    private static final String SP_KEY_BLE_NAME = "SP_KEY_BLE_NAME";
    private static final String SP_KEY_BLE_MAC = "SP_KEY_BLE_MAC";
    private static final String SP_KEY_WIFI_SSID = "SP_KEY_WIFI_SSID";
    private static final String SP_KEY_WIFI_PWD = "SP_KEY_WIFI_PWD";
    private static final String SP_KEY_CHECKBOX_PWD= "SP_KEY_CHECKBOX_PWD";
    public void wifiGet(){
        WifiManager wifiManager = (WifiManager) mUniSDKInstance.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
        }else{
            Log.e(TAG, "创建成功" );
        }
        wifiManager.startScan();
        List<ScanResult> scanResults = wifiManager.getScanResults();
        List<String> SSID = new ArrayList<>();
        for (ScanResult check:
        scanResults) {
            SSID.add(check.SSID);
        }
        JSONObject data = new JSONObject();
        data.put("wifi", SSID);
        cb.invoke(data);
    }
    /**/

    /*--------连接使用---------------*/
    private ConfigHelper configHelper;
    private static final int MSG_LOG_UI = 99;
    private static final int MSG_ERROR = 98;
    EditText etBLEName,etSSID,etPwd,etLog;
    String bleMac = "70:1D:08:08:CF:C9";
    LogListener logListener = new LogListener() {
        @Override
        public void logInfo(String tag, String message) {
            Log.e(TAG, "logInfo: "+tag );
            Log.d(tag, message) ;
            appendLog(message);
        }

        @Override
        public void logError(String tag, Exception exception) {
            exception.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            appendLog(sw.toString());
        }
    };

    private void appendLog(String log){

        Message message = mHandler.obtainMessage();
        message.obj = log;
        message.what = MSG_LOG_UI;
        mHandler.sendMessage(message);
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_ERROR:
                    String errrMsg = (String) msg.obj;
                    Toast.makeText(mUniSDKInstance.getContext(), errrMsg, Toast.LENGTH_SHORT).show();
                case MSG_LOG_UI:
                    String log = (String) msg.obj;
//                    if (log.equals("1")){
//                        JSONObject data = new JSONObject();
//                        data.put("log", "config");
//                        cb.invoke(data);
//                    }else if(log.equals("2")){
//                        JSONObject data = new JSONObject();
//                        data.put("log", "wifi");
//                        cb.invoke(data);
//                    }else if(log.equals("3")){
//                        JSONObject data = new JSONObject();
//                        data.put("log", "wifiPwd");
//                        cb.invoke(data);
//                    }
                    break;
            }
        }
    };

    ConfigListener configListener = new ConfigListener() {

        @Override
        public void onSuccess() {
            JSONObject data = new JSONObject();
            data.put("peizhi", true);
            data.put("beizhu", "配网成功");
            cb.invoke(data);
            appendLog("配网成功");
//            Toast.makeText(mUniSDKInstance.getContext(), "配网成功", Toast.LENGTH_SHORT).show();
//            enableView(true);
        }

        @Override
        public void onFail(ErrCode errCode) {
            JSONObject data = new JSONObject();
            data.put("peizhi", false);
            data.put("beizhu", "配网失败,原因:"+errCode.toString());
            cb.invoke(data);
            appendLog("配网失败,原因:"+errCode.toString());
//            enableView(true);
//            Toast.makeText(mUniSDKInstance.getContext(), "配网失败,原因:"+errCode.toString(), Toast.LENGTH_SHORT).show();
        }
    };
    /*-----------------------*/
}
