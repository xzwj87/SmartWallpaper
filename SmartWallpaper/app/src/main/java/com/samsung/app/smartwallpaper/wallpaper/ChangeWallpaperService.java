package com.samsung.app.smartwallpaper.wallpaper;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaActionSound;
import android.os.AsyncTask;
import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.samsung.app.smartwallpaper.ASRDialog;
import com.samsung.app.smartwallpaper.AppContext;
import com.samsung.app.smartwallpaper.command.Action;
import com.samsung.app.smartwallpaper.command.Command;
import com.samsung.app.smartwallpaper.model.WallpaperItem;
import com.samsung.app.smartwallpaper.network.ApiClient;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.samsung.app.smartwallpaper.WallpaperListActivity.WALLPAPER_PRELOAD_PATH;
import static com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper.EXTERNAL_MY_FAVORITE_WALLPAPER_DIR;

/**
 * Created by my2013.wang on 2018/4/25.
 */
public class ChangeWallpaperService extends JobService {

    private static final String TAG = "ChangeWallpaperService";

    private static Context mContext;

    private static SDCardListener mSDCardListener;

    private static ArrayList<WallpaperItem> mWallpaperItems;
    private static int curPos = 0;
    private static int randomIndex = 0;
    private static ArrayList<Integer> randomPosList;

    private static SensorManager mSensorManager;
    private static Vibrator mVibrator;
    private static MediaActionSound mCameraSound;

    private static final int MSG_LOAD_DONE = 0;
    private static final int MSG_SENSOR_SHAKE = 1;
    private static final int MSG_TRIGGER_CHANGE = 2;

    private static boolean enableScheduleRunning = false;
    private static boolean enableShake = false;
    private static boolean isLoadDone = false;

    private static HandlerThread mWorkerThread = null;
    private static Handler mHandler = null;
    private static void initWorkerThread() {
        mWorkerThread = new HandlerThread("change_wallpaper_thread");
        mWorkerThread.start();
        mHandler = new Handler(mWorkerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Command cmd;
                ArrayList<String> hashCodeList;
                switch (msg.what) {
                    case MSG_LOAD_DONE:
                        isLoadDone = true;
                        Log.d(TAG, "load done, mWallpaperItems.size()="+mWallpaperItems.size());
                        break;
                    case MSG_SENSOR_SHAKE:
                        //mVibrator.vibrate(200);
                        mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                        mHandler.removeMessages(MSG_SENSOR_SHAKE);
                        mHandler.removeMessages(MSG_TRIGGER_CHANGE);

                        hashCodeList = ApiClient.searchWallpaperWithHotKeywords(new ArrayList<String>(AppContext.userTagList));
                        if(hashCodeList != null && hashCodeList.size() > 0){
                            Bitmap wallpaper = ApiClient.getWallpaperByHashCode(hashCodeList.get(0));
                            if(wallpaper != null){
                                SmartWallpaperHelper.setCurHashCode(hashCodeList.get(0));
                                SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(wallpaper);
                                return;
                            }
                        }
//                        cmd = ApiClient.requestCommand("换张壁纸");
//                        if(cmd != null && !TextUtils.isEmpty(cmd.getAction())) {
//                            ArrayList<String> hashCodeList = cmd.getHashCodeList();
//                            if(hashCodeList !=null && hashCodeList.size() >0) {
//                                Bitmap wallpaper = ApiClient.getWallpaperByHashCode(hashCodeList.get(0));
//                                if(wallpaper != null){
//                                    SmartWallpaperHelper.setCurHashCode(hashCodeList.get(0));
//                                    SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(wallpaper);
//                                    return;
//                                }
//                            }
//                        }

                        if(mWallpaperItems.size() > 0) {
                            Log.d(TAG, "randomIndex="+randomIndex);
                            WallpaperItem wallpaperItem = mWallpaperItems.get(randomPosList.get(randomIndex % mWallpaperItems.size()));
                            if(TextUtils.isEmpty(wallpaperItem.getWallpaperLocalPath())){
                                SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(wallpaperItem.getWallpaperAssertPath(),true);
                            }else {
                                SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(wallpaperItem.getWallpaperLocalPath(),false);
                            }
                            randomIndex = (randomIndex + 1) % mWallpaperItems.size();
                        }
                        break;
                    case MSG_TRIGGER_CHANGE:
                        mHandler.removeMessages(MSG_TRIGGER_CHANGE);

                        hashCodeList = ApiClient.searchWallpaperWithHotKeywords(new ArrayList<String>(AppContext.userTagList));
                        if(hashCodeList != null && hashCodeList.size() > 0){
                            Bitmap wallpaper = ApiClient.getWallpaperByHashCode(hashCodeList.get(0));
                            if(wallpaper != null){
                                SmartWallpaperHelper.setCurHashCode(hashCodeList.get(0));
                                SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(wallpaper);
                                return;
                            }
                        }

//                        cmd = ApiClient.requestCommand("换张壁纸");
//                        if(cmd != null && !TextUtils.isEmpty(cmd.getAction())) {
//                            ArrayList<String> hashCodeList = cmd.getHashCodeList();
//                            if(hashCodeList !=null && hashCodeList.size() >0) {
//                                Bitmap wallpaper = ApiClient.getWallpaperByHashCode(hashCodeList.get(0));
//                                if(wallpaper != null){
//                                    SmartWallpaperHelper.setCurHashCode(hashCodeList.get(0));
//                                    SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(wallpaper);
//                                    return;
//                                }
//                            }
//                        }
                        if(mWallpaperItems.size() > 0) {
                            Log.d(TAG, "curPos="+curPos);
                            WallpaperItem wallpaperItem = mWallpaperItems.get(curPos % mWallpaperItems.size());
                            if(TextUtils.isEmpty(wallpaperItem.getWallpaperLocalPath())){
                                SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(wallpaperItem.getWallpaperAssertPath(),true);
                            }else {
                                SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(wallpaperItem.getWallpaperLocalPath(),false);
                            }
                            curPos = (curPos + 1) % mWallpaperItems.size();
                        }

                        if(useJobScheduler() && mJobService != null) {
                            mJobService.jobFinished((JobParameters) msg.obj, false);
                        }
                        break;
                }
            }
        };
    }

    private static ChangeWallpaperService mJobService;
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        // TODO Auto-generated method stub
        super.onCreate();
        mJobService = this;
        init(this);
    }

    private static void init(Context context){

        mContext = context;

        mWallpaperItems = new ArrayList<>();
        randomPosList = new ArrayList<>();

        initWorkerThread();
        loadWallpaperItems();

        mSDCardListener = new SDCardListener(EXTERNAL_MY_FAVORITE_WALLPAPER_DIR);
        mSDCardListener.startWatching();

        mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        mVibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        mCameraSound = new MediaActionSound();
        mCameraSound.load(MediaActionSound.SHUTTER_CLICK);

        if(!useJobScheduler()){
            initScheduleAndRun(true);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand-intent="+intent);
        startForeground(0, new Notification());
        if(intent != null){
            String action = intent.getAction();
            if(!TextUtils.isEmpty(action)) {
                //Toast.makeText(this, "onStartCommand-action=" + action, Toast.LENGTH_SHORT).show();
                boolean isFirstTime = intent.getBooleanExtra("first_time", false);
                if (Action.ACTION_ENABLE_SCHEDULE_CHANGE_WALLPAPER.equals(action)) {//启动自动切换壁纸
                    if (mWallpaperItems.size() == 0 && isLoadDone) {
                        Toast.makeText(this, "收藏夹中还没有壁纸，先收藏一些壁纸吧", Toast.LENGTH_SHORT).show();
                        if(ASRDialog.getASRDialogInstance() != null && !isFirstTime){
                            ASRDialog.getASRDialogInstance().finish();
                        }
                    } else {
                        startScheduleJob(isFirstTime);
                    }
                } else if (Action.ACTION_DISABLE_SCHEDULE_CHANGE_WALLPAPER.equals(action)) {//停止自动切换壁纸
                    stopScheduleJob(isFirstTime);
                } else if (Action.ACTION_TRIGGER_CHANGE_WALLPAPER.equals(action)) {
                    if (!enableScheduleRunning) {
                        initScheduleAndRun(isFirstTime);
                    }
                } else if (Action.ACTION_ENABLE_SHAKE_LISTEN.equals(action)) {
                    enableShakeListen(true, isFirstTime);
                } else if (Action.ACTION_DISABLE_SHAKE_LISTEN.equals(action)) {
                    enableShakeListen(false, isFirstTime);
                }
            }else{
                loadWallpaperItems();
            }
        }
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        if (useJobScheduler()) {
            super.onDestroy();
            mJobService = null;
            return;
        }
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
            enableShakeListen(false, false);
            mSensorManager = null;
        }

        super.onDestroy();
    }

    public static final int JOB_ID_TIMER = 1;

    public static boolean useJobScheduler(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static void createJobAndSchedule(int id){
        Log.i(TAG, "createJobAndSchedule-id="+id);
        JobScheduler jobScheduler = (JobScheduler) AppContext.appContext.getSystemService(JOB_SCHEDULER_SERVICE);
        int JOB_CNT = 15;
        for(int i=0;i<JOB_CNT;i++) {
            jobScheduler.cancel(JOB_ID_TIMER + i);
        }

        for(int i=0;i<JOB_CNT;i++) {
            JobInfo jobInfo  = new JobInfo.Builder(JOB_ID_TIMER+i, new ComponentName(AppContext.appContext, ChangeWallpaperService.class))
                        .setPeriodic((15+i)*60*1000, 60 * 1000)
//                        .setPeriodic(15000, 15000)
//                        .setOverrideDeadline(15000)
//                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                        .setPersisted(true) //重启后是否还要继续执行
                        .build();
            jobScheduler.schedule(jobInfo);
            Log.i(TAG, "jobScheduler.schedule-i="+i);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob-params.getJobId()="+params.getJobId());
        startForeground(0, new Notification());
        SharedPreferences sp;
        SharedPreferences.Editor editor;
        if(mWallpaperItems == null || mWorkerThread == null){
            init(AppContext.appContext);
        }
        if (enableScheduleRunning && mWallpaperItems.size() > 0 && !shouldIgnore()) {
            mHandler.removeMessages(MSG_TRIGGER_CHANGE);
//                    mHandler.sendEmptyMessage(MSG_TRIGGER_CHANGE);
            mHandler.sendMessage(Message.obtain( mHandler, MSG_TRIGGER_CHANGE, params));
        }else{
            mHandler.removeMessages(MSG_TRIGGER_CHANGE);
            jobFinished(params, false);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStopJob");
        return false;
    }

    static AsyncTask<String, Void, String> mLoadTask;
    public static void loadWallpaperItems() {
        Log.i(TAG, "loadWallpaperItems");
        if(mLoadTask != null){
            mLoadTask.cancel(true);
        }
        mLoadTask = new AsyncTask<String, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                isLoadDone = false;
                mWallpaperItems.clear();
                randomPosList.clear();
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
                if(files!=null && files.length > 0) {
                    for (File child : files) {
                        WallpaperItem item = new WallpaperItem();
                        item.setWallpaperLocalPath(child.getAbsolutePath());
                        mWallpaperItems.add(item);
                        //Log.i(TAG, "mWallpaperItems.add-child.getAbsolutePath()=" + child.getAbsolutePath());
                    }
                }

                String[] fileNames = null;
                try {
                    fileNames = mContext.getResources().getAssets().list(WALLPAPER_PRELOAD_PATH);
                    if (fileNames.length > 0) {
                        ArrayList<WallpaperItem> wallpaperItemList = new ArrayList<>();
                        for (String fileName : fileNames) {
                            //Log.i(TAG, "loadPreloadWallpaper-fileName="+fileName);
                            WallpaperItem item = new WallpaperItem();
                            item.setWallpaperAssertPath(WALLPAPER_PRELOAD_PATH + File.separator + fileName);
                            item.setHashCode(fileName.substring(0, fileName.indexOf(".")));
                            item.setVoteupCount(0);
                            wallpaperItemList.add(item);
                        }
                        Collections.shuffle(wallpaperItemList);
                        if(wallpaperItemList.size() > 0){
                            mWallpaperItems.addAll(wallpaperItemList);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
                for(int i=0; i< mWallpaperItems.size(); i++){
                    randomPosList.add(i);
                }
                Collections.shuffle(randomPosList);
           }
        };
        mLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static class SDCardListener extends FileObserver {
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


    private static ScheduledExecutorService mScheduledExecutor = null;
    private static Future mFuture = null;
    private static void initScheduleAndRun(boolean isFirstTime) {
        Log.d(TAG, "initScheduleAndRun");
        if(mScheduledExecutor == null) {
            mScheduledExecutor = Executors.newScheduledThreadPool(1);
            mFuture = mScheduledExecutor.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            if(mWallpaperItems.size() > 0 && enableScheduleRunning && !shouldIgnore()){
                                mHandler.removeMessages(MSG_TRIGGER_CHANGE);
                                mHandler.sendEmptyMessage(MSG_TRIGGER_CHANGE);
                            }
                        }
                    },
                    0,
                    15000, //每隔15s切换一次
                    TimeUnit.MILLISECONDS);
        }
    }
    public static void startScheduleJob(boolean isFirstTime){
        Log.d(TAG, "startScheduleJob-isFirstTime="+isFirstTime);
        if(mWorkerThread == null){
            init(AppContext.appContext);
        }
        enableScheduleRunning = true;
        if(!isFirstTime) {
            if (ASRDialog.getASRDialogInstance() != null) {
                ASRDialog.getASRDialogInstance().finish();
            }
        }

        SharedPreferences sp = AppContext.appContext.getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("enableScheduleChangeWallpaper", true);
        editor.apply();

        if(useJobScheduler()) {
            createJobAndSchedule(JOB_ID_TIMER);
        }
        if(!isFirstTime) {
            Toast.makeText(AppContext.appContext, "【启动】定时切换壁纸", Toast.LENGTH_SHORT).show();
        }
    }
    public static void stopScheduleJob(boolean isFirstTime){
        Log.d(TAG, "stopScheduleJob-isFirstTime="+isFirstTime);
        enableScheduleRunning = false;
        if(mWorkerThread == null){
            init(AppContext.appContext);
        }
        if(!isFirstTime) {
            if (ASRDialog.getASRDialogInstance() != null) {
                ASRDialog.getASRDialogInstance().finish();
            }
        }

        SharedPreferences sp = AppContext.appContext.getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("enableScheduleChangeWallpaper", false);
        editor.apply();

        if(!isFirstTime) {
            Toast.makeText(AppContext.appContext, "【关闭】定时切换壁纸", Toast.LENGTH_SHORT).show();
        }
    }
    private void releaseSchedule() {
        Log.d(TAG, "releaseSchedule");
        stopScheduleJob(false);

        if(mFuture != null){
            mFuture.cancel(true);
            mFuture = null;
        }
        if(mScheduledExecutor != null){
            mScheduledExecutor.shutdown();
            mScheduledExecutor = null;
        }
    }

    private static SensorEventListener sensorEventListener = new SensorEventListener() {

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
        public int shakeThreshold = 1200;

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
            if (enableShake && delta > shakeThreshold && currentTime - mLastDetectTime >1000) {
                Log.d(TAG, "shake detect");
                mLastDetectTime = currentTime;
                if(!mHandler.hasMessages(MSG_SENSOR_SHAKE) && !shouldIgnore()) {
                    mHandler.sendEmptyMessage(MSG_SENSOR_SHAKE);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    public static void enableShakeListen(boolean enable,boolean isFirstTime){
        Log.d(TAG, "enableShakeListen-enable="+enable+", isFirstTime="+isFirstTime);
        if(mWorkerThread == null){
            init(AppContext.appContext);
        }
        if(mSensorManager == null){
            mSensorManager = (SensorManager) AppContext.appContext.getSystemService(SENSOR_SERVICE);
        }

        if(ASRDialog.getASRDialogInstance() != null && !isFirstTime){
            ASRDialog.getASRDialogInstance().finish();
        }

        if(enable){
            mSensorManager.registerListener(sensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            if(!isFirstTime) {
                Toast.makeText(AppContext.appContext, "【启动】摇一摇换壁纸", Toast.LENGTH_SHORT).show();
            }
        }else{
            mSensorManager.unregisterListener(sensorEventListener);
            if(!isFirstTime) {
                Toast.makeText(AppContext.appContext, "【关闭】摇一摇换壁纸", Toast.LENGTH_SHORT).show();
            }
        }
        SharedPreferences sp = AppContext.appContext.getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("enableShakeListen", enable);
        editor.apply();

        enableShake = enable;
    }

    private static KeyguardManager mKeyguardManager;
    private static PowerManager mPowerManager;
    private static boolean shouldIgnore() {
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) AppContext.appContext.getSystemService(Context.KEYGUARD_SERVICE);
        }
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) AppContext.appContext.getSystemService(Context.POWER_SERVICE);
        }
        boolean ignore = !mPowerManager.isScreenOn();
        //ignore |= mKeyguardManager.inKeyguardRestrictedInputMode();
        return ignore;
    }
}
