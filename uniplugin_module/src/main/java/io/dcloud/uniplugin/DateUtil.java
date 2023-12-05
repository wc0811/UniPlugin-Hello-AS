package io.dcloud.uniplugin;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {


    public static final String DATE_TIME_FORMAT_YYYY_MM_DD_HH_MI_SS = "HH:mm:ss:SSS";

    public static String getCurrentDateStr() {
        return dateToStr(new Date(), DATE_TIME_FORMAT_YYYY_MM_DD_HH_MI_SS);
    }

    public static String getCurrentDateStr2() {
        return dateToStr(new Date(), "yyyy-MM-dd HH:mm:ss");
    }

    public static Date strToDate(String strDate, String dateFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        Date date = null;
        try {
            date = sdf.parse(strDate);
        } catch (Exception e) {

        }
        return date;
    }


    public static String dateToStr(Date date, String tarDateFormat) {
        return new SimpleDateFormat(tarDateFormat).format(date);
    }


    public static String strToStr(String strDate, String srcFormat, String tarFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat(srcFormat);
        try {
            Date date = sdf.parse(strDate);
            sdf = new SimpleDateFormat(tarFormat);
            strDate = sdf.format(date);
        } catch (Exception e) {

        }
        return strDate;
    }
}
