package com.samsung.app.smartwallpaper.wallpaper;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Created by ASUS on 2018/4/25.
 */
public class ChangeService extends Service {
    //定义当前所显示的壁纸
    int current = 0;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //如果到了最后一张，重新开始
        Bundle extras = intent.getExtras();
        if(extras != null){
            String wallpaperPath = extras.getString("wallpaper_path");
            Bitmap bitmap = BitmapFactory.decodeFile(wallpaperPath);
            SmartWallpaperHelper.getInstance(this).setHomeScreenWallpaper(bitmap);
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}
