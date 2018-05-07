package com.samsung.app.smartwallpaper.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.samsung.app.smartwallpaper.R;
import com.samsung.app.smartwallpaper.config.UrlConstant;
import com.samsung.app.smartwallpaper.network.ApiClient;

import java.io.File;
import java.io.InputStream;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

/**
 * Created by samsung on 2018/3/16.
 * Author: my2013.wang@samsung.com
 */

public class WallpaperItem {
    private static final String TAG = "WallpaperItem";
    private String mHashCode;
    private int mVoteUpCount=0;
    private Drawable mWallpaperDrawable;
    private ImageView mTargetView;
    private int placeholder = R.drawable.img_placeholder;
    private boolean mHasVoteUp = false;
    private boolean mFavoriteOn = false;

    public WallpaperItem(String hashcode){
        mHashCode = hashcode;
    }
    public void setHashCode(String hashCode){
        mHashCode = hashCode;
    }
    public String getHashCode(){
        return  mHashCode;
    }
    public String getUrl(){
        if(TextUtils.isEmpty(mHashCode)){
            return null;
        }
        return UrlConstant.DOWNLOAD_WALLPAPER_URL+mHashCode;
    }
    public int getVoteupCount(){
        return mVoteUpCount;
    }
    public void setVoteupCount(int cnt){
        mVoteUpCount = cnt;
    }
    public void voteUp(){
        mVoteUpCount++;
    }

    public void setTargetView(ImageView imageView){
        mTargetView = imageView;
        if(mTargetView != null){
            mTargetView.setScaleType(ImageView.ScaleType.CENTER);
            mTargetView.setImageResource(placeholder);
        }
    }
    public void setVoteUpState(boolean voteUp){
        mHasVoteUp = voteUp;
    }
    public boolean hasVoteUp(){
        return mHasVoteUp;
    }
    public void setFavoriteOn(boolean favoriteOn){
        mFavoriteOn = favoriteOn;
    }
    public boolean isFavoriteOn(){
        return mFavoriteOn;
    }

    public Drawable getWallpaperDrawable(){
        return mWallpaperDrawable;
    }

    private static String WALLPAPER_FILES_DIR = Environment.getExternalStorageDirectory() + File.separator + "wallpaper_files";
    private AsyncTask<String,Void,Boolean> mLoadTask = null;
    public void loadWallpaper(String hashcode){
        if(mLoadTask != null){
            mLoadTask.cancel(true);
        }
        mLoadTask = new AsyncTask<String, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mWallpaperDrawable = null;
            }

            @Override
            protected Boolean doInBackground(String... params) {
                String hashcode = params[0];
                Bitmap bitmap = ApiClient.getBitmapByHashCode(hashcode);
                if(bitmap != null){
                    mWallpaperDrawable = new BitmapDrawable(bitmap);
                    return true;
                }else{
                    String relative_path = ApiClient.getWallpaperFilePathByHashCode(hashcode);
                    Log.i(TAG, "relative_path=" + relative_path);
                    if(!TextUtils.isEmpty(relative_path)) {
                        String full_path = WALLPAPER_FILES_DIR + File.separator + relative_path;
                        bitmap = BitmapFactory.decodeFile(full_path);
                        if(bitmap != null){
                            mWallpaperDrawable = new BitmapDrawable(bitmap);
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                if(mTargetView != null && success){
                    mTargetView.setScaleType(ImageView.ScaleType.FIT_XY);
                    mTargetView.setImageDrawable(mWallpaperDrawable);
                    mTargetView.invalidate();
                }
            }
        };
        mLoadTask.executeOnExecutor(THREAD_POOL_EXECUTOR, hashcode);
    }
}
