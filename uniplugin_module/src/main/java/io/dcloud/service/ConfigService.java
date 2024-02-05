package io.dcloud.service;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import io.dcloud.erro.GattError;
import io.dcloud.security.CRC;
import io.dcloud.util.BLog;
import io.dcloud.util.ByteUtils;


public  class ConfigService extends IntentService {

    private final static String TAG = "ConfigService";

    protected int mConnectionState;
    protected final static int STATE_DISCONNECTED = 0;
    protected final static int STATE_CONNECTING = -1;
    protected final static int STATE_CONNECTED = -2;
    protected final static int STATE_CONNECTED_AND_READY = -3; // indicates that services were discovered
    protected final static int STATE_DISCONNECTING = -4;
    protected final static int STATE_CLOSED = -5;

    private BluetoothAdapter mBluetoothAdapter;

    public static final String EXTRA_DEVICE_ADDRESS = "com.belon.ota.extra.EXTRA_DEVICE_ADDRESS";
    public static final String EXTRA_SSID = "com.belon.ota.extra.EXTRA_SSID";
    public static final String EXTRA_PASSWORD = "com.belon.ota.extra.EXTRA_PASSWORD";
    public static final String EXTRA_TIMEOUT = "com.belon.ota.extra.EXTRA_TIMEOUT";


    public static final String BROADCAST_LOG = "com.belon.ota.broadcast.BROADCAST_LOG";
    public static final String BROADCAST_LOG_EXTRA_TAG = "com.belon.ota.broadcast.BROADCAST_LOG_EXTRA_TAG";
    public static final String BROADCAST_LOG_EXTRA_LEVEL = "com.belon.ota.broadcast.BROADCAST_LOG_EXTRA_LEVEL";
    public static final String BROADCAST_LOG_EXTRA_MSG = "com.belon.ota.broadcast.BROADCAST_LOG_EXTRA_MSG";
    public static final String BROADCAST_LOG_EXTRA_EXCEPTION = "com.belon.ota.broadcast.BROADCAST_LOG_EXTRA_EXCEPTION";
    public static final int    LOG_LEVEL_DEBUG = 0;
    public static final int    LOG_LEVEL_INFO = 1;
    public static final int    LOG_LEVEL_WARN = 2;
    public static final int    LOG_LEVEL_ERROR = 3;

    public final static String BROADCAST_CONFIG_CONECTED ="com.belon.bk3435.ble.BROADCAST_CONFIG_CONECTED";
    public final static String BROADCAST_CONFIG_ERR_DEV_CRC ="com.belon.bk3435.ble.BROADCAST_CONFIG_ERR_DEV_CRC";
    public final static String BROADCAST_CONFIG_ERR_APP_CRC ="com.belon.bk3435.ble.BROADCAST_CONFIG_ERR_DEV_CRC";
    public final static String BROADCAST_CONFIG_ERR_CON_TIMEOUT ="com.belon.bk3435.ble.BROADCAST_CONFIG_ERR_CON_TIMEOUT";
    public final static String BROADCAST_CONFIG_ERR_CON_PWD ="com.belon.bk3435.ble.BROADCAST_CONFIG_ERR_CON_PWD";
    public final static String BROADCAST_CONFIG_ERR_CON ="com.belon.bk3435.ble.BROADCAST_CONFIG_ERR_CON";
    public final static String BROADCAST_CONFIG_UUID_NOT_EXIST ="com.belon.bk3435.ble.BROADCAST_CONFIG_UUID_NOT_EXIST";
    public final static String BROADCAST_CONFIG_SUCCESS ="com.belon.bk3435.ble.BROADCAST_CONFIG_SUCCESS";



    public static final int ERROR_MASK = 0x1000;
    public static final int ERROR_CONNECTION_MASK = 0x4000;
    public static final int ERROR_CONNECTION_STATE_MASK = 0x8000;
    public static final int ERROR_SERVICE_DISCOVERY_NOT_STARTED = ERROR_MASK | 0x05;

    public final static UUID  CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    //配网服务UUID:FFFF
    public final static UUID UUID_SERVICE = UUID.fromString("0000ffff-0000-1000-8000-00805f9b34fb");
    //APP发数据UUID: FF01
    public final static UUID UUID_WRITE   = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    //APP收数据UUID:FF02
    public final static UUID UUID_READ    = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");

    public static final int WRITE_TYPE    = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;

    protected BluetoothGatt mBluetoothGatt;
    protected final Object mLock = new Object();
    protected boolean mConnected = false;
    protected boolean mRequestCompleted;
    protected byte[] mReceivedData = null;
    protected int mError;
    private   int mMTUSize = -1;

    private static final byte TX_HEADER_0 = (byte)0x55;
    private static final byte TX_HEADER_1 = (byte)0xAA;

    private static final byte RX_HEADER_0 = (byte)0xAA;
    private static final byte RX_HEADER_1 = (byte)0x55;

    private static final byte CMD_TX_START = 0x01;
    private static final byte CMD_TX_SSID = 0x02;
    private static final byte CMD_TX_PWD = 0x03;
    private static final byte CMD_RX_CRC_ERROR = 0x01;
    private static final byte CMD_RX_START = 0x05;
    private static final byte CMD_RX_SSID = 0x06;
    private static final byte CMD_RX_PWD = 0x07;
    private static final byte CMD_RX_CONENECTED = 0x08;
    private static final byte CMD_RX_CON_TIMEOUT = 0x09;
    private static final byte CMD_RX_CON_ERROR = (byte) 0xA0;



    public ConfigService() {
        super(TAG);
    }



    @Override
    public void onCreate() {
        super.onCreate();
        broadcastLog(LOG_LEVEL_DEBUG,getClass().getSimpleName() + " onCreate");
        initialize();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy...");
        try {
            broadcastLog(LOG_LEVEL_DEBUG,getClass().getSimpleName() + " onDestroy");
            if(mBluetoothGatt != null && mConnected){
                mBluetoothGatt.disconnect();
            }
            mBluetoothGatt = null;
            checkTimeoutRunnable = null;
        }catch (Exception e){

        }

    }

    private boolean initialize() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            BLog.e(TAG,"Unable to initialize BluetoothManager.");
            return false;
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            BLog.e(TAG,"Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    long startTime = 0L;
    String mDeviceAddress;
    byte[] mSSIDBytes;
    String mPassword;
    int timeoutMilliscond;
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        mSSIDBytes = intent.getByteArrayExtra(EXTRA_SSID);
        mPassword = intent.getStringExtra(EXTRA_PASSWORD);
        timeoutMilliscond = intent.getIntExtra(EXTRA_TIMEOUT,20*1000);

        broadcastLog(LOG_LEVEL_DEBUG,"Start connect...");
        mError = 0;
        final long before = SystemClock.elapsedRealtime();
        mBluetoothGatt = connect(mDeviceAddress);//start connect device
        final long after = SystemClock.elapsedRealtime();

        // Are we connected?
        if (mBluetoothGatt == null) {
            broadcastLog(LOG_LEVEL_DEBUG,"Bluetooth adapter disabled");
            broadcastConfig(BROADCAST_CONFIG_ERR_CON);
            return;
        }

        if (mError > 0) { // error occurred
            if ((mError & ERROR_CONNECTION_STATE_MASK) > 0) {
                final int error = mError & ~ERROR_CONNECTION_STATE_MASK;
                final boolean timeout = error == 133 && after > before + 25000; // timeout is 30 sec
                if (timeout) {
                    broadcastLog(LOG_LEVEL_DEBUG,"Device not reachable. Check if the device with address " + mDeviceAddress + " is in range, is advertising and is connectable");
                } else {
                    broadcastLog(LOG_LEVEL_DEBUG, String.format(Locale.US, "Connection failed (0x%02X): %s", error, GattError.parseConnectionError(error)));
                }
            } else {
                final int error = mError & ~ERROR_CONNECTION_MASK;
                broadcastLog(LOG_LEVEL_ERROR, String.format(Locale.US, "Connection failed (0x%02X): %s", error, GattError.parse(error)));
            }
            broadcastConfig(BROADCAST_CONFIG_ERR_CON);
            return;
        }

        broadcastConfig(BROADCAST_CONFIG_CONECTED);//connect sucess
        broadcastLog(LOG_LEVEL_DEBUG,"Connect success!");

        //open ff02 notify
        boolean state = enableNotify(UUID_SERVICE,UUID_READ,true);
        if(state == false){
            broadcastConfig(BROADCAST_CONFIG_UUID_NOT_EXIST);
            return;
        }

        //send start
        state = sendConfigStart();
        broadcastLog(LOG_LEVEL_DEBUG,"1");
        if(state == false){
            return;
        }

        //send ssid
        state = sendSSID();
        broadcastLog(LOG_LEVEL_DEBUG,"2");
        if(state == false){
            return;
        }

        //send pwd
        state = sendPWD();
        broadcastLog(LOG_LEVEL_DEBUG,"3");
        if(state == false){
            return;
        }

        waitDevConnectRouter();
    }

    Handler mHandler;
    //等待设备连接路由器
    private void waitDevConnectRouter(){

        mReceivedData = null;
        mError = 0;
        mRequestCompleted = false;

        mHandler = new Handler();
        mHandler.postDelayed(checkTimeoutRunnable,timeoutMilliscond);

        try {
            synchronized (mLock) {
                while ((!mRequestCompleted && mConnected && mError == 0))
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            Log.e(TAG,"Sleeping interrupted:"+ e);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if(mHandler.hasCallbacks(checkTimeoutRunnable)){
                mHandler.removeCallbacks(checkTimeoutRunnable);
            }
        }else {
            mHandler.removeCallbacks(checkTimeoutRunnable);
        }

        if(mReceivedData != null){
            if(mReceivedData[0] == RX_HEADER_0 && mReceivedData[1] == RX_HEADER_1){

                if(CRC.verifyCRC(mReceivedData) == false){
                    broadcastLog(LOG_LEVEL_DEBUG,"CRC failed to check the app！");
                    broadcastConfig(BROADCAST_CONFIG_ERR_APP_CRC);
                    return;
                }

                if(mReceivedData[2] == CMD_RX_CONENECTED){
                    broadcastLog(LOG_LEVEL_DEBUG,"Connect success.");
                    broadcastConfig(BROADCAST_CONFIG_SUCCESS);
                }else if(mReceivedData[2] == CMD_RX_CON_TIMEOUT){
                    broadcastLog(LOG_LEVEL_DEBUG,"Connect Timeout");
                    broadcastConfig(BROADCAST_CONFIG_ERR_CON_TIMEOUT);
                }else if(mReceivedData[2] == CMD_RX_CON_ERROR){
                    broadcastLog(LOG_LEVEL_DEBUG,"Connect Fail,ssid or pwd error");
                    broadcastConfig(BROADCAST_CONFIG_ERR_CON_PWD);
                }
            }
        }
    }

    Runnable checkTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if(mConnected){
                broadcastLog(LOG_LEVEL_DEBUG,"Timeout!");
                mLock.notifyAll();
            }
        }
    };

    private boolean sendPWD(){
        broadcastLog(LOG_LEVEL_DEBUG,"Start send password...");
        if(mBluetoothGatt == null){
            broadcastLog(LOG_LEVEL_DEBUG,"Send password fail,gatt is null!");
            return false;
        }
        BluetoothGattCharacteristic characteristic = getCharacteristic(UUID_SERVICE,UUID_WRITE);
        if(characteristic == null){
            broadcastLog(LOG_LEVEL_DEBUG,"Characteristic "+ UUID_WRITE.toString() + " not exist!");
            return false;
        }

        mReceivedData = null;
        mError = 0;
        mRequestCompleted = false;

        byte[] pwdBytes = mPassword.getBytes(StandardCharsets.UTF_8);
        byte[] sendByte = new byte[5 + pwdBytes.length];
        sendByte[0] = TX_HEADER_0;
        sendByte[1] = TX_HEADER_1;
        sendByte[2] = CMD_TX_PWD;
        sendByte[3] = (byte) pwdBytes.length;
        System.arraycopy(pwdBytes,0,sendByte,4,pwdBytes.length);
        sendByte = CRC.calcCRC(sendByte);
        broadcastLog(LOG_LEVEL_DEBUG,"send data:"+ ByteUtils.byte2HexStr(sendByte));

        characteristic.setWriteType(WRITE_TYPE);
        characteristic.setValue(sendByte);
        mBluetoothGatt.writeCharacteristic(characteristic);

        try {
            synchronized (mLock) {
                while ((!mRequestCompleted && mConnected && mError == 0))
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            Log.e(TAG,"Sleeping interrupted:"+ e);
        }

        if(mReceivedData == null){
            return false;
        }

        if(mReceivedData[0] == RX_HEADER_0 && mReceivedData[1] == RX_HEADER_1){

            if(CRC.verifyCRC(mReceivedData) == false){
                broadcastLog(LOG_LEVEL_DEBUG,"CRC failed to check the app！");
                broadcastConfig(BROADCAST_CONFIG_ERR_APP_CRC);
                return false;
            }

            if(mReceivedData[2] == CMD_RX_PWD){
                return true;
            }
            if(mReceivedData[2] == CMD_RX_CRC_ERROR){
                broadcastLog(LOG_LEVEL_DEBUG,"CRC failed to check the device！");
                broadcastConfig(BROADCAST_CONFIG_ERR_DEV_CRC);
                return false;
            }
        }
        return false;
    }

    private boolean sendSSID(){

        broadcastLog(LOG_LEVEL_DEBUG,"Start send ssid...");
        if(mBluetoothGatt == null){
            broadcastLog(LOG_LEVEL_DEBUG,"Send ssid fail,gatt is null!");
            return false;
        }
        BluetoothGattCharacteristic characteristic = getCharacteristic(UUID_SERVICE,UUID_WRITE);
        if(characteristic == null){
            broadcastLog(LOG_LEVEL_DEBUG,"Characteristic "+ UUID_WRITE.toString() + " not exist!");
            return false;
        }

        mReceivedData = null;
        mError = 0;
        mRequestCompleted = false;

        byte[] sendByte = new byte[5 + mSSIDBytes.length];
        sendByte[0] = TX_HEADER_0;
        sendByte[1] = TX_HEADER_1;
        sendByte[2] = CMD_TX_SSID;
        sendByte[3] = (byte) mSSIDBytes.length;
        System.arraycopy(mSSIDBytes,0,sendByte,4,mSSIDBytes.length);
        sendByte = CRC.calcCRC(sendByte);
        broadcastLog(LOG_LEVEL_DEBUG,"send data:"+ByteUtils.byte2HexStr(sendByte));

        characteristic.setWriteType(WRITE_TYPE);
        characteristic.setValue(sendByte);
        mBluetoothGatt.writeCharacteristic(characteristic);

        try {
            synchronized (mLock) {
                while ((!mRequestCompleted && mConnected && mError == 0))
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            Log.e(TAG,"Sleeping interrupted:"+ e);
        }

        if(mReceivedData == null){
            return false;
        }

        if(mReceivedData[0] == RX_HEADER_0 && mReceivedData[1] == RX_HEADER_1){

            if(CRC.verifyCRC(mReceivedData) == false){
                broadcastLog(LOG_LEVEL_DEBUG,"CRC failed to check the app！");
                broadcastConfig(BROADCAST_CONFIG_ERR_APP_CRC);
                return false;
            }

            if(mReceivedData[2] == CMD_RX_SSID){
                return true;
            }
            if(mReceivedData[2] == CMD_RX_CRC_ERROR){
                broadcastLog(LOG_LEVEL_DEBUG,"CRC failed to check the device！");
                broadcastConfig(BROADCAST_CONFIG_ERR_DEV_CRC);
                return false;
            }
        }
        return false;
    }


    private boolean sendConfigStart(){

        broadcastLog(LOG_LEVEL_DEBUG,"send start config...");
        if(mBluetoothGatt == null){
            broadcastLog(LOG_LEVEL_DEBUG,"Send start config fail,gatt is null!");
            return false;
        }
        BluetoothGattCharacteristic characteristic = getCharacteristic(UUID_SERVICE,UUID_WRITE);
        if(characteristic == null){
            broadcastLog(LOG_LEVEL_DEBUG,"Characteristic "+ UUID_WRITE.toString() + " not exist!");
            return false;
        }

        mReceivedData = null;
        mError = 0;
        mRequestCompleted = false;

        byte[] sendByte = new byte[]{TX_HEADER_0,TX_HEADER_1,CMD_TX_START,0x00,0x00};
        sendByte = CRC.calcCRC(sendByte);
        broadcastLog(LOG_LEVEL_DEBUG,"send data:"+ByteUtils.byte2HexStr(sendByte));
        characteristic.setWriteType(WRITE_TYPE);
        characteristic.setValue(sendByte);
        mBluetoothGatt.writeCharacteristic(characteristic);

        try {
            synchronized (mLock) {
                while ((!mRequestCompleted && mConnected && mError == 0))
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            Log.e(TAG,"Sleeping interrupted:"+ e);
        }

        if(mReceivedData == null){
            return false;
        }

        if(mReceivedData[0] == RX_HEADER_0 && mReceivedData[1] == RX_HEADER_1){
            if(CRC.verifyCRC(mReceivedData) == false){
                broadcastLog(LOG_LEVEL_DEBUG,"CRC failed to check the app！");
                broadcastConfig(BROADCAST_CONFIG_ERR_APP_CRC);
                return false;
            }
            if(mReceivedData[2] == CMD_RX_START){
                return true;
            }
            if(mReceivedData[2] == CMD_RX_CRC_ERROR){
                broadcastLog(LOG_LEVEL_DEBUG,"CRC failed to check the device！");
                broadcastConfig(BROADCAST_CONFIG_ERR_DEV_CRC);
                return false;
            }
        }

        return false;
    }



    private void broadcastConfig(String action){
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }




    protected BluetoothGattCharacteristic getCharacteristic(UUID serviceUUID,UUID characteristicUUID){
        BluetoothGattService service = mBluetoothGatt.getService(serviceUUID);
        BluetoothGattCharacteristic characteristic = null;
        if (service != null) {
            characteristic = service.getCharacteristic(characteristicUUID);
            return characteristic;
        }
        return null;
    }

    protected boolean enableNotify(UUID serviceUUID,UUID characteristicUUID,boolean notify){

        if(mBluetoothGatt == null){
            broadcastLog(LOG_LEVEL_DEBUG,"enable "+characteristicUUID+" Notify fail! BluetoothGatt is null");
            return false;
        }

        BluetoothGattService service = mBluetoothGatt.getService(serviceUUID);
        if (service != null) {

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
            boolean enabled = descriptor.getValue() != null && descriptor.getValue().length == 2 && descriptor.getValue()[0] > 0 && descriptor.getValue()[1] == 0;

            if (enabled) {
                broadcastLog(LOG_LEVEL_DEBUG,"Notify has enable !");
                return true;
            }

            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            if(notify) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }else{
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }

            broadcastLog(LOG_LEVEL_DEBUG, "gatt.writeDescriptor(" + descriptor.getUuid() + (notify ? ", value=0x01-00)" : ", value=0x02-00)"));
            mBluetoothGatt.writeDescriptor(descriptor);

            try {
                synchronized (mLock) {
                    while ((!enabled && mConnected && mError == 0)) {
                        mLock.wait();
                        // Check the value of the descriptor
                        enabled = descriptor.getValue() != null && descriptor.getValue().length == 2 && descriptor.getValue()[0] > 0 && descriptor.getValue()[1] == 0;
                    }
                }
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }

            broadcastLog(LOG_LEVEL_DEBUG,"enable Notify "+characteristicUUID+" success is :" +enabled);
            return enabled;
        }
        broadcastLog(LOG_LEVEL_DEBUG,"enable Notify fail! service uuid:"+serviceUUID.toString()+" not exist");
        return false;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // Check whether an error occurred
            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (newState == BluetoothGatt.STATE_CONNECTED) {

                    broadcastLog(LOG_LEVEL_DEBUG,"Connected to GATT server");

                    mConnected = true;
                    mConnectionState = STATE_CONNECTED;
                    if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_BONDED) {
                        BLog.e(TAG,"Waiting 1600 ms for a possible Service Changed indication...");
                        waitFor(1600);
                    }

                    // Attempts to discover services after successful connection.
                    final boolean success = gatt.discoverServices();
                    BLog.d(TAG,"Attempting to start service discovery... " + (success ? "succeed" : "failed"));
                    if (!success) {
                        mError = ERROR_SERVICE_DISCOVERY_NOT_STARTED;
                    } else {
                        // Just return here, lock will be notified when service discovery finishes
                        return;
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    BLog.d(TAG,"Disconnected from GATT server");
                    broadcastLog(LOG_LEVEL_DEBUG,"Disconnected from GATT server");
                    mConnectionState = STATE_DISCONNECTED;
                    mConnected = false;
                    mLock.notifyAll();
                }

            } else {

                if (status == 0x08 /* GATT CONN TIMEOUT */ || status == 0x13 /* GATT CONN TERMINATE PEER USER */) {
                   // BLog.d(TAG, "Target device disconnected with status: " + status);
                    broadcastLog(LOG_LEVEL_DEBUG,"Target device disconnected with status: " + status+",status info:"+ GattError.parse(status));
                }else {
                   // BLog.e(TAG, "Connection state change error: " + status + " newState: " + newState);
                    broadcastLog(LOG_LEVEL_DEBUG,"Connection state change error: " + status + " newState: " + newState+",status info:"+GattError.parse(status));
                }
                mError = ERROR_CONNECTION_STATE_MASK | status;
                if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mConnectionState = STATE_DISCONNECTED;
                }
                mConnected = false;
            }

            Log.d(TAG, "mConnectionState:" + mConnectionState);

            // Notify waiting thread
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {

            long endTime = System.currentTimeMillis();
            broadcastLog(LOG_LEVEL_DEBUG, "Connect cost:" + (endTime - startTime) +" millisecond");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                BLog.d(TAG,"Services discovered");
                mConnectionState = STATE_CONNECTED_AND_READY;
            } else {
                BLog.e(TAG,"Service discovery error: " + status);
                mError = ERROR_CONNECTION_MASK | status;
            }

            // Notify waiting thread
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastLog(LOG_LEVEL_DEBUG,characteristic.getUuid()+" onCharacteristicWrite success!");
                notifyLock();
            } else {
                broadcastLog(LOG_LEVEL_DEBUG,characteristic.getUuid()+" onCharacteristicWrite error status:"+status+",info:"+GattError.parse(status));
                mError = ERROR_CONNECTION_MASK | status;
                notifyLock();

            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG,"onCharacteristicRead:"+ characteristic.getUuid().toString()) ;
            broadcastLog(LOG_LEVEL_DEBUG,characteristic.getUuid()+" onCharacteristicRead success!");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            broadcastLog(LOG_LEVEL_DEBUG,"rv:"+characteristic.getUuid()+":" + ByteUtils.byte2HexStr(characteristic.getValue()));

            if(characteristic.getUuid().toString().equals(UUID_READ.toString())){

                mReceivedData = characteristic.getValue();
                mRequestCompleted = true;
                notifyLock();
            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastLog(LOG_LEVEL_DEBUG,"onDescriptorWrite success,"+ descriptor.getUuid().toString()) ;
                if (CLIENT_CHARACTERISTIC_CONFIG.toString().equals(descriptor.getUuid().toString())) {

                }
            } else {
                broadcastLog(LOG_LEVEL_DEBUG,"Descriptor write "+descriptor.getUuid().toString()+" error: " + status);
                mError = ERROR_CONNECTION_MASK | status;
            }
            notifyLock();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            broadcastLog(LOG_LEVEL_DEBUG,"onDescriptorRead success,"+ descriptor.getUuid().toString()) ;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (CLIENT_CHARACTERISTIC_CONFIG.equals(descriptor.getUuid())) {
                    if (CLIENT_CHARACTERISTIC_CONFIG.toString().equals(descriptor.getCharacteristic().getUuid().toString())) {
                        // We have enabled indications for the Service Changed characteristic
                        mRequestCompleted = true;
                    } else {
                        // reading other descriptor is not supported
                        Log.d(TAG,"Unknown descriptor read"); // this have to be implemented if needed
                    }
                }
            } else {
                mError = ERROR_CONNECTION_MASK | status;
            }
            notifyLock();
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mMTUSize = mtu;
                broadcastLog(LOG_LEVEL_DEBUG,"onMtuChanged ,mtu size:"+mtu);
            }
        }
    };


    protected void notifyLock() {
        synchronized (mLock) {
            mLock.notifyAll();
        }
    }


    protected BluetoothGatt connect(@NonNull final String address) {

        broadcastLog(LOG_LEVEL_DEBUG,"start connect:"+address);

        if (!mBluetoothAdapter.isEnabled())
            return null;

        mConnectionState = STATE_CONNECTING;

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        final BluetoothGatt gatt = device.connectGatt(this, false, mGattCallback);

        if(gatt != null){
            if (gatt.connect() == false) {
                broadcastLog(LOG_LEVEL_DEBUG,"gatt.connect() return false！");
                return null;
            }
        }
        // We have to wait until the device is connected and services are discovered
        // Connection error may occur as well.
        try {
            synchronized (mLock) {
                while ((mConnectionState == STATE_CONNECTING || mConnectionState == STATE_CONNECTED) && mError == 0)
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
            BLog.e(TAG,"Sleeping interrupted");
        }
        return gatt;
    }

    /**
     * Wait for given number of milliseconds.
     *
     * @param millis waiting period.
     */
    protected void waitFor(final long millis) {
        synchronized (mLock) {
            try {
                mLock.wait(millis);
            } catch (final InterruptedException e) {
                e.printStackTrace();
                BLog.e(TAG,"Sleeping interrupted");
            }
        }
    }


    protected void broadcastLog(int logLevel,String message) {
        Intent intentLog = new Intent(BROADCAST_LOG);
        intentLog.putExtra(BROADCAST_LOG_EXTRA_TAG, TAG);
        intentLog.putExtra(BROADCAST_LOG_EXTRA_LEVEL, logLevel);
        intentLog.putExtra(BROADCAST_LOG_EXTRA_MSG, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentLog);
    }

    protected void broadcastLog(Exception e) {
        Intent intentLog = new Intent(BROADCAST_LOG);
        intentLog.putExtra(BROADCAST_LOG_EXTRA_TAG, TAG);
        intentLog.putExtra(BROADCAST_LOG_EXTRA_LEVEL, LOG_LEVEL_ERROR);
        intentLog.putExtra(BROADCAST_LOG_EXTRA_EXCEPTION,e);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentLog);
    }





}
