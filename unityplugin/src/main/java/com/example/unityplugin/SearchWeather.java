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

public class SearchWeather extends AsyncTask<Void, Void, String>{


    //バックグラウンドでの処理
    @Override
    protected String doInBackground(Void...String) {
        // 取得したテキストを格納する変数
        String readStr="";
        // アクセス先URL
        URL url=null;
        final String baseURL="https://map.yahooapis.jp/weather/V1/place";
        //アプリケーションID
        final String appID="dj00aiZpPUozcklHeUJsV1FCdiZzPWNvbnN1bWVyc2VjcmV0Jng9OGM-";
        //出力方式
        final String output="json";
        //緯度経度
        String coordinates="139.732293,35.663613";

        HttpURLConnection con = null;

        try {
            //URLの作成
            url=new URL(baseURL+"?coordinates="+coordinates+"&appid="+appID+"&output="+output);

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

    //バックグランドからの結果をmainなどに返す
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

}
