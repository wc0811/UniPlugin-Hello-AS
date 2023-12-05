package io.dcloud.uniplugin.lib.service;

import androidx.annotation.NonNull;

public enum ProcessState {

    Connecting,
    Connected,
    DisconveringServeice,
    DisconveredServeice,
    ReadingVersion,
    ReadedVersion,
    WriteFileHeader,
    SendingFile,
    Completed,
    DisConnected;

    @NonNull
    @Override
    public String toString() {
        if(this.ordinal() == Connecting.ordinal()){
            return "开始连接设备";
        }else if(this.ordinal() == Connected.ordinal()){
            return "连接设备成功";
        }else if(this.ordinal() == DisconveringServeice.ordinal()){
            return "开始搜索服务";
        }else if(this.ordinal() == DisconveredServeice.ordinal()){
            return "成功搜索到服务，开始打开FFC1 Notify";
        }else if(this.ordinal() == ReadingVersion.ordinal()){
            return "FFC1 Notify成功，开始读取设备版本号";
        }else if(this.ordinal() == ReadedVersion.ordinal()){
            return "读取设备版本成功,开始打开FFC2 Notify";
        }else if(this.ordinal() == WriteFileHeader.ordinal()){
            return "FFC2 Notify成功,开始写配置信息，等待设备更新参数";
        }else if(this.ordinal() == SendingFile.ordinal()){
            return "设备更新连接参数成功，开始发送Bin文件";
        }else if(this.ordinal() == Completed.ordinal()){
            return "Bin文件发送完成,升级成功！";
        }else if(this.ordinal() == DisConnected.ordinal()){
            return "连接失败！";
        }
        return "未知状态:"+this.ordinal();
    }
}
