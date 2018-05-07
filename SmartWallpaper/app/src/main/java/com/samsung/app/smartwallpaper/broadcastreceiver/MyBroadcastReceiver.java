package com.samsung.app.smartwallpaper.broadcastreceiver;

/**
 * Created by ASUS on 2018/4/7.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.samsung.app.smartwallpaper.wakeup.WakeupService;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "HomeReceiver";
    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    private static final String SYSTEM_DIALOG_REASON_LOCK = "lock";
    private static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(LOG_TAG, "onReceive: action: " + action);
        if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
            // android.intent.action.CLOSE_SYSTEM_DIALOGS
            String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
            Log.i(LOG_TAG, "reason: " + reason);

            if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                // 短按Home键
                Log.i(LOG_TAG, "homekey");
                Toast.makeText(context, "检查到短按Home键",Toast.LENGTH_SHORT).show();
            }
            else if (SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)) {
                // 长按Home键 或者 activity切换键
                Log.i(LOG_TAG, "long press home key or activity switch");
                Toast.makeText(context, "检查到长按Home键",Toast.LENGTH_SHORT).show();
            }
            else if (SYSTEM_DIALOG_REASON_LOCK.equals(reason)) {
                // 锁屏
                Log.i(LOG_TAG, "lock");
                Toast.makeText(context, "检查到锁屏",Toast.LENGTH_SHORT).show();
            }
            else if (SYSTEM_DIALOG_REASON_ASSIST.equals(reason)) {
                // samsung 长按Home键
                Log.i(LOG_TAG, "assist");
                Toast.makeText(context, "检查Samsung长按Home键",Toast.LENGTH_SHORT).show();
            }

        }else if("android.intent.action.BOOT_COMPLETED".equals(action) || "android.media.AUDIO_BECOMING_NOISY".equals(action)){
            Intent i = new Intent(context, MyBroadcastReceiver.class);
            intent.setAction("arui.alarm.action");
            PendingIntent sender = PendingIntent.getBroadcast(context, 0,
                    intent, 0);
            long firstime = SystemClock.elapsedRealtime();
            AlarmManager am = (AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE);
            // 10秒一个周期,不停的发送广播
            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstime,
                    10 * 1000, sender);
        }else if("arui.alarm.action".equals(action)){
            Intent service = new Intent(context, WakeupService.class);
            context.startService(service);
            Toast.makeText(context, "AUDIO_BECOMING_NOISY-启动wakeup service",Toast.LENGTH_SHORT).show();
        }
    }
}