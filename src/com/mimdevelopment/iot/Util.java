/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mimdevelopment.iot;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author Luther Stanton
 */
public class Util {

    private static final int MILLISECONDS_TO_HOUR_CONVERSION_FACTOR = 1000 * 60 * 60;

    public static String formatUTCDate(Calendar srcCalendar) {

        // make the time adjustment to GMT
        TimeZone timeZone = srcCalendar.getTimeZone();
        if (!(timeZone.getID().equalsIgnoreCase("GMT"))) {
            long millisecondGmtOffset = timeZone.getOffset(new Date().getTime());
            int timeZoneOffset = (int) (millisecondGmtOffset / MILLISECONDS_TO_HOUR_CONVERSION_FACTOR);
            srcCalendar.add(Calendar.HOUR_OF_DAY, (timeZoneOffset * -1));
        }
        
        StringBuilder timeFormat = new StringBuilder(Integer.toString(srcCalendar.get(Calendar.YEAR)));
        timeFormat.append(String.format("-%02d-",  new Integer((srcCalendar.get(Calendar.MONTH) + 1))));
        timeFormat.append(String.format("%02dT", new Integer(srcCalendar.get(Calendar.DAY_OF_MONTH))));
        timeFormat.append(String.format("%02d", new Integer(srcCalendar.get(Calendar.HOUR_OF_DAY))));
        timeFormat.append(String.format(":%02d:", new Integer(srcCalendar.get(Calendar.MINUTE))));
        timeFormat.append(String.format("%02dZ", new Integer(srcCalendar.get(Calendar.SECOND))));

        return timeFormat.toString();
    }

    public static String formatDateForLogging(Calendar srcCalendar) {

        StringBuilder timeFormat = new StringBuilder(String.format("%02d", new Integer(srcCalendar.get(Calendar.MONTH) + 1)));
        timeFormat.append(String.format("-%02d-", new Integer((srcCalendar.get(Calendar.DAY_OF_MONTH)))));
        timeFormat.append(String.format(Integer.toString(srcCalendar.get(Calendar.YEAR))));
        timeFormat.append(String.format("  %02d", new Integer(srcCalendar.get(Calendar.HOUR_OF_DAY))));
        timeFormat.append(String.format(":%02d:", new Integer(srcCalendar.get(Calendar.MINUTE))));
        timeFormat.append(String.format("%02d.", new Integer(srcCalendar.get(Calendar.SECOND))));
        timeFormat.append(String.format("%03d", new Integer(srcCalendar.get(Calendar.MILLISECOND))));

        return timeFormat.toString();
    }
}
