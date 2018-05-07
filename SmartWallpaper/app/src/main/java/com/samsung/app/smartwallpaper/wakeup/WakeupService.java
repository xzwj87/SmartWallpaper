package com.samsung.app.smartwallpaper.wakeup;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;

import com.baidu.speech.asr.SpeechConstant;
import com.samsung.app.smartwallpaper.ASRDialog;
import com.samsung.app.smartwallpaper.control.MyWakeup;
import com.samsung.app.smartwallpaper.recognization.PidBuilder;
import com.samsung.app.smartwallpaper.wakeup.IWakeupListener;
import com.samsung.app.smartwallpaper.wakeup.RecogWakeupListener;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.samsung.app.smartwallpaper.recognization.IStatus.STATUS_WAKEUP_SUCCESS;

public class WakeupService extends IntentService {

    private Context mContext;

    protected MyWakeup myWakeup;
    private int backTrackInMs = 1500;
    protected Handler handler= new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            handleMsg(msg);
        }

    };

    protected void handleMsg(Message msg) {
        if (msg.what == STATUS_WAKEUP_SUCCESS){
            // 此处 开始正常识别流程
            Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
            params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
            int pid = PidBuilder.create().model(PidBuilder.INPUT).toPId(); //如识别短句，不需要需要逗号，将PidBuilder.INPUT改为搜索模型PidBuilder.SEARCH
            params.put(SpeechConstant.PID, pid);
            if (backTrackInMs > 0) { // 方案1， 唤醒词说完后，直接接句子，中间没有停顿。
                params.put(SpeechConstant.AUDIO_MILLS, System.currentTimeMillis() - backTrackInMs);

            }
            Intent intent = new Intent(mContext, ASRDialog.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
    public WakeupService() {
        super("WakeupService");
        mContext = this;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IWakeupListener listener = new RecogWakeupListener(handler);
        myWakeup = new MyWakeup(this,listener);
        Map<String,Object> params = new HashMap<String,Object>();
        params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");//"assets:///WakeUp.bin" 表示WakeUp.bin文件定义在assets目录下

        // params.put(SpeechConstant.ACCEPT_AUDIO_DATA,true);
        //params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME,true);
        // params.put(SpeechConstant.IN_FILE,"res:///com/baidu/android/voicedemo/wakeup.pcm");
        // params里 "assets:///WakeUp.bin" 表示WakeUp.bin文件定义在assets目录下
        myWakeup.start(params);
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        myWakeup.release();
        super.onDestroy();
    }
}
