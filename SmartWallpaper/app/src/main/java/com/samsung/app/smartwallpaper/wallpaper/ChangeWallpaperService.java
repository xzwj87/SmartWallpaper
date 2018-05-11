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

    private SDCardListener listener;

    private ArrayList<WallpaperItem> mWallpaperItems;
    private static int curPos = 0;

    private SensorManager mSensorManager;
    private Vibrator mVibrator;
    private MediaActionSound mCameraSound;
    private static final int SENSOR_SHAKE = 10;
    private boolean isShakeListening= false;
    private boolean isTimerChanging = false;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        // TODO Auto-generated method stub
        super.onCreate();
        mContext = this;
        mWallpaperItems = new ArrayList<>();
        loadWallpaperItems();
        alarmManager=(AlarmManager)getSystemService(Service.ALARM_SERVICE);
        intent = new Intent(mContext, ChangeWallpaperService.class);
        intent.setAction(Action.ACTION_TIMER_CHANGE_WALLPAPER);
        // 创建PendingIntent对象
        pi= PendingIntent.getService(mContext, 0, intent, 0);

        listener = new SDCardListener(EXTERNAL_MY_FAVORITE_WALLPAPER_DIR);
        listener.startWatching();

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
                if(!isTimerChanging) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000, pi);
                }
                isTimerChanging = true;
            }else if(Action.ACTION_STOP_TIMER_CHANGE_WALLPAPER.equals(action)){//停止自动切换壁纸
                alarmManager.cancel(pi);
                isTimerChanging = false;
            }else if(Action.ACTION_TIMER_CHANGE_WALLPAPER.equals(action)){
                int total = mWallpaperItems.size();
                if(total > 0){
                    Log.i(TAG, "onStartCommand-total="+total +", curPos="+curPos);
                    WallpaperItem wallpaperItem = mWallpaperItems.get(curPos%total);
                    SmartWallpaperHelper.getInstance(this).setHomeScreenWallpaper(wallpaperItem.getWallpaperDrawable());
                    curPos = (curPos+1)%total;
                }
                isTimerChanging = true;
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
        listener.stopWatching();

        if(alarmManager != null) {
            alarmManager.cancel(pi);
            isTimerChanging = false;
        }
        if (mSensorManager != null) {// 取消监听器
            mSensorManager.unregisterListener(sensorEventListener);
            isShakeListening = false;
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

//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            // 传感器信息改变时执行该方法
//            float[] values = event.values;
//            float x = values[0]; // x轴方向的重力加速度，向右为正
//            float y = values[1]; // y轴方向的重力加速度，向前为正
//            float z = values[2]; // z轴方向的重力加速度，向上为正
//            Log.i(TAG, "x轴方向的重力加速度" + x +  "；y轴方向的重力加速度" + y +  "；z轴方向的重力加速度" + z);
//            // 一般在这三个方向的重力加速度达到40就达到了摇晃手机的状态。
//            int medumValue = 19;// 如果不敏感请自行调低该数值,低于10的话就不行了,因为z轴上的加速度本身就已经达到10了
//            if (Math.abs(x) > medumValue || Math.abs(y) > medumValue || Math.abs(z) > medumValue) {
//                vibrator.vibrate(200);
//                Message msg = new Message();
//                msg.what = SENSOR_SHAKE;
//                handler.sendMessage(msg);
//            }
//        }


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
        public int shakeThreshold = 2000;

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
            // 当加速度的差值大于指定的阈值，认为这是一个摇晃
            if (delta > shakeThreshold) {
                Message msg = new Message();
                msg.what = SENSOR_SHAKE;
                handler.sendMessage(msg);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    /**
     * 动作执行
     */
    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SENSOR_SHAKE:
                    if(!isShakeListening) {
                        Intent intent = new Intent(mContext, ChangeWallpaperService.class);
                        intent.setAction(Action.ACTION_START_SHAKE_LISTEN);
                        startService(intent);
                    }else {
                        //mVibrator.vibrate(200);
                        mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                        startService(intent);
                    }
                    break;
            }
        }

    };
}
