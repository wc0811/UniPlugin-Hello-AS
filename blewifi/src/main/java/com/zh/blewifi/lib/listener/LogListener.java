package com.zh.lib.listener;

public interface LogListener {

    public void logInfo(String tag, String message);
    public void logError(String tag, Exception exception);

}
