package com.samsung.app.smartwallpaper.model;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.samsung.app.smartwallpaper.WallpaperListActivity;
import com.samsung.app.smartwallpaper.view.DragPhotoView;

import java.util.ArrayList;


/**
 * Created by ASUS on 2018/5/6.
 */

public class PhotoViewPagerAdapter extends PagerAdapter {
    private static final String TAG = "PhotoViewPagerAdapter";
    private ArrayList<WallpaperItem> mWallpaperItems;
    private Context mContext;

    public PhotoViewPagerAdapter(Context context) {
        this.mContext = context;
    }

    public PhotoViewPagerAdapter(Context context, ArrayList<WallpaperItem> wallpaperItems) {
        this.mContext = context;
        this.mWallpaperItems = wallpaperItems;
    }

    public void setWallpaperItems(ArrayList<WallpaperItem> wallpaperItems){
        mWallpaperItems = wallpaperItems;
        notifyDataSetChanged();
    }

    private DragPhotoView.CallBack mDragPhotoViewCb;
    public void setDragPhotoViewCallBack(DragPhotoView.CallBack cb){
        mDragPhotoViewCb = cb;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        WallpaperItem wallpaperItem = mWallpaperItems.get(position);
        final DragPhotoView dragPhotoView = new DragPhotoView(mContext);
        dragPhotoView.setTag(position);
        dragPhotoView.setZoomable(true);
        dragPhotoView.setMinScale(0.95f);

        if(wallpaperItem.getWallpaperDrawable() == null) {
            wallpaperItem.setWallpaperView(dragPhotoView);
            if(TextUtils.isEmpty(wallpaperItem.getWallpaperLocalPath())) {
                wallpaperItem.loadWallpaperByHashCode(wallpaperItem.getHashCode());
            }else{
                wallpaperItem.loadWallpaperByPath(wallpaperItem.getWallpaperLocalPath());
            }
        }else{
            dragPhotoView.setScaleType(ImageView.ScaleType.FIT_XY);
            dragPhotoView.setImageDrawable(wallpaperItem.getWallpaperDrawable());
        }
        container.addView(dragPhotoView);
        dragPhotoView.setOnExitListener(new DragPhotoView.OnExitListener() {
            @Override
            public void onExit(DragPhotoView view, float x, float y, float w, float h) {
                if(mCb != null){
                    mCb.onExitWallpaperPreview();
                }
            }
        });
        dragPhotoView.setCallBack(mDragPhotoViewCb);
        return dragPhotoView;
    }

    @Override
    public int getCount() {
        return mWallpaperItems != null ? mWallpaperItems.size() : 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public interface CallBack{
        void onExitWallpaperPreview();
    }
    private CallBack mCb;
    public void setCallBack(CallBack listener){
        mCb = listener;
    }
}
