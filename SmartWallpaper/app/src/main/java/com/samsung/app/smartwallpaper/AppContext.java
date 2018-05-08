package com.samsung.app.smartwallpaper;

import java.util.Properties;
import java.util.UUID;

import com.samsung.app.smartwallpaper.utils.PermisionUtil;
import com.samsung.app.smartwallpaper.wakeup.WakeupService;
import com.samsung.app.smartwallpaper.utils.Logger;
import com.samsung.app.smartwallpaper.utils.StringUtils;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class AppContext extends Application{
	private boolean isLogin = false;	//登录状态
	private int loginUid = 0;	//登录用户的id
	private Logger logger = Logger.getLogger(AppContext.class);
	public static boolean gifRunning = true;//gif是否运行
	
	private String saveImagePath;//保存图片路径

	public static Context appContext;
	
	@Override
	public void onCreate() {
		super.onCreate();
		appContext = this;
		logger.i("Application starts");
//        Thread.setDefaultUncaughtExceptionHandler(AppException.getAppExceptionHandler());
        init();
	}

	private void init(){
		//检测读写权限
		startService();

		//设置保存图片的路径
		saveImagePath = getProperty(AppConfig.SAVE_IMAGE_PATH);
		if(StringUtils.isEmpty(saveImagePath)){
			setProperty(AppConfig.SAVE_IMAGE_PATH, AppConfig.DEFAULT_SAVE_IMAGE_PATH);
			saveImagePath = AppConfig.DEFAULT_SAVE_IMAGE_PATH;
		}
	}	
	
	private void startService(){
		logger.i("start IMService");
		Intent intent = new Intent();
		intent.setClass(this, WakeupService.class);
		startService(intent);
	}
	
	/**
	 * 获取App安装包信息
	 * @return
	 */
	public PackageInfo getPackageInfo() {
		PackageInfo info = null;
		try { 
			info = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {    
			e.printStackTrace(System.err);
		} 
		if(info == null) info = new PackageInfo();
		return info;
	}
	
	/**
	 * 获取App唯一标识
	 * @return
	 */
	public String getAppId() {
		String uniqueID = this.getPackageName();
		if(StringUtils.isEmpty(uniqueID)){
			uniqueID = UUID.randomUUID().toString();
			setProperty("appId", uniqueID);
		}
		return uniqueID;
	}
	
	public boolean containsProperty(String key){
		Properties props = getProperties();
		 return props.containsKey(key);
	}

	public void setProperties(Properties ps){
		AppConfig.getAppConfig(this).set(ps);
	}

	public Properties getProperties(){
		return AppConfig.getAppConfig(this).get();
	}
	
	public void setProperty(String key,String value){
		AppConfig.getAppConfig(this).set(key, value);
	}
	
	public String getProperty(String key){
		return AppConfig.getAppConfig(this).get(key);
	}
	public void removeProperty(String...key){
		AppConfig.getAppConfig(this).remove(key);
	}

	/**
	 * 是否启动检查更新
	 * @return
	 */
	public boolean isCheckUp()
	{
		String perf_checkup = "true";
		//默认是开启
		if(StringUtils.isEmpty(perf_checkup))
			return true;
		else
			return StringUtils.toBool(perf_checkup);
	}
	
	/**
	 * 检测网络是否可用
	 * @return
	 */
	public boolean isNetworkConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnectedOrConnecting();
	}


}
