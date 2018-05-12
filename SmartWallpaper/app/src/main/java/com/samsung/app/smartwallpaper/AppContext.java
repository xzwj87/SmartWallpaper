package com.samsung.app.smartwallpaper;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.samsung.app.smartwallpaper.command.Action;
import com.samsung.app.smartwallpaper.utils.PermisionUtil;
import com.samsung.app.smartwallpaper.wakeup.WakeupService;
import com.samsung.app.smartwallpaper.utils.Logger;
import com.samsung.app.smartwallpaper.utils.StringUtils;
import com.samsung.app.smartwallpaper.wallpaper.ChangeWallpaperService;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class AppContext extends Application{
	public static Context appContext;
	
	@Override
	public void onCreate() {
		super.onCreate();
		appContext = this;
        init();
	}

	private void init(){
		startService();
	}	
	
	private void startService(){
		Intent intent = new Intent();
		intent.setClass(this, WakeupService.class);
		startService(intent);

		SharedPreferences sp = getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
		boolean enableChangeWallpaper = sp.getBoolean("enableScheduleChangeWallpaper", false);
		if(enableChangeWallpaper) {
			intent = new Intent(this, ChangeWallpaperService.class);
			intent.setAction(Action.ACTION_ENABLE_SCHEDULE_CHANGE_WALLPAPER);
			startService(intent);
		}else{
			//仅仅只是启动服务
			if(!isServiceRunning(ChangeWallpaperService.class.getName())) {
				intent = new Intent(this, ChangeWallpaperService.class);
				startService(intent);
			}
		}

		boolean enableShakeListen = sp.getBoolean("enableShakeListen", false);
		if(enableShakeListen) {
			intent = new Intent(this, ChangeWallpaperService.class);
			intent.setAction(Action.ACTION_ENABLE_SHAKE_LISTEN);
			startService(intent);
		}else{
			//仅仅只是启动服务
			if(!isServiceRunning(ChangeWallpaperService.class.getName())) {
				intent = new Intent(this, ChangeWallpaperService.class);
				startService(intent);
			}
		}
	}

	/**
	 * 判断服务是否运行
	 */
	private boolean isServiceRunning(final String className) {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> info = activityManager.getRunningServices(Integer.MAX_VALUE);
		if (info == null || info.size() == 0) return false;
		for (ActivityManager.RunningServiceInfo aInfo : info) {
			if (className.equals(aInfo.service.getClassName())) return true;
		}
		return false;
	}

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
}
