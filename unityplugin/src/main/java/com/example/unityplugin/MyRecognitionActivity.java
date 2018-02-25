package com.example.unityplugin;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class MyRecognitionActivity extends UnityPlayerActivity {
    Context mContext = null;       // Listenerからも参照できるようにMyRecognitionActivityのthisを格納
    private SpeechRecognizer sr;   // Androidの音声認識器
    String str;                     // 認識した文字列を格納　1つしか格納できないので取りこぼしの恐れあり要改良
    private static final String TAG = "MyRecognitionActivity";      // デバッグ用のタグ
    private final String ACTION_NAME = "startRecognitionAction";   // よくわかんないｗ
    BroadcastReceiver mBroadcastReceiver;                               // よくわかんないｗ
    private SearchWeather sw;       //SeacthWeatherクラスのインスタンス変数

    // アプリ起動時の一番最初に呼ばれる
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //SeacthWeatherクラスのインスタンス化
        sw=new SearchWeather();

        // Activityの上書きが成功してるかどうか見るためにToastを表示している
        // ...正直このめんどい方法じゃなくてもできたかもしれない...
        Toast.makeText(this, TAG + ": activity overriding is successful", Toast.LENGTH_LONG).show();

        mContext = this;
        str = "";
        registerBroadcastReceiver();
        mBroadcastReceiver = new BroadcastReceiver() {
            // startRecognition
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.i(action, action);

                if(action.equals(ACTION_NAME)) {
                    // startRecognitionでsendBroadcastするとここで受け取られる
                    // ここはUIスレッドで動くのでSpeechRecognizerが正常に起動できる
                    SpeechRecognizer sr2;
                    sr2 = SpeechRecognizer.createSpeechRecognizer(mContext);

                    // 認識とかエラーの処理が書いてあるListenerを登録
                    sr2.setRecognitionListener(new Listener());

                    // 実際にSpeechRecognizerを起動
                    sr2.startListening(new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS));
                }
            }
        };
        registerBroadcastReceiver();
    }

    public void startRecognition() {
        // 音声認識をスタートするときUnity側のコードから呼ばれる（Unity側で呼ぶ）メソッド
        // AndroidのSpeechRecognizerはUIスレッドってところで
        // 起動する必要があり，Unityから呼ばれたコード（このコード）はUIスレッドでは実行されないので
        // sendBroadcastで起動要求をUIスレッドで動いてるBroadcastReceiverに送って
        // UIスレッドでSpeechRecognizerを起動するようにしている
        // ...と思う（自信なし
        // コードはほとんどここのパクリです...
        // https://qiita.com/CST_negi/items/aac8337b4748a658473f
        // https://gist.github.com/NegishiTakumi/ba7d678a13b85317db48
        Intent mIntent = new Intent(ACTION_NAME);
        sendBroadcast(mIntent);
    }

    public void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_NAME);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    //unity側に認識した文字列を渡す
    public void sendUnity(String getstr){
        UnityPlayer.UnitySendMessage("VoiceRecognitionObject","onCallBackString",getstr);
    }

    // 認識とかエラーの処理が書いてある""Listener
    class Listener implements RecognitionListener {
        // エラー情報がintなのでデバッグでわかりやすく文字列で表示できるように
        // HashMapで整数から文字列の対応表を作っている
        private final HashMap<Integer, String> errorMessageTable =
                new HashMap<Integer, String>(){{
                    put(SpeechRecognizer.ERROR_AUDIO, "ERROR_AUDIO");
                    put(SpeechRecognizer.ERROR_CLIENT, "ERROR_CLIENT");
                    put(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, "ERROR_INSUFFICIENT_PERMISSIONS");
                    put(SpeechRecognizer.ERROR_NETWORK, "ERROR_NETWORK");
                    put(SpeechRecognizer.ERROR_NETWORK_TIMEOUT, "ERROR_NETWORK_TIMEOUT");
                    put(SpeechRecognizer.ERROR_NO_MATCH, "ERROR_NO_MATCH");
                    put(SpeechRecognizer.ERROR_RECOGNIZER_BUSY, "ERROR_RECOGNIZER_BUSY");
                    put(SpeechRecognizer.ERROR_SERVER, "ERROR_SERVER");
                    put(SpeechRecognizer.ERROR_SPEECH_TIMEOUT, "ERROR_SPEECH_TIMEOUT");
                }};

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            // 認識終了したとき呼ばれる処理
            // 現状，一回だけ言葉を認識するとSpeechRecognizerは終了してしまうので
            // 終了したらもう一度起動してやる必要がある
            // 要改良
            Log.d(TAG, "onEndOfSpeech");

        }

        // エラー時の処理
        // とりあえずログとToastに表示するだけ
        // 致命的なエラーの時もここが呼ばれるのでちゃんと例外投げたりしたほうがいいはず
        // 認識失敗エラーのときだけ音声認識を再起動
        @Override
        public void onError(int error) {
            String message = "RECOGNITION ERROR: " +
                    (errorMessageTable.containsKey(error) ?
                            errorMessageTable.get(error) : "UNKNOWN KEY");
            Log.d(TAG, message);
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();

            if(error == SpeechRecognizer.ERROR_NO_MATCH) {
                Intent mIntent = new Intent(ACTION_NAME);
                sendBroadcast(mIntent);
            }
        }

        // 結果の1つをとりあえずstrに格納　本当は結果のArrayList配列をそのままUnity側に渡したいが
        // やり方がわからない
        // あとログに出力
        @Override
        public void onResults(Bundle results) {
            String str = "";
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            String debugMessage = "RECOGNITION RESULT: " + (String)data.get(0);
            Log.d(TAG, debugMessage);

            if(data.size() > 0){
                str = (String)data.get(0);
                sendUnity(str);
            }
        }

        // 認識の途中に呼ばれる
        // partialResultsは認識途中の文字列の断片
        // とりあえずログに出すだけ
        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList data =
                    partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            String debugMessage = "PARTIAL RECOGNITION RESULT: " + (String)data.get(0);
            Log.d(TAG, debugMessage);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }


    //天気の情報を取得する関数
    //音声認識のクラスに入れちゃったけど別にどこでもいい
    //そのときはunity側のコードを変える必要があるよ
    public void searchWeatherMethod(){
        sw.execute();
    }
}