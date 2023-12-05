package com.zh.blewifi.lib.error;

import androidx.annotation.NonNull;

public enum ErrCode{
    CRC_DEV_VERTIFY_FAIL,//
    CRC_APP_VERTIFY_FAIL,//
    CON_TEIMOUT,//连接路由器超时了
    PWD_ERROR,//SSID 或 密码错误
    CON_FAIL,
    UUID_NOT_EXIST,
    ConnectErr//升级过程中，连接断开了
    ;

    @NonNull
    @Override
    public String toString() {
        if(this.ordinal() == CRC_DEV_VERTIFY_FAIL.ordinal()){
            return "Dev CRC失败";
        }

        if(this.ordinal() == CON_TEIMOUT.ordinal()){
            return "连接路由器超时";
        }

        if(this.ordinal() == PWD_ERROR.ordinal()){
            return "连接路由器失败,SSID或密码错误";
        }

        if(this.ordinal() == CON_FAIL.ordinal()){
            return "连接BLE设备失败";
        }

        if(this.ordinal() == UUID_NOT_EXIST.ordinal()){
            return "配网服不存在，不支持配网";
        }

        if(this.ordinal() == CRC_APP_VERTIFY_FAIL.ordinal()){
            return "CRC 校验失败";
        }


        return "未知异常";
    }
}
