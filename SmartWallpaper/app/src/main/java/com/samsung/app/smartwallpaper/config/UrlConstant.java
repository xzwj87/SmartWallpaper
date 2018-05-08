package com.samsung.app.smartwallpaper.config;

import android.webkit.URLUtil;

import java.io.File;
import java.nio.file.Path;

/**
 * Author: my2013.wang
 * Date: 2017-12-21
 * Des:url配置
 */

public class UrlConstant {
    
    public final static String HOST ="193.112.122.156";
    public final static String PORT_VERSION ="8886";//
    public final static String PORT_TS ="8887";//
    public final static String PORT_WRS ="8888";//

    public final static String HOST_URL_TS = "http://" + HOST + ":" + PORT_TS + File.separator;
    public final static String HOST_URL_WRS = "http://" + HOST + ":" + PORT_WRS + File.separator;


    public final static String GET_WALLPAPER_FILE_PATH_URL = HOST_URL_WRS + "api/getwallpaperfilepath/hashcode=";
    public final static String DOWNLOAD_WALLPAPER_URL = HOST_URL_WRS + "api/downloadwallpaper/hashcode=";
    public final static String VOTEUP_WALLPAPER_URL = HOST_URL_WRS + "api/voteupwallpaper/hashcode=";
    public final static String SEARCH_WALLPAPER_URL = HOST_URL_WRS + "api/searchwallpaper";



    public final static String HOST_URL_VERSION = HOST_URL_WRS + "checkversion";
}
