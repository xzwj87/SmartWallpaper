package com.samsung.app.smartwallpaper;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.TextView;

import com.samsung.app.smartwallpaper.model.PhotoViewPagerAdapter;
import com.samsung.app.smartwallpaper.model.WallpaperGridAdapter;
import com.samsung.app.smartwallpaper.model.WallpaperItem;
import com.samsung.app.smartwallpaper.view.PhotoViewPager;
import com.samsung.app.smartwallpaper.view.WallpaperRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.co.senab.photoview.PhotoView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by ASUS on 2018/4/22.
 */

public class WallpaperPreviewDialog extends Dialog implements View.OnClickListener{
    private final String TAG = "WallpaperListActivity";
    private Context mContext;

    private View mContentView, mDecorView;

    float touchDownY, latestY;
    int deltaY;

    private PhotoViewPager mViewPager;
    private int currentPosition;
    private PhotoViewPagerAdapter adapter;

    private static WallpaperPreviewDialog wallpaperPreviewDialog = null;
    public static WallpaperPreviewDialog getInstance(Context context){
        if(wallpaperPreviewDialog == null){
            wallpaperPreviewDialog = new WallpaperPreviewDialog(context);
        }
        return wallpaperPreviewDialog;
    }

    private WallpaperPreviewDialog(Context context) {
        super(context, R.style.BottomAnimDialogStyle);
//        super(context);
        mContext = context;
        initView();
    }

    private void initView() {
        mContentView = LayoutInflater.from(mContext).inflate(R.layout.wallpaper_preview_layout, null);

        Window window = this.getWindow();
        if (window != null) { //设置dialog的布局样式 让其位于底部
            window.setGravity(Gravity.TOP);
            WindowManager.LayoutParams lp = window.getAttributes();
//            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.width = this.getContext().getResources().getDisplayMetrics().widthPixels;
//            lp.height = (int)(this.getContext().getResources().getDisplayMetrics().heightPixels*0.95f);
            lp.height = this.getContext().getResources().getDisplayMetrics().heightPixels;
            window.setAttributes(lp);
            window.setContentView(mContentView);

            ViewGroup.LayoutParams layoutParams = mContentView.getLayoutParams();
            layoutParams.width = this.getContext().getResources().getDisplayMetrics().widthPixels;
//            layoutParams.height = (int)(this.getContext().getResources().getDisplayMetrics().heightPixels*0.95f);
            layoutParams.height = this.getContext().getResources().getDisplayMetrics().heightPixels;
            mContentView.setLayoutParams(layoutParams);
            mDecorView = window.getDecorView();
            mViewPager = (PhotoViewPager)mContentView.findViewById(R.id.view_paper);
        }

    }

    public void show(ArrayList<WallpaperItem> wallpaperItems, int pos) {
        Log.i(TAG, "show");
        adapter = new PhotoViewPagerAdapter(mContext);
        adapter.setWallpaperItems(wallpaperItems);
        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(pos);
        mViewPager.setOffscreenPageLimit(3);
        super.show();
    }

    @Override
    public void dismiss() {
        Log.i(TAG, "dismiss");
        super.dismiss();
        wallpaperPreviewDialog = null;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.ib_close:
                wallpaperPreviewDialog.dismiss();
                break;
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        boolean consumed = super.onTouchEvent(event);
        if(!consumed) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchDownY = event.getY();
                    latestY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    moveTo(event.getY() - touchDownY);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                default:
                    if(mContentView.getY() >= mContentView.getHeight()/8) {
                        startHideAnimation();
                    }else{
                        startShowAnimation();
                    }
                    break;
            }
        }
        return consumed;
    }

    private void moveTo(float y){
        if(y < 0){
            y = 0;
        }else if(y > mContentView.getHeight()/2) {
            y = mContentView.getHeight()/2;
        }
        mContentView.setY(y);
    }

    private void startShowAnimation(){
        Log.d(TAG, "startShowAnimation");
        TranslateAnimation showTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.ABSOLUTE, mContentView.getY(),
                Animation.ABSOLUTE,0.0f);
        showTranslateAnimation.setDuration(200);
        showTranslateAnimation.setFillAfter(false);
        showTranslateAnimation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                mContentView.setVisibility(VISIBLE);
                mContentView.setY(0);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mContentView.startAnimation(showTranslateAnimation);
    }

    private void startHideAnimation(){
        Log.d(TAG, "startHideAnimation");
        TranslateAnimation hideTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.ABSOLUTE, mContentView.getY(),
                Animation.ABSOLUTE, mContentView.getHeight());
        hideTranslateAnimation.setDuration(200);
        hideTranslateAnimation.setFillAfter(false);
        hideTranslateAnimation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                mContentView.setY(mContentView.getHeight());
                WallpaperPreviewDialog.this.dismiss();
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mContentView.startAnimation(hideTranslateAnimation);
    }
}
