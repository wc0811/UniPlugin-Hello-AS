package io.dcloud.uniplugin.lib;

import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_CONFIG_CONECTED;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_CONFIG_ERR_APP_CRC;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_CONFIG_ERR_CON;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_CONFIG_ERR_CON_PWD;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_CONFIG_ERR_CON_TIMEOUT;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_CONFIG_ERR_DEV_CRC;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_CONFIG_SUCCESS;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_CONFIG_UUID_NOT_EXIST;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_LOG;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_LOG_EXTRA_EXCEPTION;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_LOG_EXTRA_LEVEL;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_LOG_EXTRA_MSG;
import static io.dcloud.uniplugin.lib.service.ConfigService.BROADCAST_LOG_EXTRA_TAG;
import static io.dcloud.uniplugin.lib.service.ConfigService.EXTRA_DEVICE_ADDRESS;
import static io.dcloud.uniplugin.lib.service.ConfigService.EXTRA_PASSWORD;
import static io.dcloud.uniplugin.lib.service.ConfigService.EXTRA_SSID;
import static io.dcloud.uniplugin.lib.service.ConfigService.EXTRA_TIMEOUT;
import static io.dcloud.uniplugin.lib.service.ConfigService.LOG_LEVEL_DEBUG;
import static io.dcloud.uniplugin.lib.service.ConfigService.LOG_LEVEL_ERROR;
import static io.dcloud.uniplugin.lib.service.ConfigService.LOG_LEVEL_INFO;
import static io.dcloud.uniplugin.lib.service.ConfigService.LOG_LEVEL_WARN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import io.dcloud.uniplugin.lib.error.ErrCode;
import io.dcloud.uniplugin.lib.listener.ConfigListener;
import io.dcloud.uniplugin.lib.listener.LogListener;
import io.dcloud.uniplugin.lib.service.ConfigService;

public  class ConfigHelper {

    private static final String TAG = ConfigHelper.class.getSimpleName();

    private  int timeoutMilliscond = 20*1000;
    private  String deviceAddress;
    private  byte[] ssidBytes;
    private  String password;
    private  boolean startAsForegroundService;

    private static LogBroadcastReceiver mLogBroadcastReceiver;
    private static ConfigBroadcastReceiver mConfigBroadcastReceiver;

    public ConfigHelper() {

    }

    public ConfigHelper setTimeoutMilliscond(int timeoutMilliscond) {
        this.timeoutMilliscond = timeoutMilliscond;
        return this;
    }

    public ConfigHelper setSsid(byte[] ssidBytes) {
        this.ssidBytes = ssidBytes;
        return this;
    }

    public ConfigHelper setPassword(String password) {
        this.password = password;
        return this;
    }

    public ConfigHelper(@NonNull final String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public ConfigHelper setDeviceAddress(@NonNull final String deviceAddress) {
       this.deviceAddress = deviceAddress;
       return this;
    }

    public ConfigHelper setForeground(final boolean foreground) {
        this.startAsForegroundService = foreground;
        return this;
    }


    public boolean startConfig(@NonNull final Context context, @NonNull final Class<? extends ConfigService> service){

        if(TextUtils.isEmpty(deviceAddress) || ssidBytes == null || TextUtils.isEmpty(password)){
            Log.d(TAG, "param error...");
            return false;
        }

        final Intent intent = new Intent(context, service);
        intent.putExtra(EXTRA_DEVICE_ADDRESS,deviceAddress);
        intent.putExtra(EXTRA_SSID,ssidBytes);
        intent.putExtra(EXTRA_PASSWORD,password);
        intent.putExtra(EXTRA_TIMEOUT,timeoutMilliscond);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && startAsForegroundService) {
            // On Android Oreo and above the service must be started as a foreground service to make it accessible from
            // a killed application.
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        return true;
    }


    public ConfigHelper registerLogListener(@NonNull final Context context, @NonNull final LogListener listener){

        if (mLogBroadcastReceiver == null) {
            mLogBroadcastReceiver = new LogBroadcastReceiver();
            final IntentFilter filter = new IntentFilter();
            filter.addAction(BROADCAST_LOG);
            LocalBroadcastManager.getInstance(context).registerReceiver(mLogBroadcastReceiver, filter);
        }

        mLogBroadcastReceiver.setmLogListener(listener);
        return this;
    }

    public ConfigHelper unRegisterLogListener(@NonNull final Context context){
        if(mLogBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(mLogBroadcastReceiver);
        }
        mLogBroadcastReceiver = null;
        return this;
    }

    public ConfigHelper registerConfigListener(@NonNull final Context context, @NonNull final ConfigListener listener){

        if (mConfigBroadcastReceiver == null) {
            mConfigBroadcastReceiver = new ConfigBroadcastReceiver();
            final IntentFilter filter = new IntentFilter();
            filter.addAction(BROADCAST_CONFIG_ERR_CON);
            filter.addAction(BROADCAST_CONFIG_CONECTED);
            filter.addAction(BROADCAST_CONFIG_ERR_DEV_CRC);
            filter.addAction(BROADCAST_CONFIG_ERR_CON_TIMEOUT);
            filter.addAction(BROADCAST_CONFIG_ERR_CON_PWD);
            filter.addAction(BROADCAST_CONFIG_UUID_NOT_EXIST);
            filter.addAction(BROADCAST_CONFIG_ERR_APP_CRC);
            filter.addAction(BROADCAST_CONFIG_SUCCESS);
            LocalBroadcastManager.getInstance(context).registerReceiver(mConfigBroadcastReceiver, filter);
        }

        mConfigBroadcastReceiver.setmConfigListener(listener);
        return this;
    }

    public ConfigHelper unRegisterConfigListener(@NonNull final Context context){
        if(mConfigBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(mConfigBroadcastReceiver);
        }
        mConfigBroadcastReceiver = null;
        return this;
    }

    private static class LogBroadcastReceiver extends BroadcastReceiver {

        private LogListener mLogListener;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action =  intent.getAction();
            if(action.equals(BROADCAST_LOG)){
                int logLevel  = intent.getIntExtra(BROADCAST_LOG_EXTRA_LEVEL,LOG_LEVEL_DEBUG);
                switch (logLevel){
                    case LOG_LEVEL_DEBUG:
                    case LOG_LEVEL_INFO:
                    case LOG_LEVEL_WARN:
                        String message = intent.getStringExtra(BROADCAST_LOG_EXTRA_MSG);
                        String tag = intent.getStringExtra(BROADCAST_LOG_EXTRA_TAG);
                        if(mLogListener != null){
                            mLogListener.logInfo(tag,message);
                        }
                        break;
                    case LOG_LEVEL_ERROR:
                        Exception exception = (Exception)intent.getSerializableExtra(BROADCAST_LOG_EXTRA_EXCEPTION);
                        String logTag = intent.getStringExtra(BROADCAST_LOG_EXTRA_TAG);
                        if(mLogListener != null){
                            mLogListener.logError(logTag,exception);
                        }
                        break;
                }
            }
        }

        public void setmLogListener(LogListener mLogListener) {
            this.mLogListener = mLogListener;
        }
    }

    private static class ConfigBroadcastReceiver extends BroadcastReceiver {

        private ConfigListener mConfigListener;

        public void setmConfigListener(ConfigListener mConfigListener) {
            this.mConfigListener = mConfigListener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(mConfigListener == null){
                return;
            }
            String action =  intent.getAction();
            if(action.equals(BROADCAST_CONFIG_CONECTED)){
                mConfigListener.onConnect();
            }else if(action.equals(BROADCAST_CONFIG_ERR_DEV_CRC)){
                mConfigListener.onFail(ErrCode.CRC_DEV_VERTIFY_FAIL);
            }else if(action.equals(BROADCAST_CONFIG_ERR_CON_TIMEOUT)){
                mConfigListener.onFail(ErrCode.CON_TEIMOUT);
            }else if(action.equals(BROADCAST_CONFIG_ERR_CON_PWD)){
                mConfigListener.onFail(ErrCode.PWD_ERROR);
            }else if(action.equals(BROADCAST_CONFIG_ERR_CON)){
                mConfigListener.onFail(ErrCode.CON_FAIL);
            }else if(action.equals(BROADCAST_CONFIG_UUID_NOT_EXIST)){
                mConfigListener.onFail(ErrCode.UUID_NOT_EXIST);
            }else if(action.equals(BROADCAST_CONFIG_SUCCESS)){
                mConfigListener.onSuccess();
            }else if(action.equals(BROADCAST_CONFIG_ERR_APP_CRC)){
                mConfigListener.onFail(ErrCode.CRC_APP_VERTIFY_FAIL);
            }
        }
    }
}
