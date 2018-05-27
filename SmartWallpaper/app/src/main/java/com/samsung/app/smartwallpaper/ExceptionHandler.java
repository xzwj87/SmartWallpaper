package com.samsung.app.smartwallpaper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.samsung.app.smartwallpaper.utils.FileUtil;
import com.samsung.app.smartwallpaper.utils.PermisionUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * uncaught exception handler
 */

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "ExceptionHandler";
    private static final String APP_NAME = "SmartWallpaper";
    private static final String CRASH_LOG = "CrashLog";

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.e(TAG,"uncaughtException(): thread=" + t.getName());
        if (PermisionUtil.hasStoragePermissions(AppContext.appContext)) {
            String dir =  APP_NAME + File.separator + CRASH_LOG;
            File dirFile = FileUtil.creatSDDir(dir);

            String fileName = System.currentTimeMillis() + ".log";
            File log = new File(dirFile, fileName);
            if (!log.exists()) {
                try {
                    log.createNewFile();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    return;
                }
            }

            try {
                PrintWriter pw = new PrintWriter(new FileOutputStream(log));

                pw.println("BuildConfig:");
                pw.println(Build.FINGERPRINT);

                pw.println();
                e.printStackTrace(pw);

                pw.flush();
                pw.close();

            } catch (FileNotFoundException e1) {
                //e1.printStackTrace();
            }
        }
    }
}
