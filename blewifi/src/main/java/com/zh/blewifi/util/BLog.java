package com.zh.util;

import android.util.Log;

public class BLog {

    private static boolean DEGUB = true;
    public static void d(String tag, String message) {
        if (DEGUB) {
            Log.d(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (DEGUB) {
            Log.e(tag, message);
        }
    }

    public  static void i(String tag, String message) {
        if (DEGUB) {
            Log.i(tag, message);
        }
    }

}
