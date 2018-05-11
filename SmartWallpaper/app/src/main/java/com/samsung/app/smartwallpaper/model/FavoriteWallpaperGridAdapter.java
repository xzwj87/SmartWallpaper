package com.samsung.app.smartwallpaper.model;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.app.smartwallpaper.R;
import com.samsung.app.smartwallpaper.command.Command;
import com.samsung.app.smartwallpaper.command.CommandExecutor;
import com.samsung.app.smartwallpaper.command.RuleId;
import com.samsung.app.smartwallpaper.utils.FileUtil;
import com.samsung.app.smartwallpaper.utils.FileUtils;
import com.samsung.app.smartwallpaper.view.WallpaperRecyclerView;
import com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper;

import java.util.ArrayList;

import static com.samsung.app.smartwallpaper.command.Action.ACTION_FAVORITE_WALLPAPER;
import static com.samsung.app.smartwallpaper.command.Action.ACTION_TOUCH_VOTEUP_WALLPAPER;

/**
 * Created by samsung on 2018/3/16.
 * Author: my2013.wang@samsung.com
 */

public class FavoriteWallpaperGridAdapter extends RecyclerView.Adapter<FavoriteWallpaperGridAdapter.WallpaperViewHolder> implements
        View.OnClickListener{

    private static final String TAG = "FavoriteWallpaperGridAdapter";

    private LayoutInflater inflater;
    private Context mContext;
    private WallpaperRecyclerView mRecyclerView;
    private static ArrayList<WallpaperItem> mWallpaperItems = null;

    public FavoriteWallpaperGridAdapter(Context context, WallpaperRecyclerView recyclerView){
        inflater = LayoutInflater.from(context);
        mRecyclerView = recyclerView;
        mContext = context;
    }

    public void setWallpaperItems(ArrayList<WallpaperItem> wallpaperItems){
        mWallpaperItems = wallpaperItems;
        notifyDataSetChanged();
    }

    @Override
    public WallpaperViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.favoritewallpaper_item_grid, parent , false);
        WallpaperViewHolder viewHolder = new WallpaperViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(WallpaperViewHolder holder, int position) {
        WallpaperItem wallpaperItem = mWallpaperItems.get(position);

        holder.fl_item.setTag(position);
        holder.iv_delete.setTag(position);
        holder.tv_apply.setTag(position);
        holder.ib_share.setTag(position);

        holder.fl_item.setOnClickListener(this);
        holder.iv_delete.setOnClickListener(this);
        holder.tv_apply.setOnClickListener(this);
        holder.ib_share.setOnClickListener(this);

        if(wallpaperItem.getWallpaperDrawable() == null) {
            wallpaperItem.setTargetView(holder.iv_wallpaper);
            wallpaperItem.loadWallpaperByPath(wallpaperItem.getWallpaperPath());
        }else{
            holder.iv_wallpaper.setScaleType(ImageView.ScaleType.FIT_XY);
            holder.iv_wallpaper.setImageDrawable(wallpaperItem.getWallpaperDrawable());
        }
        holder.iv_wallpaper.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        if(mWallpaperItems == null){
            return 0;
        }
        return mWallpaperItems.size();
    }

    @Override
    public void onClick(final View v) {
        int id = v.getId();
        final int pos = (int)v.getTag();
        final WallpaperItem wallpaperItem = mWallpaperItems.get(pos);
        ImageView view = null;
        switch (id){
            case R.id.ib_share:
                SmartWallpaperHelper.getInstance(mContext).shareWallpaper(wallpaperItem.getWallpaperDrawable());
                break;
            case R.id.fl_item:
                if(mCb != null) {
                    mCb.onItemClick(pos);
                }
                break;
            case R.id.iv_delete:
                startShakeAnimation(v);
                FileUtils.deleteFileWithPath(wallpaperItem.getWallpaperPath());
                mWallpaperItems.remove(pos);
                notifyDataSetChanged();
                break;
            case R.id.tv_apply:
                startScaleAnimation(v);
                CommandExecutor.getInstance(mContext).executeApplyWallpaperTask(wallpaperItem.getWallpaperDrawable());
                break;
        }
    }

    private static void startShakeAnimation(final View targetView){

        Log.d(TAG, "startVoteUpAnimation");
        final Interpolator alphaInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                if (t <= 1) {
                    return (float) Math.cos(Math.PI/2 * (t / 1.0f));
                }
                return 0;
            }
        };
        final Interpolator scaleInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                return 1.0f+t/2;
            }
        };
        final Interpolator translateInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                return -1.5f*targetView.getHeight() * t;
            }
        };
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(400);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                targetView.setAlpha(0f);
                targetView.setTranslationX(0f);
                targetView.setTranslationY(0f);
                targetView.setScaleX(1.0f);
                targetView.setScaleY(1.0f);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                targetView.setAlpha(1f);
                targetView.setTranslationX(0f);
                targetView.setTranslationY(0f);
                targetView.setScaleX(1.0f);
                targetView.setScaleY(1.0f);
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (float) animation.getAnimatedValue();
                float alpha = alphaInterpolator.getInterpolation(t);
                float scale = scaleInterpolator.getInterpolation(t);
                float translate = translateInterpolator.getInterpolation(t);
                targetView.setAlpha(alpha);
                targetView.setTranslationY(translate);
                targetView.setScaleX(scale);
                targetView.setScaleY(scale);
            }
        });
        anim.start();
    }
    private static void startScaleAnimation(final View targetView){
        Log.d(TAG, "startFavoriteAnimation");
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.4f, 1.0f, 1.4f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(300);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                targetView.setScaleX(1.0f);
                targetView.setScaleY(1.0f);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        targetView.startAnimation(scaleAnimation);
    }

    public static class WallpaperViewHolder extends RecyclerView.ViewHolder{
        FrameLayout fl_item;
        ImageView iv_wallpaper;
        ImageView iv_delete;
        TextView tv_apply;
        ImageButton ib_share;

        public WallpaperViewHolder(View itemView) {
            super(itemView);
            fl_item = (FrameLayout)itemView.findViewById(R.id.fl_item);
            iv_wallpaper = (ImageView)itemView.findViewById(R.id.iv_wallpaper);
            iv_delete = (ImageView)itemView.findViewById(R.id.iv_delete);
            tv_apply = (TextView)itemView.findViewById(R.id.tv_apply);
            ib_share = (ImageButton)itemView.findViewById(R.id.ib_share);
        }
    }

    public interface CallBack{
        void onItemVoteUp(int position);
        void onItemFavorite(int position);
        void onItemApply(int position);
        void onItemClick(int position);
    }

    private CallBack mCb;
    public void setCallBack(CallBack listener){
        mCb = listener;
    }
}


