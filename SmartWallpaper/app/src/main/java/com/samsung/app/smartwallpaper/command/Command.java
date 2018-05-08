package com.samsung.app.smartwallpaper.command;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by ASUS on 2017/12/21.
 */

public class Command implements Serializable{
    private String mRuleId;
    private String mAction;
    private HashMap<String,String> mParams;
    private ArrayList<String> mHashCodeList;
    private ArrayList<Integer> mVoteUpCntList;
    private String mUttSeg;
    private int mResultCode;
    private HashMap<String, Boolean> mExtra;

    public String getRuleId() {
        return mRuleId;
    }

    public void setRuleId(String ruleId) {
        this.mRuleId = ruleId;
    }

    public String getAction() {
        return mAction;
    }

    public void setAction(String action) {
        this.mAction = action;
    }

    public HashMap<String,String> getParams() {
        return mParams;
    }

    public void setParams(HashMap<String, String> params) {
        this.mParams = params;
    }

    public ArrayList<String> getHashCodeList() {
        return mHashCodeList;
    }

    public void setHashCodeList(ArrayList<String> hashCodeList) {
        this.mHashCodeList = hashCodeList;
    }
    public ArrayList<Integer> getVoteUpCntList() {
        return mVoteUpCntList;
    }

    public void setVoteUpCntList(ArrayList<Integer> voteUpCntList) {
        this.mVoteUpCntList = voteUpCntList;
    }

    public String getUttSeg() {
        return mUttSeg;
    }

    public void setUttSeg(String uttSeg) {
        this.mUttSeg = uttSeg;
    }

    public int getResultCode() {
        return mResultCode;
    }

    public void setResultCode(int resultCode) {
        this.mResultCode = resultCode;
    }

    public void setExtra(HashMap<String, Boolean> extra){
        mExtra = extra;
    }
    public boolean getBooleanExtra(String key){
        if(mExtra != null){
            return mExtra.get(key);
        }
        return false;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append("uttSeg="+mUttSeg);
        sb.append(", ");
        sb.append("ruleId="+mRuleId);
        sb.append(", ");
        sb.append("action="+mAction);
        sb.append(", ");
        sb.append("parameters=[");
        if(mParams != null) {
            for (String paramName : mParams.keySet()) {
                sb.append(paramName + "=" + mParams.get(paramName));
                sb.append(",");
            }
            sb.deleteCharAt(sb.lastIndexOf(","));
        }
        sb.append("]");
        sb.append(", ");
        sb.append("hashCodeList=[");
        if(mHashCodeList != null && mHashCodeList.size()>0) {
            for (String hashcode : mHashCodeList) {
                sb.append(hashcode);
                sb.append(",");
            }
            sb.deleteCharAt(sb.lastIndexOf(","));
        }
        sb.append("]");

        sb.append(", ");
        sb.append("extra=[");
        if(mExtra != null && mExtra.size()>0) {
            for (String key : mExtra.keySet()) {
                sb.append(key+"="+mExtra.get(key));
                sb.append(",");
            }
            sb.deleteCharAt(sb.lastIndexOf(","));
        }
        sb.append("]");

        sb.append(" }");
        return sb.toString();
    }
}
