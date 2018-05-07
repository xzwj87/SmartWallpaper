package com.samsung.app.smartwallpaper.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.provider.Browser;

import com.samsung.app.smartwallpaper.R;

import java.util.Arrays;

/**
 * Created by samsung on 2018/3/13.
 */

public class ShortcutHelper {

    private final static String TAG = "ShortcutHelper";
    public static final String INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    public static final String ACTION_REMOVE_SHORTCUT = "com.android.launcher.action.UNINSTALL_SHORTCUT";

    static Bitmap createIcon(Context context, Bitmap favicon) {
        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final int iconDimension = am.getLauncherLargeIconSize();
        final int iconDensity = am.getLauncherLargeIconDensity();
        return createIcon(context, favicon, iconDimension, iconDensity);
    }

    private static Bitmap createIcon(Context context, Bitmap favicon, int iconDimension, int iconDensity) {
        Bitmap bm = Bitmap.createBitmap(iconDimension, iconDimension, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Rect iconBounds = new Rect(0, 0, bm.getWidth(), bm.getHeight());

        if(favicon == null){
            Drawable drawable = context.getResources().getDrawableForDensity(R.mipmap.ic_launcher, iconDensity);
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bd = (BitmapDrawable) drawable;
                favicon = bd.getBitmap();
            }
        }

        drawIconToCanvas(favicon, canvas, iconBounds);
        canvas.setBitmap(null);
        return bm;
    }

    private static void drawIconToCanvas(Bitmap favicon, Canvas canvas, Rect iconBounds) {
        Rect src = new Rect(0, 0, favicon.getWidth(), favicon.getHeight());
        // Paint used for scaling the bitmap and drawing the rounded rect.
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(favicon, src, iconBounds, paint);

        // Construct a path from a round rect. This will allow drawing with
        // an inverse fill so we can punch a hole using the round rect.
        Path path = new Path();
        path.setFillType(Path.FillType.INVERSE_WINDING);
        RectF rect = new RectF(iconBounds);
        rect.inset(1, 1);
        path.addRoundRect(rect, 8f, 8f, Path.Direction.CW);

        // Reuse the paint and clear the outside of the rectangle.
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPath(path, paint);
     }

    public static Intent createShortcutIntent(String url) {
        Intent shortcutIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        long urlHash = url.hashCode();
        long uniqueId = (urlHash << 32) | shortcutIntent.hashCode();
        shortcutIntent.putExtra(Browser.EXTRA_APPLICATION_ID, Long.toString(uniqueId));
        return shortcutIntent;
    }

    public static Intent createAddToHomeIntent(Context context, String url, String title, Bitmap favicon) {
        Intent i = new Intent(INSTALL_SHORTCUT);
        Intent shortcutIntent = createShortcutIntent(url);
        i.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        i.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        i.putExtra(Intent.EXTRA_SHORTCUT_ICON, createIcon(context, favicon));
        // Do not allow duplicate items
        i.putExtra("duplicate", false);
        return i;
    }

    //添加快捷方式到桌面应用上下文菜单
    public static boolean addDynamicShortCut(Context context, String url, String title, Bitmap favicon){
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        if(shortcutManager.getDynamicShortcuts().size() < shortcutManager.getMaxShortcutCountPerActivity()){
            long urlHash = url.hashCode();
            long uniqueId = (urlHash << 32) | title.hashCode();

            ShortcutInfo shortcut = new ShortcutInfo.Builder(context, Long.toString(uniqueId))
                    .setShortLabel(title)
                    .setLongLabel(url)
                    .setIcon(Icon.createWithBitmap(createIcon(context, favicon)))
                    .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    .build();
//            shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));
            shortcutManager.addDynamicShortcuts(Arrays.asList(shortcut));
            return true;
        }
        return false;
    }
    public static boolean addDynamicShortCut(Context context, String title, Bitmap favicon){
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        if(shortcutManager.getDynamicShortcuts().size() < shortcutManager.getMaxShortcutCountPerActivity()){
            long uniqueId = title.hashCode();

            ShortcutInfo shortcut = new ShortcutInfo.Builder(context, Long.toString(uniqueId))
                    .setShortLabel(title)
                    .setIcon(Icon.createWithBitmap(createIcon(context, favicon)))
                    .setIntent(new Intent(Intent.ACTION_VIEW))
                    .build();
//            shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));
            shortcutManager.addDynamicShortcuts(Arrays.asList(shortcut));
            return true;
        }
        return false;
    }

    //添加快捷方式到桌面
    public static boolean addPinnedShortCut(Context context, String url, String title, Bitmap favicon){
        context.sendBroadcast(createAddToHomeIntent(context, url, title, favicon));
        return true;
    }

    public static boolean addShortCut(Context context, String url, String title, Bitmap favicon){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            return addDynamicShortCut(context, url, title, favicon);
        }else{
            return addPinnedShortCut(context, url, title, favicon);
        }
    }

    public static boolean addShortCut(Context context, String title, Bitmap favicon){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            return addDynamicShortCut(context, title, favicon);
        }
        return false;
    }

    public static void clearShortCut(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            shortcutManager.removeAllDynamicShortcuts();
        }
    }

}
