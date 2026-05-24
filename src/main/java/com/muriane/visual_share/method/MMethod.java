package com.muriane.visual_share.method;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MMethod {
    public static <T> String getTimeId(T object){
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date(currentTimeMillis);
        return formatter.format(date) + "-" + object.hashCode();
    }
}
