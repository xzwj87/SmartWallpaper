package com.samsung.app.smartwallpaper.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by samsung on 2018/3/15.
 * Author: my2013.wang@samsung.com
 */

public class WallpaperRecyclerView extends RecyclerView{
    private final static String TAG = "WallpaperRecyclerView";
    float touchDownX, touchDownY, latestX, latestY, diffX, diffY;
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
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                touchDownY = event.getY();
                downTime = event.getEventTime();
                break;
            case MotionEvent.ACTION_UP:
                if(mCb != null) {
                    mCb.onTouchUp();
                }
                latestX = event.getX();
                latestY = event.getY();
                diffX = latestX - touchDownX;
                diffY = latestY - touchDownY;
                diffTime = event.getEventTime() - downTime;
                if(diffX > 200 && diffTime < 800 && Math.abs(diffY)<Math.abs(diffX) && !canScrollHorizontally(-1)){
                    if(mCb != null) {
                        mCb.onSwipe(true);
                        return true;
                    }
                }else if(diffX < -200 && diffTime < 800 && Math.abs(diffY)<Math.abs(diffX) && !canScrollHorizontally(1)){
                    if(mCb != null) {
                        mCb.onSwipe(false);
                        return true;
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    public interface CallBack{
        void onSwipe(boolean fromLtoR);
        void onTouchUp();
    }

    public void setCallBack(CallBack cb){
        mCb = cb;
    }
}
