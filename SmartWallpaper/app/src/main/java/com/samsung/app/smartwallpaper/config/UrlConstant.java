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
    
    public final static String HOST = "193.112.122.156";//线上服务器
//    public final static String HOST = "192.168.0.101";//本地调试用
    public final static String PORT_VERSION = "8886";//
    public final static String PORT_TS ="8887";//
    public final static String PORT_WRS ="8888";//

    public final static String HOST_URL_TS = "http://" + HOST + ":" + PORT_TS + File.separator;
    public final static String HOST_URL_WRS = "http://" + HOST + ":" + PORT_WRS + File.separator;

    public final static String GET_WALLPAPER_VOTEUP_COUNT_URL = HOST_URL_WRS + "api/getwallpapervoteupcount/hashcode=";
    public final static String GET_WALLPAPER_FILE_PATH_URL = HOST_URL_WRS + "api/getwallpaperfilepath/hashcode=";
    public final static String DOWNLOAD_WALLPAPER_URL = HOST_URL_WRS + "api/downloadwallpaper/hashcode=";
    public final static String VOTEUP_WALLPAPER_URL = HOST_URL_WRS + "api/voteupwallpaper/hashcode=";
    public final static String SEARCH_WALLPAPER_URL = HOST_URL_WRS + "api/searchwallpaper";
    public final static String SEARCH_WALLPAPER_WITH_HOT_KEYWORDS_URL = HOST_URL_WRS + "api/searchwallpaperwithhotkeywords";
    public final static String UPLOAD_WALLPAPER_URL = HOST_URL_WRS + "api/uploadwallpaper";
    public final static String UPLOAD_USER_BEHAVIOR_URL = HOST_URL_WRS + "api/uploaduserbehavior/keywords=%s&behavior_type=%d";
    public final static String GET_HOT_KEYWORDS_URL = HOST_URL_WRS + "api/gethotkeywords/top_count=";

    public final static String HOST_URL_VERSION = HOST_URL_WRS + "checkversion";
}
