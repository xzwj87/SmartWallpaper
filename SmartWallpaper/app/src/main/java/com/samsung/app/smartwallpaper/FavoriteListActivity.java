package com.samsung.app.smartwallpaper;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.app.smartwallpaper.command.Action;
import com.samsung.app.smartwallpaper.command.CommandExecutor;
import com.samsung.app.smartwallpaper.model.FavoriteWallpaperGridAdapter;
import com.samsung.app.smartwallpaper.model.PhotoViewPagerAdapter;
import com.samsung.app.smartwallpaper.model.WallpaperGridAdapter;
import com.samsung.app.smartwallpaper.model.WallpaperItem;
import com.samsung.app.smartwallpaper.view.PhotoViewPager;
import com.samsung.app.smartwallpaper.view.WallpaperRecyclerView;
import com.samsung.app.smartwallpaper.wallpaper.ChangeWallpaperService;
import com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper.EXTERNAL_MY_FAVORITE_WALLPAPER_DIR;
import static com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper.saveBitmap;

/**
 * 长按某个图片拖动删除
 */
public class FavoriteListActivity extends Activity  implements View.OnClickListener,
        FavoriteWallpaperGridAdapter.CallBack, PhotoViewPagerAdapter.CallBack{
    private static final String TAG = "FavoriteListActivity";
    private Context mContext;

    private TextView tv_title;
    private Switch play_wallpaper;
    private ImageButton ib_upload;
    private ImageButton ib_close;

    private ProgressBar pb_loadingwait;
    private TextView tv_hint;
    private TextView tv_empty;
    private WallpaperRecyclerView mWallpaperRecyclerView;

    private GridLayoutManager mGridLayoutManager = null;
    private FavoriteWallpaperGridAdapter mGridAdapter = null;
    private ArrayList<WallpaperItem> mWallpaperItems;

    private FrameLayout fl_wallpaper_preview;
    private PhotoViewPager mViewPager;
    private PhotoViewPagerAdapter mPhotoViewPagerAdapter;
    private TextView tv_apply;
    private ImageButton ib_share;
    private TextView tv_index;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        mWallpaperItems = new ArrayList<>();
        setContentView(R.layout.favorite_list_layout);
        initView();
        loadWallpaperItems();
    }
    private void initView() {
        Window window = this.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        tv_title = (TextView)findViewById(R.id.tv_title);
        play_wallpaper = (Switch)findViewById(R.id.play_wallpaper);
        play_wallpaper.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                Toast.makeText(mContext, "isChecked="+isChecked, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(FavoriteListActivity.this, ChangeWallpaperService.class);
                if(isChecked){
                    //启动切换壁纸
                    if (isServiceRunning(ChangeWallpaperService.class.getName())){
                        Toast.makeText(mContext, "已开启自动切换壁纸服务", Toast.LENGTH_SHORT).show();
//                        return;
                    }else{
                        Toast.makeText(mContext, "自动切换壁纸服务开启", Toast.LENGTH_SHORT).show();
                    }
                    intent.setAction(Action.ACTION_START_TIMER_CHANGE_WALLPAPER);
                    startService(intent);
                }else{
                    intent.setAction(Action.ACTION_STOP_TIMER_CHANGE_WALLPAPER);
                    startService(intent);

                    stopService(intent);
                    Toast.makeText(mContext, "自动切换壁纸服务关闭", Toast.LENGTH_SHORT).show();
                }

                SharedPreferences sp = getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean("enableChangeWallpaper", isChecked);
                editor.apply();
            }
        });
        SharedPreferences sp = getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
        boolean enableChangeWallpaper = sp.getBoolean("enableChangeWallpaper", false);
        if(enableChangeWallpaper || isServiceRunning(ChangeWallpaperService.class.getName())){
            play_wallpaper.setChecked(true);
        }else{
            play_wallpaper.setChecked(false);
        }

        ib_upload = (ImageButton)findViewById(R.id.ib_upload);
        ib_close = (ImageButton)findViewById(R.id.ib_close);
        pb_loadingwait = (ProgressBar)findViewById(R.id.pb_loadingwait);
        tv_hint = (TextView)findViewById(R.id.tv_hint);
        tv_empty = (TextView)findViewById(R.id.tv_empty);
        mWallpaperRecyclerView = (WallpaperRecyclerView)findViewById(R.id.wallpaper_recycleview);

        ib_close.setOnClickListener(this);
        ib_upload.setOnClickListener(this);

        mGridAdapter = new FavoriteWallpaperGridAdapter(mContext, mWallpaperRecyclerView);
        mGridLayoutManager = new GridLayoutManager(mContext, 2);
        mWallpaperRecyclerView.setAdapter(mGridAdapter);
        mWallpaperRecyclerView.setLayoutManager(mGridLayoutManager);
        mWallpaperRecyclerView.setItemAnimator(new DefaultItemAnimator());
        showEmptyView(true);

        mGridAdapter.setCallBack(this);
        mWallpaperRecyclerView.setCallBack(new WallpaperRecyclerView.CallBack() {
            @Override
            public void close() {
                FavoriteListActivity.this.finish();
            }
        });
        mWallpaperRecyclerView.addItemDecoration(new FavoriteListActivity.SpaceItemDecoration(0));

        fl_wallpaper_preview = (FrameLayout)findViewById(R.id.fl_wallpaper_preview);
        mViewPager = (PhotoViewPager)findViewById(R.id.view_paper);
        tv_apply = (TextView)findViewById(R.id.tv_apply);
        ib_share = (ImageButton)findViewById(R.id.ib_share);
        tv_index = (TextView)findViewById(R.id.tv_index);

        tv_apply.setOnClickListener(this);
        ib_share.setOnClickListener(this);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                updateWallpaperPreviewUI(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
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

    AsyncTask<String, Void, String> mLoadTask;
    public void loadWallpaperItems() {
        Log.i(TAG, "loadWallpaperItems");
        mWallpaperItems.clear();
        if(mLoadTask != null){
            mLoadTask.cancel(true);
        }
        mLoadTask = new AsyncTask<String, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                pb_loadingwait.setVisibility(View.VISIBLE);
                tv_empty.setVisibility(View.GONE);
            }

            @Override
            protected String doInBackground(String... params) {
                File myfavoritelist_dir = new File(EXTERNAL_MY_FAVORITE_WALLPAPER_DIR);
                File[] files = myfavoritelist_dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        String lowerName = name.toLowerCase();
                        if(lowerName.endsWith(".png") || lowerName.endsWith(".jpg")|| lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp")){
                            return true;
                        }
                        return false;
                    }
                });

                for(File child:files){
                    WallpaperItem item = new WallpaperItem();
                    item.setWallpaperPath(child.getAbsolutePath());
                    mWallpaperItems.add(item);
                    Log.i(TAG,"mWallpaperItems.add-child.getAbsolutePath()="+child.getAbsolutePath());
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                pb_loadingwait.setVisibility(View.GONE);
                if(mWallpaperItems.size() == 0){
                    showEmptyView(true);
                    return;
                }
                showEmptyView(false);
                mGridAdapter.setWallpaperItems(mWallpaperItems);
                mWallpaperRecyclerView.scrollToPosition(0);
            }
        };
        mLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



    public void updateWallpaperPreviewUI(int position){
        WallpaperItem wallpaperItem = mWallpaperItems.get(position);
        tv_index.setText(String.format("%d/%d", position+1, mWallpaperItems.size()));
    }

    @Override
    public void onItemVoteUp(int position) {

    }

    @Override
    public void onItemFavorite(int position) {

    }

    @Override
    public void onItemApply(int position) {

    }

    @Override
    public void onItemClick(int position) {
        showWallpaperPreview(position);
    }

    @Override
    public void onExitWallpaperPreview() {
        hideWallpaperPreview();
    }

    class SpaceItemDecoration extends RecyclerView.ItemDecoration {

        int mSpace;
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
//            outRect.left = mSpace;
//            outRect.right = mSpace;
            outRect.bottom = mSpace;
            if (parent.getChildAdapterPosition(view) == 0) {
//                outRect.top = mSpace;
            }
        }
        public SpaceItemDecoration(int space) {
            this.mSpace = space;
        }
    }

    public void showEmptyView(boolean bShowEmptyView){
        Log.i(TAG, "showEmptyView="+bShowEmptyView);
        if(bShowEmptyView) {
            mWallpaperRecyclerView.setVisibility(GONE);
            tv_empty.setVisibility(VISIBLE);
        }else{
            mWallpaperRecyclerView.setVisibility(VISIBLE);
            tv_empty.setVisibility(GONE);
        }
    }
    @Override
    public void onClick(View v) {
        int id = v.getId();
        WallpaperItem wallpaperItem;
        int pos;
        switch (id){
            case R.id.ib_upload:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 100);
                break;
            case R.id.ib_close:
                finish();
                break;
            case R.id.tv_apply:
                pos = mViewPager.getCurrentItem();
                wallpaperItem = mWallpaperItems.get(pos);
                CommandExecutor.getInstance(mContext).executeApplyWallpaperTask(wallpaperItem.getWallpaperDrawable());
                break;
            case R.id.ib_share:
                pos = mViewPager.getCurrentItem();
                wallpaperItem = mWallpaperItems.get(pos);
                SmartWallpaperHelper.getInstance(mContext).shareWallpaper(wallpaperItem.getWallpaperDrawable());
                break;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap wallpaper = BitmapFactory.decodeFile(picturePath);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            Date date = new Date(System.currentTimeMillis());

            int idx = picturePath.lastIndexOf(".");
            String ext = picturePath.substring(idx);
            if(TextUtils.isEmpty(ext)){
                ext = ".jpg";
            }
            saveBitmap(wallpaper, EXTERNAL_MY_FAVORITE_WALLPAPER_DIR + File.separator + simpleDateFormat.format(date) + ext);
            showHint("正在上传...");
        }
    }

    @Override
    public void onBackPressed() {
        if(fl_wallpaper_preview.getVisibility() == View.VISIBLE){
            hideWallpaperPreview();
            return;
        }
        super.onBackPressed();
    }

    public void showWallpaperPreview(int pos) {
        Log.i(TAG, "showWallpaperPreview");
        mPhotoViewPagerAdapter = new PhotoViewPagerAdapter(mContext);
        mPhotoViewPagerAdapter.setWallpaperItems(mWallpaperItems);
        mPhotoViewPagerAdapter.setCallBack(this);
        mViewPager.setAdapter(mPhotoViewPagerAdapter);
        mViewPager.setCurrentItem(pos);
        updateWallpaperPreviewUI(pos);
        mViewPager.setOffscreenPageLimit(3);
        fl_wallpaper_preview.setVisibility(View.VISIBLE);

        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.0f, 1.0f, 0.0f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(80);
        fl_wallpaper_preview.startAnimation(scaleAnimation);
    }
    public void hideWallpaperPreview(){
        fl_wallpaper_preview.setVisibility(View.GONE);
        mGridAdapter.notifyDataSetChanged();
    }

    private void showHint(String hintText){
        Log.d(TAG, "showHint-hintText="+hintText);
        tv_hint.setText(hintText);
        TranslateAnimation showTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -tv_hint.getHeight(),
                Animation.RELATIVE_TO_SELF,0.0f);
        showTranslateAnimation.setDuration(2000);
        showTranslateAnimation.setFillAfter(false);
        showTranslateAnimation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation) {
                tv_hint.setTranslationY(-tv_hint.getHeight());
                tv_hint.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                tv_hint.setVisibility(VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideHint();
                    }
                },5000);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        tv_hint.startAnimation(showTranslateAnimation);
    }
    private void hideHint(){
        Log.d(TAG, "hideHint");
        TranslateAnimation hideTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -tv_hint.getHeight());
        hideTranslateAnimation.setDuration(3000);
        hideTranslateAnimation.setFillAfter(false);
        hideTranslateAnimation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                tv_hint.setVisibility(GONE);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        tv_hint.startAnimation(hideTranslateAnimation);
    }
}
