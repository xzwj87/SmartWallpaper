package com.samsung.app.smartwallpaper.wallpaper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.samsung.app.smartwallpaper.ASRDialog;
import com.samsung.app.smartwallpaper.FavoriteListActivity;
import com.samsung.app.smartwallpaper.command.Action;
import com.samsung.app.smartwallpaper.model.WallpaperItem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    private boolean isScheduleRunning = false;
    private boolean isLoadDone = false;

    private HandlerThread mWorkerThread = null;
    private Handler mHandler = null;
    private void initWorkerThread() {
        mWorkerThread = new HandlerThread("change_wallpaper_thread");
        mWorkerThread.start();
        mHandler = new Handler(mWorkerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_LOAD_DONE:
                        isLoadDone = true;
                        Log.d(TAG, "load done, mWallpaperItems.size()="+mWallpaperItems.size());
                        break;
                    case MSG_SENSOR_SHAKE:
                        //mVibrator.vibrate(200);
                        if(mWallpaperItems.size() > 0) {
                            mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand-intent="+intent);

        if(intent != null){
            String action = intent.getAction();
            if(!TextUtils.isEmpty(action)) {
                Toast.makeText(this, "onStartCommand-action=" + action, Toast.LENGTH_SHORT).show();
                if (Action.ACTION_ENABLE_SCHEDULE_CHANGE_WALLPAPER.equals(action)) {//启动自动切换壁纸
                    if (mWallpaperItems.size() == 0 && isLoadDone) {
                        Toast.makeText(this, "收藏夹中还没有壁纸，先收藏一些壁纸吧", Toast.LENGTH_SHORT).show();
                        if(ASRDialog.getASRDialogInstance() != null){
                            ASRDialog.getASRDialogInstance().finish();
                        }
                    } else {
                        if (!isScheduleRunning) {
                            initScheduleAndRun();
                        }else{
                            Toast.makeText(mContext, "定时切换壁纸【已开启】", Toast.LENGTH_SHORT).show();
                            if(ASRDialog.getASRDialogInstance() != null){
                                ASRDialog.getASRDialogInstance().finish();
                            }
                        }
                    }
                } else if (Action.ACTION_DISABLE_SCHEDULE_CHANGE_WALLPAPER.equals(action)) {//停止自动切换壁纸
                    stopScheduleJob();
                } else if (Action.ACTION_TRIGGER_CHANGE_WALLPAPER.equals(action)) {
                    if (!isScheduleRunning) {
                        initScheduleAndRun();
                    }
                } else if (Action.ACTION_ENABLE_SHAKE_LISTEN.equals(action)) {
                    enableShakeListen(true);
                } else if (Action.ACTION_DISABLE_SHAKE_LISTEN.equals(action)) {
                    enableShakeListen(false);
                }
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
        releaseSchedule();
        if (mSensorManager != null) {// 取消监听器
            enableShakeListen(false);
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
                isLoadDone = false;
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


    private ScheduledExecutorService mScheduledExecutor = null;
    private Future mFuture = null;
    private void initScheduleAndRun() {
        Log.d(TAG, "initScheduleAndRun");
        if(mScheduledExecutor == null) {
            mScheduledExecutor = Executors.newScheduledThreadPool(1);
            mFuture = mScheduledExecutor.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            if(mWallpaperItems.size() > 0 && isScheduleRunning){
                                mHandler.removeMessages(MSG_TRIGGER_CHANGE);
                                mHandler.sendEmptyMessage(MSG_TRIGGER_CHANGE);
                            }
                        }
                    },
                    0,
                    15000, //每隔15s切换一次
                    TimeUnit.MILLISECONDS);
            startScheduleJob();
        }
    }
    private void startScheduleJob(){
        Log.d(TAG, "startScheduleJob-"+ASRDialog.getASRDialogInstance());
        isScheduleRunning = true;
        if(ASRDialog.getASRDialogInstance() != null){
            ASRDialog.getASRDialogInstance().finish();
        }

        SharedPreferences sp = getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("enableScheduleChangeWallpaper", true);
        editor.apply();

        Toast.makeText(this, "【启动】定时切换壁纸", Toast.LENGTH_SHORT).show();
    }
    private void stopScheduleJob(){
        Log.d(TAG, "stopScheduleJob");
        isScheduleRunning = false;
        if(ASRDialog.getASRDialogInstance() != null){
            ASRDialog.getASRDialogInstance().finish();
        }

        SharedPreferences sp = getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("enableScheduleChangeWallpaper", false);
        editor.apply();

        Toast.makeText(this, "【关闭】定时切换壁纸", Toast.LENGTH_SHORT).show();

    }
    private void releaseSchedule() {
        Log.d(TAG, "releaseSchedule");
        stopScheduleJob();

        if(mFuture != null){
            mFuture.cancel(true);
            mFuture = null;
        }
        if(mScheduledExecutor != null){
            mScheduledExecutor.shutdown();
            mScheduledExecutor = null;
        }
    }

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
    private void enableShakeListen(boolean enable){
        if(mSensorManager == null){
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }
        if(ASRDialog.getASRDialogInstance() != null){
            ASRDialog.getASRDialogInstance().finish();
        }

        if(enable){
            mSensorManager.registerListener(sensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            Toast.makeText(this, "【启动】摇一摇换壁纸", Toast.LENGTH_SHORT).show();
        }else{
            mSensorManager.unregisterListener(sensorEventListener);
            Toast.makeText(this, "【关闭】摇一摇换壁纸", Toast.LENGTH_SHORT).show();
        }

        SharedPreferences sp = getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("enableShakeListen", enable);
        editor.apply();
    }

}
