package com.samsung.app.smartwallpaper.wallpaper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.samsung.app.smartwallpaper.utils.FastBlur;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by ASUS on 2017/12/21.
 */

public class SmartWallpaperHelper {

    private static final String TAG = "SmartWallpaperHelper";

    private Context mContext;
    private WallpaperManager wManager = null;
    private AlarmManager mAlarmManager = null;
    private PendingIntent pi = null;
    public static Bitmap curWallpaper = null;
    public static Bitmap previousWallpaper = null;

    private static SmartWallpaperHelper mSmartWallpaperManager = null;
    public static SmartWallpaperHelper getInstance(Context context){
        if(mSmartWallpaperManager == null){
            mSmartWallpaperManager = new SmartWallpaperHelper(context);
        }
        return mSmartWallpaperManager;
    }
    private SmartWallpaperHelper(Context context){
        mContext = context;
        wManager = WallpaperManager.getInstance(context);

        Intent intent = new Intent(mContext, ChangeWallpaperService.class);
        pi = PendingIntent.getService(mContext, 0, intent, 0);

    }
    //    private void startLiveWallpaper(){
//        final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
//        Intent chooser = Intent.createChooser(pickWallpaper,"设置动态壁纸");
//        startActivity(chooser);
//    }

    public synchronized void setLiveWallpaper(Class<?> cls) {
        Log.d(TAG, "setLiveWallpaper");
        previousWallpaper = getCurrentWallpaper();
        curWallpaper = null;
        final Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(mContext, cls));
        mContext.startActivity(intent);
    }

    //设置主屏幕壁纸
    public synchronized boolean setHomeScreenWallpaper(Bitmap wallpaper){
        Log.d(TAG, "setHomeScreenWallpaper-Bitmap="+wallpaper);
        if(wallpaper == null){
            return false;
        }
        try {
            previousWallpaper = getCurrentWallpaper();
            wManager.setBitmap(wallpaper);
            curWallpaper = wallpaper;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public synchronized boolean setHomeScreenWallpaper(Drawable wallpaper){
        Log.d(TAG, "setHomeScreenWallpaper-Drawable="+wallpaper);
        if(wallpaper == null){
            return false;
        }
        Bitmap bitmap = ((BitmapDrawable)wallpaper).getBitmap();
        return setHomeScreenWallpaper(bitmap);
    }
    //设置锁屏壁纸
    private void setLockScreenWallPaper(Bitmap wallpaper) {
        Log.d(TAG, "setLockScreenWallPaper-");
        try {
            WallpaperManager mWallManager = WallpaperManager.getInstance(mContext);
            Class class1 = mWallManager.getClass();//获取类名
            Method setWallPaperMethod = class1.getMethod("setBitmapToLockWallpaper", Bitmap.class);
            setWallPaperMethod.invoke(mWallManager, wallpaper);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    public Bitmap getCurrentWallpaper(){
        Log.d(TAG, "getCurrentWallpaper");
        Drawable wallpaperDrawable = wManager.getDrawable();
        Bitmap wallpaper = ((BitmapDrawable) wallpaperDrawable).getBitmap();
        return wallpaper;
    }
    public Drawable getCurrentWallpaperDrawable(){
        Drawable wallpaperDrawable = wManager.getDrawable();
        return wallpaperDrawable;
    }

    //自动更换壁纸
    public void startAutoChangeWallpaper(){
        mAlarmManager = (AlarmManager)mContext.getSystemService(Service.ALARM_SERVICE);
        mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 0, 5000, pi);
    }
    public void stopAutoChangeWallpaper(){
        if(mAlarmManager != null){
            mAlarmManager.cancel(pi);
        }
        mAlarmManager = null;
        pi = null;
    }


    public static final String TEMP_DIR = Environment.getExternalStorageDirectory() + "/smartwallpaper/";
    public static final String TEMP_WALLPAPER = TEMP_DIR + "temp.png";
    public boolean saveWallpaper(Drawable wallpaperDrawable){
        Log.i(TAG, "saveCurWallpaper");
        Bitmap wallpaper = ((BitmapDrawable) wallpaperDrawable).getBitmap();
        if(wallpaper == null) {
            return false;
        }
        try {
            File file = new File(TEMP_DIR);
            if(!file.exists()){
                file.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(TEMP_WALLPAPER);
            wallpaper.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }
    public boolean shareWallpaper(Drawable wallpaperDrawable){
        Log.i(TAG, "shareWallpaper");
        if(saveWallpaper(wallpaperDrawable)) {
            File f = new File(TEMP_WALLPAPER);
            if (f.exists() && f.isFile()) {
                Intent shareImageIntent = new Intent();
                shareImageIntent.setAction(Intent.ACTION_SEND);
                shareImageIntent.setType("image/*");
                shareImageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareImageIntent.putExtra(Intent.EXTRA_TITLE, "壁纸图片");
                Uri uri = Uri.fromFile(f);
                shareImageIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareImageIntent.putExtra("Kdescription", "来自SmartWallaper的壁纸图片分享");
                mContext.startActivity(Intent.createChooser(shareImageIntent, "分享到"));
                return true;
            }
        }
        return false;
    }
    public boolean shareCurrentWallpaper(){
        Log.i(TAG, "shareCurrentWallpaper");
        return shareWallpaper(getCurrentWallpaperDrawable());
    }
    public boolean restoreWallpaper(){
        Log.i(TAG, "shareWallpaper");
        if(previousWallpaper != null){
            setHomeScreenWallpaper(previousWallpaper);
            return true;
        }
        return false;
    }


    public static final String EXTERNAL_MY_FAVORITE_WALLPAPER_DIR = Environment.getExternalStorageDirectory() + "/壁纸收藏夹";
    public static final String WALLPAPER_FILE_EXT = ".jpg";
    public static void saveBitmap(Bitmap bitmap, String dstFileName){
        try {
            FileOutputStream fos = new FileOutputStream(dstFileName);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
        }
    }
    //收藏指定Drawable的壁纸
    public static void favoriteWallpaper(Drawable drawable, String hashcode){
        try {
            String filepath = EXTERNAL_MY_FAVORITE_WALLPAPER_DIR + File.separator + hashcode + WALLPAPER_FILE_EXT;
            BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            FileOutputStream fos = new FileOutputStream(filepath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
        }
    }
    //收藏当前壁纸
    public boolean favoriteCurrentWallpaper(String hashcode){
        Bitmap wallpaper = getCurrentWallpaper();
        if(wallpaper == null) {
            return false;
        }
        File dir = new File(EXTERNAL_MY_FAVORITE_WALLPAPER_DIR);
        if(!dir.exists()){
            dir.mkdirs();
        }
        String filepath = EXTERNAL_MY_FAVORITE_WALLPAPER_DIR + File.separator + hashcode + WALLPAPER_FILE_EXT;
        try {
            FileOutputStream fos = new FileOutputStream(filepath);
            wallpaper.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }
    public static boolean unFavoriteWallpaper(String hashcode){
        String filepath = EXTERNAL_MY_FAVORITE_WALLPAPER_DIR + File.separator + hashcode + WALLPAPER_FILE_EXT;
        File file = new File(filepath);
        if(file.exists() && file.isFile()){
            file.delete();
        }
        return true;
    }
    private static void grantUriPermission (Context context, Uri fileUri, Intent intent) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }
    //打开壁纸收藏夹
    public void openFavoriteList(){
        File file = new File(EXTERNAL_MY_FAVORITE_WALLPAPER_DIR);
        if(!file.exists()){
            file.mkdirs();
        }
        File myFavoriteListFolder = new File(EXTERNAL_MY_FAVORITE_WALLPAPER_DIR);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri fileUri = FileProvider.getUriForFile(mContext, mContext.getPackageName()+".provider", myFavoriteListFolder);//android 7.0以上
            intent.setDataAndType(fileUri, "*/*");
            grantUriPermission(mContext, fileUri, intent);
        }else {
            intent.setDataAndType(Uri.fromFile(myFavoriteListFolder), "*/*");
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        mContext.startActivity(intent);
    }

    //模糊化壁纸
    private void blurWallpaper() {
        Bitmap wallpaper = getCurrentWallpaper();
        if(wallpaper == null){
            return;
        }

        float scaleFactor = 8;
        float radius = 2;

        int width = wManager.getDesiredMinimumWidth();
        int height = wManager.getDesiredMinimumHeight();

        Bitmap new_wallpaper = Bitmap.createBitmap(
                (int) (width / scaleFactor),
                (int) (height / scaleFactor),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(new_wallpaper);
        canvas.scale(1 / scaleFactor, 1 / scaleFactor);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(wallpaper, 0, 0, paint);

        new_wallpaper = FastBlur.doBlur(new_wallpaper, (int) radius, true);

        setHomeScreenWallpaper(new_wallpaper);
    }

    //加跑马灯效果
    private void marqueeWallpaper(){
        Bitmap wallpaper = getCurrentWallpaper();
        if(wallpaper == null){
            return;
        }

        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        int width = wManager.getDesiredMinimumWidth();
        int height = wManager.getDesiredMinimumHeight();

        //线性渐变效果
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        Bitmap new_wallpaper = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(new_wallpaper);
        canvas.drawBitmap(wallpaper, 0, 0, paint);
        canvas.drawRoundRect(0,0,screenWidth,screenHeight, 5,5, paint);


        LinearGradient lg = new LinearGradient(0,0,100,100, Color.RED,Color.BLUE, Shader.TileMode.MIRROR);
        paint.setShader(lg);





        //高斯模糊
//        FastBlur.doBlur()


        setHomeScreenWallpaper(new_wallpaper);
    }

}
