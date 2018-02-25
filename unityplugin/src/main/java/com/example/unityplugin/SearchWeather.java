package com.example.unityplugin;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.io.UnsupportedEncodingException;
import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchWeather extends AsyncTask<Void, Void, String>{


    //バックグラウンドでの処理
    @Override
    protected String doInBackground(Void...String) {
        // 取得したテキストを格納する変数
        String readStr="";
        // アクセス先URL
        URL url=null;
        final String baseURL="http://weather.livedoor.com/forecast/webservice/json/v1";
        //地域ID
        //今は東京を設定してる
        String location="130010";

        HttpURLConnection con = null;

        try {
            //URLの作成
            url=new URL(baseURL+"?city="+location);

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
                InputStream in=con.getInputStream();
                readStr=readInputStream(in);
                readStr=parseJson(readStr);
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
        UnityPlayer.UnitySendMessage("VoiceRecognitionObject","onCallBackWeather",str);
    }


    //json出力のための関数
    public String readInputStream(InputStream in) throws IOException, UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        String st = "";

        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        while((st = br.readLine()) != null)
        {
            sb.append(st);
        }
        try
        {
            in.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return sb.toString();
    }


    //jsonから必要な情報を抜き出す関数
    //天気の情報をunityに送る形にする
    //送る文字列＞＞   ("予報日(今日，明日など)","天気","最高気温","最低気温")
    //送る情報の間には","を入れるようにして区切るようにした
    //この天気APIは下のURLです
    //http://weather.livedoor.com/weather_hacks/webservice
    public String parseJson(String str){
        String day,weather,maxTemp,minTemp;
        String result=null;
        maxTemp=minTemp=null;
        try {
            JSONArray jsonArray = new JSONObject(str).getJSONArray("forecasts");
            JSONObject jsonData=jsonArray.getJSONObject(0);
            day=jsonData.getString("dateLabel");                    //
            weather=jsonData.getString("telop");
            JSONObject temperature=jsonData.getJSONObject("temperature");
            if(temperature.get("max")!=JSONObject.NULL) {
                JSONObject maxData = temperature.getJSONObject("max");
                maxTemp = maxData.getString("celsius");
            }
            if(temperature.get("min")!=JSONObject.NULL){
                JSONObject minData = temperature.getJSONObject("min");
                maxTemp = minData.getString("celsius");
            }

            result=day+","+weather+","+maxTemp+","+minTemp;

        }catch (JSONException e){
            e.printStackTrace();
        }
        return result;
    }

}
