package io.dcloud.uniplugin.lib.listener;


import io.dcloud.uniplugin.lib.error.ErrCode;

public abstract class ConfigListener {

    public void onConnect(){

    }
    public void onDisConnect(){

    }

    //配网成功
    public abstract void onSuccess();
    //配网成功
    public abstract void onFail(ErrCode errCode);
}
