package com.samsung.app.smartwallpaper.notice;

import org.json.JSONObject;

/**
 * Author: my2013.wang
 * Date: 2017-12-21
 * Des:消息提示基础类
 */

public abstract class Notice {

    public Notice() {
        super();
    }
    public JSONObject parseResponse(String res){
        JSONObject result = new JSONObject();
        try {

        }catch (Exception e){

        }finally {

        }
        return result;
    }
    public String requestNotice(NoticeType type){
        String res = null;


        return res;
    }

}
