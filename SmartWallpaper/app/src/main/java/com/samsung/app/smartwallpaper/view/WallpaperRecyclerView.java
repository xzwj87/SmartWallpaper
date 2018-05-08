package com.samsung.app.smartwallpaper.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by samsung on 2018/3/15.
 * Author: my2013.wang@samsung.com
 */

public class WallpaperRecyclerView extends RecyclerView{
    private final static String TAG = "WallpaperRecyclerView";
    float touchDownY, latestX, latestY, diffY;
    long downTime,diffTime;
    private CallBack mCb;

    public WallpaperRecyclerView(Context context) {
        this(context, null);
    }

    public WallpaperRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownY = event.getY();
                downTime = event.getEventTime();
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownY = event.getY();
                downTime = event.getEventTime();
                break;
            case MotionEvent.ACTION_UP:
                latestX = event.getX();
                latestY = event.getY();
                float slope = Math.abs(latestY/latestX);
                diffY = latestY - touchDownY;
                diffTime = event.getEventTime() - downTime;
                if(diffY > 700 && diffTime < 400 && slope >1 && !canScrollVertically(-1)){
                    Log.d(TAG, "onTouchEvent-ACTION_UP-need to dismiss dialog");
                    if(mCb != null) {
                        //mCb.close();
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public interface CallBack{
        void close();
    }

    public void setCallBack(CallBack cb){
        mCb = cb;
    }
}
