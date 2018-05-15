package com.samsung.app.smartwallpaper.command;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.samsung.app.smartwallpaper.ASRDialog;
import com.samsung.app.smartwallpaper.AppContext;
import com.samsung.app.smartwallpaper.FavoriteListActivity;
import com.samsung.app.smartwallpaper.UploadListActivity;
import com.samsung.app.smartwallpaper.WallpaperListActivity;
import com.samsung.app.smartwallpaper.model.WallpaperItem;
import com.samsung.app.smartwallpaper.network.ApiClient;
import com.samsung.app.smartwallpaper.utils.FileUtil;
import com.samsung.app.smartwallpaper.wallpaper.CameraLiveWallpaper;
import com.samsung.app.smartwallpaper.wallpaper.ChangeWallpaperService;
import com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper;
import com.samsung.app.smartwallpaper.wallpaper.VideoLiveWallpaper;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper.EXTERNAL_MY_FAVORITE_WALLPAPER_DIR;
import static com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper.EXTERNAL_UPLOAD_WALLPAPER_DIR;
import static com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper.WALLPAPER_FILE_EXT;
import static com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper.saveBitmap;

/**
 * Created by ASUS on 2018/4/23.
 * 换壁纸 服务
 */

public class CommandExecutor {
    private static final String TAG = "CommandExecutor";
    private Context mContext;
    public static ArrayList<WallpaperItem> mWallpaperItems = new ArrayList<>();

    private Handler mMainHandler = new Handler();
    private Handler mThreadHandler;
    private HandlerThread mHandlerThread;

    private static CommandExecutor mExecutor;
    public static CommandExecutor getInstance(Context context){
        if(mExecutor == null){
            mExecutor = new CommandExecutor(context);
        }
        return mExecutor;
    }
    private CommandExecutor(Context context){
        mContext = context;
        mHandlerThread = new HandlerThread("command_handler_thread");
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg){
                int what = msg.what;
                Bundle bundle = msg.getData();
                String hashcode = null;
                switch (what){
                    case MSG_APPLY_WALLPAPER:
                        hashcode = bundle.getString("hashcode");
                        boolean islockscreen = bundle.getBoolean("islockscreen");
                        Bitmap wallpaper = ApiClient.getWallpaperByHashCode(hashcode);
                        if(wallpaper != null){
                            CommandExecutor.getInstance(mContext).executeApplyWallpaperTask(new BitmapDrawable(wallpaper), islockscreen);
                        }else{
                            showToast("换壁纸失败");
                        }
                        break;
                    case MSG_SHARE_WALLPAPER:
                        SmartWallpaperHelper.getInstance(mContext).shareCurrentWallpaper();
                        mMainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(ASRDialog.getASRDialogInstance() != null){
                                    ASRDialog.getASRDialogInstance().onCommandFinish(false);
                                }
                            }
                        },500);
                        break;
                    case MSG_TOUCH_VOTEUP_WALLPAPER:
                        if(bundle != null) {
                            hashcode = bundle.getString("hashcode");
                            if (ApiClient.voteUpWallpaper(hashcode)) {
                                showToast("点赞成功");
                            } else {
                                showToast("点赞失败");
                            }
                        }
                        break;
                    case MSG_UPLOAD_WALLPAPER_TO_SERVER:

                        break;
                    case MSG_FAVORITE_WALLPAPER:
                        if(bundle != null) {
                            hashcode = bundle.getString("hashcode");
                            if(SmartWallpaperHelper.getInstance(mContext).favoriteCurrentWallpaper(hashcode)){
                                showToast("收藏成功");
                                return;
                            }
                        }
                        showToast("收藏失败");
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(ASRDialog.getASRDialogInstance() != null){
                                    ASRDialog.getASRDialogInstance().onCommandFinish(false);
                                }
                            }
                        });
                        break;
                    case MSG_FAVORITE_WALLPAPER_LIST:
//                        SmartWallpaperHelper.getInstance(mContext).openFavoriteList();
//                        showToast("打开壁纸收藏夹成功");
//                        mMainHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                if(ASRDialog.getASRDialogInstance() != null){
//                                    ASRDialog.getASRDialogInstance().finish();
//                                }
//                            }
//                        });
                        Intent intent = new Intent(mContext, FavoriteListActivity.class);
                        mContext.startActivity(intent);
                        break;
                    case MSG_RESTORE_WALLPAPER:
                        SmartWallpaperHelper.getInstance(mContext).restoreWallpaper();
                        showToast("还原到之前的壁纸成功");
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(ASRDialog.getASRDialogInstance() != null){
                                    ASRDialog.getASRDialogInstance().finish();
                                }
                            }
                        });
                        break;
                    case MSG_EDIT_WALLPAPER:
                        SmartWallpaperHelper.getInstance(mContext).blurWallpaper();
                        break;
                    case MSG_SELFDEFINE_WALLPAPER:
                        SmartWallpaperHelper.getInstance(mContext).setLiveWallpaper(CameraLiveWallpaper.class);
                        showToast("点击【设置为壁纸】即可设置相机壁纸");
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(ASRDialog.getASRDialogInstance() != null){
                                    ASRDialog.getASRDialogInstance().finish();
                                }
                            }
                        });
                        break;
                    case MSG_LIVE_WALLPAPER:
                        SmartWallpaperHelper.getInstance(mContext).setLiveWallpaper(VideoLiveWallpaper.class);
                        showToast("点击【设置为壁纸】即可设置动态壁纸");
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(ASRDialog.getASRDialogInstance() != null){
                                    ASRDialog.getASRDialogInstance().finish();
                                    VideoLiveWallpaper.voiceNormal(mContext);
                                }
                            }
                        });
                        break;
                    default:
                        Log.i(TAG, "unkown message");
                        break;
                }
            }
        };
    }
    public void quit(){
        mHandlerThread.quit();
    }
    public void showToast(final String text) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static final int MSG_SHARE_WALLPAPER = 0x100;
    private static final int MSG_TOUCH_VOTEUP_WALLPAPER = 0x101;//针对手动点赞
    private static final int MSG_UPLOAD_WALLPAPER_TO_SERVER = 0x102;
    private static final int MSG_FAVORITE_WALLPAPER = 0x103;
    private static final int MSG_FAVORITE_WALLPAPER_LIST = 0x104;
    private static final int MSG_RESTORE_WALLPAPER = 0x105;
    private static final int MSG_EDIT_WALLPAPER = 0x106;
    private static final int MSG_SELFDEFINE_WALLPAPER = 0x107;
    private static final int MSG_LIVE_WALLPAPER = 0x108;
    private static final int MSG_SHAKE_WALLPAPER_ON = 0x109;
    private static final int MSG_SHAKE_WALLPAPER_OFF = 0x10A;
    private static final int MSG_SCHEDULE_WALLPAPER_ON = 0x10A;
    private static final int MSG_SCHEDULE_WALLPAPER_OFF = 0x10A;

    private static final int MSG_APPLY_WALLPAPER = 0x200;

    //run on Main thread
    public boolean execute(final Command cmd){
        Log.i(TAG, "execute-cmd="+cmd.toString());
        boolean handled = false;
        if(RuleId.RULE_ID_1.equals(cmd.getRuleId())){//SetWallpaper

            boolean islockscreen = Boolean.parseBoolean(cmd.getParams().get("islockscreen"));
            if(islockscreen){
                ArrayList<String> hashCodeList = cmd.getHashCodeList();
                if(hashCodeList !=null && hashCodeList.size() >0) {
                    Message msg = Message.obtain();
                    msg.what = MSG_APPLY_WALLPAPER;
                    Bundle data = new Bundle();
                    data.putString("hashcode", hashCodeList.get(0));
                    data.putBoolean("islockscreen", islockscreen);
                    msg.setData(data);
                    mThreadHandler.sendMessage(msg);
                }else{
                    showToast("获取壁纸失败");
                }
                return false;
            }

            if(Action.ACTION_APPLY_WALLPAPER.equals(cmd.getAction())) {//直接应用第一个壁纸
                ArrayList<String> hashCodeList = cmd.getHashCodeList();
                if(hashCodeList !=null && hashCodeList.size() >0) {
                    Message msg = Message.obtain();
                    msg.what = MSG_APPLY_WALLPAPER;
                    Bundle data = new Bundle();
                    data.putString("hashcode", hashCodeList.get(0));
                    data.putBoolean("islockscreen", islockscreen);
                    msg.setData(data);
                    mThreadHandler.sendMessage(msg);
                }else{
                    showToast("获取壁纸失败");
                }
            }else {
                Intent intent = new Intent(mContext, WallpaperListActivity.class);
                intent.putExtra("command", cmd);
                mContext.startActivity(intent);
            }
            handled = true;
        }else if(RuleId.RULE_ID_2.equals(cmd.getRuleId())){//VoteWallpaper
            if(Action.ACTION_VOTEUP_WALLPAPER.equals(cmd.getAction())) {//语音点赞执行完后的反馈
                //反馈
                if (cmd.getResultCode() == 200) {
                    Toast.makeText(mContext, "点赞成功", Toast.LENGTH_SHORT).show();
                    handled = true;
                } else {
                    Toast.makeText(mContext, "点赞失败", Toast.LENGTH_SHORT).show();
                    handled = false;
                }
            }else if(Action.ACTION_TOUCH_VOTEUP_WALLPAPER.equals(cmd.getAction())){//手动点赞命令
                Message msg = Message.obtain();
                msg.what = MSG_TOUCH_VOTEUP_WALLPAPER;
                Bundle data = new Bundle();
                data.putString("hashcode", cmd.getHashCodeList().get(0));
                msg.setData(data);
                mThreadHandler.sendMessage(msg);
                handled = true;
            }

        }else if(RuleId.RULE_ID_3.equals(cmd.getRuleId())){//ShareWallpaper
            //分享当前壁纸
            mThreadHandler.sendEmptyMessage(MSG_SHARE_WALLPAPER);
            handled = true;
        }else if(RuleId.RULE_ID_4.equals(cmd.getRuleId())){//UploadWallpaperToServer
            //将壁纸上传到服务器
            Message message = new Message();
            mThreadHandler.sendEmptyMessage(MSG_SHARE_WALLPAPER);
            handled = true;
        }else if(RuleId.RULE_ID_5.equals(cmd.getRuleId())){//FavoriteWallpaper
            //收藏壁纸
            Message msg = Message.obtain();
            msg.what = MSG_FAVORITE_WALLPAPER;
            Bundle data = new Bundle();
            data.putString("hashcode", cmd.getHashCodeList().get(0));
            msg.setData(data);
            mThreadHandler.sendMessage(msg);
            handled = true;
        }else if(RuleId.RULE_ID_6.equals(cmd.getRuleId())){//FavoriteWallpaperList
            //打开壁纸收藏夹
            mThreadHandler.sendEmptyMessage(MSG_FAVORITE_WALLPAPER_LIST);

            handled = true;
        }else if(RuleId.RULE_ID_7.equals(cmd.getRuleId())){//RestoreWallpaper
            //恢复壁纸
            mThreadHandler.sendEmptyMessage(MSG_RESTORE_WALLPAPER);
            handled = true;
        }else if(RuleId.RULE_ID_8.equals(cmd.getRuleId())){//EditWallpaper
            //编辑壁纸
            mThreadHandler.sendEmptyMessage(MSG_EDIT_WALLPAPER);
            handled = true;
        }else if(RuleId.RULE_ID_9.equals(cmd.getRuleId())){//SelfDefineWallpaper
            //打开透明壁纸
            mThreadHandler.sendEmptyMessage(MSG_SELFDEFINE_WALLPAPER);
            handled = true;
        }else if(RuleId.RULE_ID_10.equals(cmd.getRuleId())){//LiveWallpaper
            //设置视频动画壁纸
            mThreadHandler.sendEmptyMessage(MSG_LIVE_WALLPAPER);
            handled = true;
        }else if(RuleId.RULE_ID_11.equals(cmd.getRuleId())){
            //打开摇一摇换壁纸功能
            Intent intent = new Intent(mContext, ChangeWallpaperService.class);
            intent.setAction(Action.ACTION_ENABLE_SHAKE_LISTEN);
            mContext.startService(intent);
            handled = true;
        }else if(RuleId.RULE_ID_12.equals(cmd.getRuleId())){
            //关闭摇一摇换壁纸功能
            Intent intent = new Intent(mContext, ChangeWallpaperService.class);
            intent.setAction(Action.ACTION_DISABLE_SHAKE_LISTEN);
            mContext.startService(intent);
            handled = true;
        }else if(RuleId.RULE_ID_13.equals(cmd.getRuleId())){
            //打开自动换壁纸功能
            Intent intent = new Intent(mContext, ChangeWallpaperService.class);
            intent.setAction(Action.ACTION_ENABLE_SCHEDULE_CHANGE_WALLPAPER);
            mContext.startService(intent);
            handled = true;
        }else if(RuleId.RULE_ID_14.equals(cmd.getRuleId())){
            //关闭自动换壁纸功能
            Intent intent = new Intent(mContext, ChangeWallpaperService.class);
            intent.setAction(Action.ACTION_DISABLE_SCHEDULE_CHANGE_WALLPAPER);
            mContext.startService(intent);
            handled = true;
        }
        return handled;
    }

    public void executeTask(Runnable r){
        Log.i(TAG, "executeTask");
        mThreadHandler.post(r);
    }

    private Runnable applyWallpaperTask;
    public void executeApplyWallpaperTask(final Drawable drawable){
        Log.i(TAG, "executeApplyWallpaperTask");
        executeApplyWallpaperTask(drawable,false);
    }
    public void executeApplyWallpaperTask(final Drawable drawable, final boolean isLockScreen){
        Log.i(TAG, "executeApplyWallpaperTask");
        if(applyWallpaperTask != null){
            mThreadHandler.removeCallbacks(applyWallpaperTask);
        }

        applyWallpaperTask = new Runnable() {
            @Override
            public void run() {
                if(isLockScreen){
                    SmartWallpaperHelper.getInstance(mContext).setLockScreenWallpaper(((BitmapDrawable)drawable).getBitmap());
                }else {
                    SmartWallpaperHelper.getInstance(mContext).setHomeScreenWallpaper(drawable);
                }
                showToast("应用壁纸成功");
                applyWallpaperTask = null;
            }
        };
        executeTask(applyWallpaperTask);
    }

    public void executeUnFavoriteTask(final String hashcode){
        Log.i(TAG, "executeUnFavoriteTask");
        executeTask(new Runnable() {
            @Override
            public void run() {
                SmartWallpaperHelper.getInstance(mContext).unFavoriteWallpaper(hashcode);
                showToast("取消收藏成功");
            }
        });
    }

    public void uploadWallpaperTask(final String picturePath){
        Log.i(TAG, "uploadWallpaperTask");
        executeTask(new Runnable() {
            @Override
            public void run() {
                File file = new File(picturePath);
                if(file.exists()){
                    String response = ApiClient.uploadWallpaper(picturePath);
                    Log.d(TAG, "response="+response);
                    if(TextUtils.isEmpty(response)){
                        showToast("上传壁纸失败");
                    }else{
                        try {
                            JSONObject jsonResult = new JSONObject(response);
                            int errno = jsonResult.getInt("errno");
                            if (errno == 0) {
                                String hashcode = jsonResult.getString("hashcode");
                                if(!TextUtils.isEmpty(hashcode)) {
//                                    int idx = picturePath.lastIndexOf(".");
//                                    String ext = picturePath.substring(idx);
//                                    if (TextUtils.isEmpty(ext)) {
//                                        ext = WALLPAPER_FILE_EXT;
//                                    }
                                    file = new File(EXTERNAL_UPLOAD_WALLPAPER_DIR);
                                    if(!file.exists()){
                                        file.mkdirs();
                                    }
                                    String dstFilePath = EXTERNAL_UPLOAD_WALLPAPER_DIR + File.separator + hashcode;//+ ext;
                                    file = new File(dstFilePath);
                                    if(!file.exists()){
                                        SmartWallpaperHelper.copyFile(picturePath, dstFilePath);
                                    }
                                }
                                showToast("上传壁纸成功");
                                Intent intent = new Intent(mContext, UploadListActivity.class);
                                mContext.startActivity(intent);
                                return;
                            }
                        }catch (Exception e){
                            Log.e(TAG, "error="+e.toString());
                        }
                        showToast("上传壁纸失败");
                    }
                }
            }
        });
    }
}
