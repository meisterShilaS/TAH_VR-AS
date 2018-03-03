package com.example.unityplugin;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.io.UnsupportedEncodingException;
import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchWeather extends AsyncTask<Void, Void, String> {

    String readStr = "";          // 取得したテキストを格納する変数かつjsonをパースした文字列
    private String latitude;    //緯度
    private String longitude;   //経度
    private String baseURL;    //url
    URL url;                    // URL変数
    private int searchDay;      //いつの天気を調べるかの変数　0.現在　1.今日　2.明日　3.明後日


    public SearchWeather(String lat, String lon, int day) {
        this.latitude = lat;
        this.longitude = lon;
        this.searchDay = day;
    }

    //バックグラウンドでの処理
    @Override
    protected String doInBackground(Void... String) {

        HttpURLConnection con = null;

        if (searchDay == 0) {
            baseURL = "http://api.openweathermap.org/data/2.5/forecast?lat=" + latitude + "&lon=" + longitude + "&cnt=1&APPID=efe88ff70224cf7cfa2dbb9adcd12024";
        } else {
            baseURL = "http://weather.livedoor.com/forecast/webservice/json/v1?city=130010";      //東京の天気
        }

        try {
            //URLの作成
            url = new URL(baseURL);

            // コネクション取得
            con = (HttpURLConnection) url.openConnection();
            // リダイレクトを自動で許可しない設定
            con.setInstanceFollowRedirects(false);
            // リクエストメソッドの設定
            con.setRequestMethod("GET");
            con.setDoInput(true);
            //接続
            con.connect();

            // HTTPレスポンスコード
            final int status = con.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                // 通信に成功した
                // テキストを取得する
                con.connect();
                InputStream in = con.getInputStream();
                readStr = readInputStream(in);
                if (searchDay == 0) {
                    readStr = parseJsonOWM(readStr);
                } else {
                    readStr = parseJsonLWWS(readStr);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                // コネクションを切断
                con.disconnect();
            }
        }
        return readStr;
    }


    //バックグランドからの結果("str")をunityの"VoiceRecognitionObject"（という名前のGameObject）のscriptの中にある関数"onCallBackWeather"に渡す
    @Override
    protected void onPostExecute(String str) {
        UnityPlayer.UnitySendMessage("VoiceRecognitionObject", "onCallBackWeather", str);
    }


    //json出力のための関数
    public String readInputStream(InputStream in) throws IOException, UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        String st = "";

        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        while ((st = br.readLine()) != null) {
            sb.append(st);
        }
        try {
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }


    //OpenWeatherMap
    //jsonから必要な情報を抜き出す関数
    //天気の情報をunityに送る形にする
    public String parseJsonOWM(String str) {
        String weather;
        Double temp;
        int intTemp, humidity;
        String result = null;

        try {
            JSONArray jsonArray = new JSONObject(str).getJSONArray("list");
            JSONObject jsonData = jsonArray.getJSONObject(0);

            JSONObject mainObj = jsonData.getJSONObject("main");

            temp = mainObj.getDouble("temp");     //気温
            humidity = mainObj.getInt("humidity");       //湿度

            //天気
            JSONArray weatherArray = jsonData.getJSONArray("weather");
            JSONObject weatherObj = weatherArray.getJSONObject(0);
            String iconId = weatherObj.getString("icon");

            switch (iconId) {
                case "01d":
                case "01n":
                    weather = "快晴";
                    break;
                case "02d":
                case "02n":
                    weather = "晴れ";
                    break;
                case "03d":
                case "03n":
                    weather = "くもり";
                    break;
                case "04d":
                case "04n":
                    weather = "くもり";
                    break;
                case "09d":
                case "09n":
                    weather = "小雨";
                    break;
                case "10d":
                case "10n":
                    weather = "雨";
                    break;
                case "11d":
                case "11n":
                    weather = "雷雨";
                    break;
                case "13d":
                case "13n":
                    weather = "雪";
                    break;
                case "50d":
                case "50n":
                    weather = "霧";
                    break;
                default:
                    weather = "不明";
                    break;
            }

            temp -= 273.15;
            BigDecimal bd = new BigDecimal(temp);
            BigDecimal bd1 = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
            intTemp = bd1.intValue();

            result = "現在の気温は" + String.valueOf(intTemp) + "度，湿度，" + String.valueOf(humidity) + "パーセントで" + weather + "です";

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }


    //Livedoor Weather Web Service / LWWS
    public String parseJsonLWWS(String str) {
        String day, weather, maxTemp, minTemp;
        String result = null;
        maxTemp = minTemp = "不明";
        int when = searchDay - 1;
        try {
            JSONArray jsonArray = new JSONObject(str).getJSONArray("forecasts");
            JSONObject jsonData = jsonArray.getJSONObject(when);
            day = jsonData.getString("dateLabel");
            weather = jsonData.getString("telop");
            JSONObject temperature = jsonData.getJSONObject("temperature");
            if (temperature.get("max") != JSONObject.NULL) {
                JSONObject maxData = temperature.getJSONObject("max");
                maxTemp = maxData.getString("celsius");
                maxTemp += "度";
            }
            if (temperature.get("min") != JSONObject.NULL) {
                JSONObject minData = temperature.getJSONObject("min");
                maxTemp = minData.getString("celsius");
                minTemp += "度";
            }

            result = day + "の天気は" + weather + "で，最高気温" + maxTemp + "，最低気温" + minTemp + "です";

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
