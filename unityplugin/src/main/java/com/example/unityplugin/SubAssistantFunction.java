package com.example.unityplugin;


import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class SubAssistantFunction {

    //現在時刻の取得
    public String getNowTime(){
        DateFormat dateFormat = new SimpleDateFormat("現在の時刻はHH時mm分です");
        Date date = new Date(System.currentTimeMillis());
        return dateFormat.format(date);
    }

    //今日の日にちの取得
    public String getNowDate(){
        DateFormat dateFormat = new SimpleDateFormat("今日の日付はyyyy年MM月dd日です");
        Date date = new Date(System.currentTimeMillis());
        return dateFormat.format(date);
    }
}
