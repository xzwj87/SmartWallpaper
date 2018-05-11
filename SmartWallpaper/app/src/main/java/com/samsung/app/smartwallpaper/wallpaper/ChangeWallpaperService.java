package com.samsung.app.smartwallpaper.wallpaper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaActionSound;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.samsung.app.smartwallpaper.FavoriteListActivity;
import com.samsung.app.smartwallpaper.command.Action;
import com.samsung.app.smartwallpaper.model.WallpaperItem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper.EXTERNAL_MY_FAVORITE_WALLPAPER_DIR;

/**
 * Created by my2013.wang on 2018/4/25.
 */
public class ChangeWallpaperService extends Service {

    private static final String TAG = "ChangeWallpaperService";

    private Context mContext;

    //定义当前所显示的壁纸
    private AlarmManager alarmManager;
    private Intent intent;
    private PendingIntent pi;

    private SDCardListener mSDCardListener;

    private ArrayList<WallpaperItem> mWallpaperItems;
    private static int curPos = 0;
    private static int shakeRandomPos = 0;

    private SensorManager mSensorManager;
    private Vibrator mVibrator;
    private MediaActionSound mCameraSound;

    private static final int MSG_LOAD_DONE = 0;
    private static final int MSG_SENSOR_SHAKE = 1;
    private static final int MSG_TRIGGER_CHANGE = 2;


    private boolean isShakeListening = false;
    private boolean isTimerChangeRunning = false;

    private HandlerThread mWorkerThread = null;
    private Handler mHandler = null;
    private void initWorkerThread() {
        mWorkerThread = new HandlerThread("worker_thread");
        mWorkerThread.start();
        mHandler = new Handler(mWorkerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_LOAD_DONE:

                        break;
                    case MSG_SENSOR_SHAKE:
                        if (!isShakeListening) {
                            Intent intent = new Intent(mContext, ChangeWallpaperService.class);
                            intent.setAction(Action.ACTION_START_SHAKE_LISTEN);
                            startService(intent);
                        } else {
                            //mVibrator.vibrate(200);
                            if(mWallpaperItems.size() > 0) {
                                mHandler.removeMessages(MSG_SENSOR_SHAKE);
                                mHandler.removeMessages(MSG_TRIGGER_CHANGE);
                                int random = (int)(Math.random() * mWallpaperItems.size());
                                if(random == shakeRandomPos){
                                    shakeRandomPos++;
                                }else{
                                    shakeRandomPos = random;
                                }
                                Log.d(TAG, "shakeRandomPos="+shakeRandomPos);
                                WallpaperItem wallpaperItem = mWallpaperItems.get(shakeRandomPos % mWallpaperItems.size());
                                SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(wallpaperItem.getWallpaperDrawable());
                                mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                            }
                        }
                        break;
                    case MSG_TRIGGER_CHANGE:
                        if(mWallpaperItems.size() > 0) {
                            mHandler.removeMessages(MSG_TRIGGER_CHANGE);
                            Log.d(TAG, "curPos="+curPos);
                            WallpaperItem wallpaperItem = mWallpaperItems.get(curPos % mWallpaperItems.size());
                            SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(wallpaperItem.getWallpaperDrawable());
                            curPos = (curPos + 1) % mWallpaperItems.size();
                        }
                        break;
                }
            }

        };
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        // TODO Auto-generated method stub
        super.onCreate();

        mContext = this;
        mWallpaperItems = new ArrayList<>();

        initWorkerThread();
        loadWallpaperItems();

        alarmManager=(AlarmManager)getSystemService(Service.ALARM_SERVICE);
        intent = new Intent(mContext, ChangeWallpaperService.class);
        intent.setAction(Action.ACTION_TRIGGER_CHANGE_WALLPAPER);
        // 创建PendingIntent对象
        pi= PendingIntent.getService(mContext, 0, intent, 0);

        mSDCardListener = new SDCardListener(EXTERNAL_MY_FAVORITE_WALLPAPER_DIR);
        mSDCardListener.startWatching();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        mCameraSound = new MediaActionSound();
        mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
        if (mSensorManager != null) {
            mSensorManager.registerListener(sensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            isShakeListening = true;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand-intent="+intent);

        if(intent != null){
            String action = intent.getAction();
            Toast.makeText(this, "onStartCommand-action="+action, Toast.LENGTH_SHORT).show();
            if(Action.ACTION_START_TIMER_CHANGE_WALLPAPER.equals(action)){//启动自动切换壁纸
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+5000, pi);
//                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), pi);
//                } else {
//                    alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 5000, pi);
//                }
                if(!isTimerChangeRunning) {
                    //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000, pi);
                    isTimerChangeRunning = true;
                    executeFixedRate();
                }
            }else if(Action.ACTION_STOP_TIMER_CHANGE_WALLPAPER.equals(action)){//停止自动切换壁纸
//                alarmManager.cancel(pi);
                isTimerChangeRunning = false;
                mScheduledExecutor = null;
            }else if(Action.ACTION_TRIGGER_CHANGE_WALLPAPER.equals(action)){
                if(!isTimerChangeRunning) {
                    isTimerChangeRunning = true;
                    executeFixedRate();
                }
            }else if(Action.ACTION_START_SHAKE_LISTEN.equals(action)){
                if (mSensorManager != null) {
                    mSensorManager.registerListener(sensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
                    isShakeListening = true;
                }
            }else if(Action.ACTION_STOP_SHAKE_LISTEN.equals(action)){
                if (mSensorManager != null) {
                    mSensorManager.unregisterListener(sensorEventListener);
                }
                isShakeListening = false;
            }
        }
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        if(mWorkerThread != null) {
            mWorkerThread.quit();
            mWorkerThread = null;
        }

        if(mSDCardListener != null) {
            mSDCardListener.stopWatching();
            mSDCardListener = null;
        }

        if(alarmManager != null) {
            alarmManager.cancel(pi);
            isTimerChangeRunning = false;
            alarmManager = null;
        }
        if (mSensorManager != null) {// 取消监听器
            mSensorManager.unregisterListener(sensorEventListener);
            isShakeListening = false;
            mSensorManager = null;
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    AsyncTask<String, Void, String> mLoadTask;
    public void loadWallpaperItems() {
        Log.i(TAG, "loadWallpaperItems");
        mWallpaperItems.clear();
        if(mLoadTask != null){
            mLoadTask.cancel(true);
        }
        mLoadTask = new AsyncTask<String, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                File myfavoritelist_dir = new File(EXTERNAL_MY_FAVORITE_WALLPAPER_DIR);
                File[] files = myfavoritelist_dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        String lowerName = name.toLowerCase();
                        if(lowerName.endsWith(".png") || lowerName.endsWith(".jpg")|| lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp")){
                            return true;
                        }
                        return false;
                    }
                });

                for(File child:files){
                    WallpaperItem item = new WallpaperItem();
                    item.setWallpaperPath(child.getAbsolutePath());
                    Bitmap bitmap = BitmapFactory.decodeFile(child.getAbsolutePath());
                    if(bitmap != null){
                        Drawable wallpaperDrawable = new BitmapDrawable(bitmap);
                        item.setWallpaperDrawable(wallpaperDrawable);
                    }
                    mWallpaperItems.add(item);
                    Log.i(TAG,"mWallpaperItems.add-child.getAbsolutePath()="+child.getAbsolutePath());
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                mHandler.removeMessages(MSG_LOAD_DONE);
                mHandler.sendEmptyMessage(MSG_LOAD_DONE);

                if(mWallpaperItems.size() == 0){
                    return;
                }
           }
        };
        mLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public class SDCardListener extends FileObserver {
        public SDCardListener(String path) {
            super(path);
        }
        @Override
        public void onEvent(int event, String path) {
            switch(event) {
                case FileObserver.DELETE:
                case FileObserver.CREATE:
                    loadWallpaperItems();
                    Log.d(TAG, "onEvent-event="+event+", path="+ path);
                    break;
            }
        }
    }
    /**
     * 重力感应监听
     */
    private SensorEventListener sensorEventListener = new SensorEventListener() {

        /**
         * 检测的时间间隔
         */
        static final int UPDATE_INTERVAL = 100;
        /**
         * 上一次检测的时间
         */
        long mLastUpdateTime;
        /**
         * 上一次检测时，加速度在x、y、z方向上的分量，用于和当前加速度比较求差。
         */
        float mLastX, mLastY, mLastZ;

        /**
         * 摇晃检测阈值，决定了对摇晃的敏感程度，越小越敏感。
         */
        public int shakeThreshold = 1500;

        long mLastDetectTime;

        @Override
        public void onSensorChanged(SensorEvent event) {
            long currentTime = System.currentTimeMillis();
            long diffTime = currentTime - mLastUpdateTime;
            if (diffTime < UPDATE_INTERVAL) {
                return;
            }
            mLastUpdateTime = currentTime;
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float deltaX = x - mLastX;
            float deltaY = y - mLastY;
            float deltaZ = z - mLastZ;
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            float delta = (float) (Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime * 10000);
            if (delta > shakeThreshold && currentTime - mLastDetectTime >1000) {
                Log.d(TAG, "shake detect");
                mLastDetectTime = currentTime;
                if(!mHandler.hasMessages(MSG_SENSOR_SHAKE)) {
                    mHandler.sendEmptyMessage(MSG_SENSOR_SHAKE);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private ScheduledExecutorService mScheduledExecutor = null;
    public void executeFixedRate() {
        if(mScheduledExecutor == null) {
            mScheduledExecutor = Executors.newScheduledThreadPool(1);
            mScheduledExecutor.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            if(mWallpaperItems.size() > 0 && isTimerChangeRunning){
                                mHandler.removeMessages(MSG_TRIGGER_CHANGE);
                                mHandler.sendEmptyMessage(MSG_TRIGGER_CHANGE);
                            }
                        }
                    },
                    0,
                    15000,
                    TimeUnit.MILLISECONDS);
        }
    }
}
