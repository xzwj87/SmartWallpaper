package com.samsung.app.smartwallpaper.wallpaper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.samsung.app.smartwallpaper.AppContext;
import com.samsung.app.smartwallpaper.command.CommandExecutor;

import java.io.File;
import java.io.FileOutputStream;

public class CameraLiveWallpaper extends WallpaperService {

    private static final String TAG = "CameraLiveWallpaper";

    @Override
    public Engine onCreateEngine() {
        return new CameraLiveWallpaperEngine();
    }

    class CameraLiveWallpaperEngine extends Engine implements Camera.PreviewCallback {
        private Camera mCamera;
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            Log.i(TAG,"oncreate");
            startPreview();
            setTouchEventsEnabled(true);
        }

        private long latestClickTime = 0;
        private long timeDiff = 0;
        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);

            long clickTime = SystemClock.uptimeMillis();
            timeDiff = clickTime - latestClickTime;
            if(timeDiff > 500){
                latestClickTime = clickTime;
                return;
            }
            if(mCamera != null && !isRunning){
                takePictureAsWallpaper();
            }
        }
        //拍照
        private boolean isRunning = false;
        public synchronized void takePictureAsWallpaper(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    isRunning = true;
                    mCamera.takePicture(null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            Bitmap bitmap= BitmapFactory.decodeByteArray(data,0,data.length);
                            Drawable drawable = new BitmapDrawable(bitmap);
                            CommandExecutor.getInstance(AppContext.appContext).executeApplyWallpaperTask(drawable);
                        }
                    });
                    isRunning = false;
                }
            }).start();
        }
        @Override
        public void onDestroy() {
            super.onDestroy();
            stopPreview();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                startPreview();
            } else {
                Log.i(TAG,"wallpager invisible");
                stopPreview();
            }
        }

        public void startPreview() {

            if (mCamera == null) {
                Log.i(TAG, "wallpager startPreview " + System.currentTimeMillis());
                try {
                    mCamera = Camera.open(0);
                    if (mCamera != null) {
                        Camera.Parameters parameters=mCamera.getParameters();
                        parameters.setPictureFormat(PixelFormat.JPEG);
                        parameters.set("jpeg-quality",85);
                        mCamera.setParameters(parameters);

                        mCamera.enableShutterSound(true);
                        mCamera.setDisplayOrientation(90);
                        mCamera.setPreviewDisplay(getSurfaceHolder());
                        mCamera.startPreview();
                    }
                } catch (Exception e) {
                    Log.i(TAG,"wallpager "+e.getMessage());
                }
            }

        }

        public void stopPreview() {
            if (mCamera != null) {
                try {
                    mCamera.stopPreview();
                    mCamera.setPreviewCallback(null);
                } catch (Exception e) {
                    Log.i(TAG, "Exception " + System.currentTimeMillis());
                } finally {
                    mCamera.release();
                    mCamera = null;
                }
                Log.i(TAG, "wallpager stopPreview " + System.currentTimeMillis());
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            mCamera.addCallbackBuffer(data);
        }
    }
}
