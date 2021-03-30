package com.example.musicplayer.util;

import android.annotation.SuppressLint;

/**
 * Created by 残渊 on 2018/10/22.
 */

public class MediaUtil {

    public static String formatTime(long time) {
        String min = time / 60 + "";
        String sec = time % 60 + "";
        if(sec.length()<2){
            sec = "0"+sec;
        }
        return min + ":" + sec;
    }
    public static String formatSinger(String singer){
        if(singer.contains("/")){
            String[]s=singer.split("/");
            singer=s[0];
        }
        return singer.trim();
    }

    @SuppressLint("DefaultLocale")
    public static String formatSize(long size){
        double d = (double) size/1024/1024;
        return String.format("%.1f",d);
    }



}


