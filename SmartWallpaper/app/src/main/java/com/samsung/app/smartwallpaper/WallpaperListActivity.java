package com.samsung.app.smartwallpaper;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.samsung.app.smartwallpaper.command.Action;
import com.samsung.app.smartwallpaper.command.Command;
import com.samsung.app.smartwallpaper.command.CommandExecutor;
import com.samsung.app.smartwallpaper.command.RuleId;
import com.samsung.app.smartwallpaper.config.UrlConstant;
import com.samsung.app.smartwallpaper.model.PhotoViewPagerAdapter;
import com.samsung.app.smartwallpaper.model.WallpaperGridAdapter;
import com.samsung.app.smartwallpaper.model.WallpaperItem;
import com.samsung.app.smartwallpaper.network.ApiClient;
import com.samsung.app.smartwallpaper.utils.PopupWindowHelper;
import com.samsung.app.smartwallpaper.view.DragPhotoView;
import com.samsung.app.smartwallpaper.view.PhotoViewPager;
import com.samsung.app.smartwallpaper.view.SearchBox;
import com.samsung.app.smartwallpaper.view.TagContainer;
import com.samsung.app.smartwallpaper.view.WallpaperRecyclerView;
import com.samsung.app.smartwallpaper.wallpaper.CameraLiveWallpaper;
import com.samsung.app.smartwallpaper.wallpaper.ChangeWallpaperService;
import com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper;
import com.samsung.app.smartwallpaper.wallpaper.VideoLiveWallpaper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.samsung.app.smartwallpaper.command.Action.ACTION_TOUCH_VOTEUP_WALLPAPER;

/**
 * Created by ASUS on 2018/4/22.
 */

public class WallpaperListActivity extends Activity implements View.OnClickListener,
        WallpaperGridAdapter.CallBack, PhotoViewPagerAdapter.CallBack, DragPhotoView.CallBack{
    private final String TAG = "WallpaperListActivity";
    private Context mContext;
    private View mDecorView;

    float touchDownY, latestY;
    int deltaY;

    private TextView tv_title;
    private SearchBox searchbox;
    private TagContainer tag_container;
    private ImageButton ib_favorite;
    private ImageButton ib_close;
    private TextView tv_hint;
    private TextView tv_empty;
    private WallpaperRecyclerView mWallpaperRecyclerView;
    private LinearLayout ll_favoritelist;
    private LinearLayout ll_nextbatch;
    private LinearLayout ll_settings;

    private GridLayoutManager mGridLayoutManager = null;
    private WallpaperGridAdapter mGridAdapter = null;
    private ArrayList<WallpaperItem> mWallpaperItems;
    private HashMap<String,String> mParams = null;

    private FrameLayout fl_wallpaper_preview;
    private PhotoViewPager mViewPager;
    private int curPos;
    private PhotoViewPagerAdapter mPhotoViewPagerAdapter;
    private ImageButton ib_voteup_icon;
    private TextView tv_voteup_count;
    private TextView tv_apply;
    private ImageButton ib_share;
    private TextView tv_index;

    private PopupWindowHelper morePopupWindowHelper;
    private PopupWindowHelper contextPopupWindowHelper;
    private View moreMenuPopupView;
    private View contextMenuPopupView;
    private LinearLayout item_myuploads;
    private LinearLayout item_videowallpaper;
    private LinearLayout item_camerawallpaper;
    private LinearLayout item_schedulewallpaper;
    private LinearLayout item_shakeswitch;
    private LinearLayout item_about;
    private Switch sw_schedulewallpaper, sw_shakeswitch;

    private ImageView iv_voice;
    private Runnable mRunnable = null;

    private static WallpaperListActivity mWallpaperListActivity = null;
    public static WallpaperListActivity getInstance(){
        return mWallpaperListActivity;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        mContext = this;
        mWallpaperListActivity = this;
        mWallpaperItems = new ArrayList<>();
        setContentView(R.layout.wallpaper_list_layout);
        initView();
        loadWallpaperItems(getIntent());
    }

    @Override
    protected void onResume() {
        Log.i(TAG,"onResume");
        super.onResume();
        if(morePopupWindowHelper != null) {
            morePopupWindowHelper.dismiss();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG,"onNewIntent");
        super.onNewIntent(intent);
        loadWallpaperItems(intent);
    }

    public void loadWallpaperItems(Intent intent) {
        Log.i(TAG, "loadWallpaperItems-intent="+intent);
        ArrayList<String> hashCodeList = null;
        ArrayList<Integer> voteUpCntList = null;
        boolean isTagMatched = false;//搜索结果中，是否全部属于tag匹配到的
        boolean isTagPartialMatched = false;//搜索结果 全匹配或部分匹配
        Command cmd = null;
        if(intent != null) {
            cmd = (Command)intent.getSerializableExtra("command");
            if(cmd == null){
                Log.d(TAG, "cmd is null");
                isTagMatched = false;
                isTagPartialMatched = false;
            }else {
                Log.d(TAG, "cmd=" + cmd.toString());
                isTagMatched = cmd.getBooleanExtra("isTagMatched");
                isTagPartialMatched = cmd.getBooleanExtra("isTagPartialMatched");
                hashCodeList = cmd.getHashCodeList();
                voteUpCntList = cmd.getVoteUpCntList();
                mParams = cmd.getParams();
            }
        }
        Log.d(TAG, "isTagMatched="+isTagMatched);
        Log.d(TAG, "isTagPartialMatched="+isTagPartialMatched);
        String tag1 = null;
        String tag2 = null;
        String tag3 = null;
        if(mParams != null){
            tag1 = mParams.get("tag1");
            tag2 = mParams.get("tag2");
            tag3 = mParams.get("tag3");
        }

        String hintText = null;
        if(!TextUtils.isEmpty(tag1) &&
                ((!isTagMatched && !isTagPartialMatched) || hashCodeList==null || hashCodeList.size()==0)){//有关键字，但数据库中没匹配到相关壁纸
            hintText = getResources().getString(R.string.hint_no_matched);
            tv_title.setText(getResources().getString(R.string.search_result));
        }else if(!TextUtils.isEmpty(tag1)){//匹配到 用户说的 关键词
            hintText = String.format(getResources().getString(R.string.hint_tag_matched), tag1,tag2==null? "":tag2,tag3==null? "":tag3).replace("  ", " ");
            String tags = tag1 + " " + (tag2==null? "":tag2)  + " " +  (tag3==null? "":tag3);
            tags = tags.replace("  "," ").trim();
//            tv_title.setText(String.format(getResources().getString(R.string.tag_search_result),tags));
            AppContext.userTagList.add(tag1);
            if(!TextUtils.isEmpty(tag2)){
                AppContext.userTagList.add(tag2);
            }
            if(!TextUtils.isEmpty(tag3)){
                AppContext.userTagList.add(tag3);
            }

            searchbox.setText(tags);
        }else{//用户没有说 关键词
            if(cmd != null) {
                hintText = getResources().getString(R.string.hint_notag_matched);
            }
            tv_title.setText(getResources().getString(R.string.recommend_result));
        }
        showHint(hintText);

        if(cmd == null) {
            searchByKeywords(tag1, tag2, tag3);
        }else {
            loadWallpaperItems(hashCodeList, voteUpCntList);
        }
    }
    public void loadWallpaperItems(ArrayList<String> hashCodeList, ArrayList<Integer> voteUpCntList) {
        Log.i(TAG, "loadWallpaperItems-hashCodeList="+hashCodeList);

        if(hashCodeList == null || hashCodeList.size() ==0 || voteUpCntList == null || voteUpCntList.size() ==0
                || hashCodeList.size() != voteUpCntList.size()){
//            showEmptyView(true);
            loadPreloadWallpaper();
            return;
        }

        mWallpaperItems.clear();
        for(int i=0;i<hashCodeList.size();i++){
            String hashCode = hashCodeList.get(i);
            int voteUpCnt = voteUpCntList.get(i);
            WallpaperItem item = new WallpaperItem();
            item.setHashCode(hashCode);
            item.setVoteupCount(voteUpCnt);
            mWallpaperItems.add(item);
        }
        if(mWallpaperItems.size() == 0){
            showEmptyView(true);
            return;
        }

        showEmptyView(false);
        mGridAdapter.setWallpaperItems(mWallpaperItems);
        mWallpaperRecyclerView.scrollToPosition(0);
    }
    @Override
    protected void onPause() {
        Log.i(TAG,"onPause");
        super.onPause();
        if(morePopupWindowHelper != null) {
            morePopupWindowHelper.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG,"onDestroy");

        if(AppContext.userTagList != null &&  AppContext.userTagList.size() > 0) {
            SharedPreferences sp = AppContext.appContext.getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putStringSet("user_tag_set", AppContext.userTagList);
            editor.apply();
        }
        super.onDestroy();
//        if(ASRDialog.getASRDialogInstance() != null) {
//            ASRDialog.getASRDialogInstance().start();
//        }
        mWallpaperListActivity = null;
    }

    private void initView() {
        Log.i(TAG,"initView");
        Window window = this.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mDecorView = window.getDecorView();
        }

        tv_title = (TextView)findViewById(R.id.tv_title);
        searchbox = (SearchBox)findViewById(R.id.searchbox);
        tag_container = (TagContainer)findViewById(R.id.tag_container);
        ib_favorite = (ImageButton)findViewById(R.id.ib_favorite);
        ib_close = (ImageButton)findViewById(R.id.ib_close);
        tv_hint = (TextView)findViewById(R.id.tv_hint);
        tv_empty = (TextView)findViewById(R.id.tv_empty);
        mWallpaperRecyclerView = (WallpaperRecyclerView)findViewById(R.id.wallpaper_recycleview);
        ll_favoritelist = (LinearLayout)findViewById(R.id.ll_favoritelist);
        ll_nextbatch = (LinearLayout)findViewById(R.id.ll_nextbatch);
        ll_settings = (LinearLayout)findViewById(R.id.ll_settings);
        iv_voice = (ImageView)findViewById(R.id.iv_voice);

        ib_close.setOnClickListener(this);
        ib_favorite.setOnClickListener(this);
        ll_favoritelist.setOnClickListener(this);
        ll_nextbatch.setOnClickListener(this);
        ll_settings.setOnClickListener(this);
        iv_voice.setOnClickListener(this);

        mGridAdapter = new WallpaperGridAdapter(mContext, mWallpaperRecyclerView);
        mGridLayoutManager = new GridLayoutManager(mContext, 2);
        mWallpaperRecyclerView.setAdapter(mGridAdapter);
        mWallpaperRecyclerView.setLayoutManager(mGridLayoutManager);
        mWallpaperRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mGridAdapter.setCallBack(this);
        mRunnable = new Runnable() {
            @Override
            public void run() {
                final ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
                animator.setDuration(2000);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        float alpha = (float)valueAnimator.getAnimatedValue();
                        if(alpha < 0.05f){
                            iv_voice.setVisibility(View.INVISIBLE);
                            animator.cancel();
                        }else{
                            iv_voice.setAlpha(alpha);
                        }
                    }
                });
                animator.start();
            }
        };
        mWallpaperRecyclerView.setCallBack(new WallpaperRecyclerView.CallBack() {
            @Override
            public void onSwipe(boolean fromLtoR) {
                if(fromLtoR) {
                    WallpaperListActivity.this.finish();
                }else{
                    Intent intent = new Intent(mContext, FavoriteListActivity.class);
                    mContext.startActivity(intent);
                }
                tag_container.setVisibility(View.GONE);
                searchbox.clearFocus();
            }

            @Override
            public void onTouchUp() {
//                iv_voice.setAlpha(1.0f);
//                iv_voice.setVisibility(View.VISIBLE);
//                Handler handler = new Handler();
//                handler.removeCallbacks(mRunnable);
//                handler.postDelayed(mRunnable, 5000);

                tag_container.setVisibility(View.GONE);
                searchbox.clearFocus();

            }
        });
        mWallpaperRecyclerView.addItemDecoration(new SpaceItemDecoration(0));

        fl_wallpaper_preview = (FrameLayout)findViewById(R.id.fl_wallpaper_preview);
        mViewPager = (PhotoViewPager)findViewById(R.id.view_paper);
        ib_voteup_icon = (ImageButton)findViewById(R.id.ib_voteup_icon);
        tv_voteup_count = (TextView)findViewById(R.id.tv_voteup_count);
        tv_apply = (TextView)findViewById(R.id.tv_apply);
        ib_share = (ImageButton)findViewById(R.id.ib_share);
        tv_index = (TextView)findViewById(R.id.tv_index);

        ib_voteup_icon.setOnClickListener(this);
        tv_apply.setOnClickListener(this);
        ib_share.setOnClickListener(this);
        tv_voteup_count.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ib_voteup_icon.callOnClick();
            }
        });

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageSelected(int position) {
                updateWallpaperPreviewUI(position);
                View currentView = mViewPager.findViewWithTag(position);
                if(currentView != null && currentView instanceof DragPhotoView){
                    ((DragPhotoView)currentView).setScale(1);
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tag_container.setVisibility(View.GONE);
        searchbox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    String keywords = searchbox.getText().toString();
                    searchByKeywords(keywords, null, null);
                    tag_container.setVisibility(View.GONE);
                    searchbox.clearFocus();
                    return true;
                }
                return false;
            }
        });
        searchbox.setOnSearchIconClickListener(new SearchBox.OnSearchIconClickListener() {
            @Override
            public void onClick(View v) {
                String keywords = searchbox.getText().toString();
                searchByKeywords(keywords, null, null);
                tag_container.setVisibility(View.GONE);
                searchbox.clearFocus();
            }
        });

        moreMenuPopupView = LayoutInflater.from(this).inflate(R.layout.more_menu_popupview, null);
        morePopupWindowHelper = new PopupWindowHelper(moreMenuPopupView);
        item_myuploads = (LinearLayout)moreMenuPopupView.findViewById(R.id.item_myuploads);
        item_videowallpaper = (LinearLayout)moreMenuPopupView.findViewById(R.id.item_videowallpaper);
        item_camerawallpaper = (LinearLayout)moreMenuPopupView.findViewById(R.id.item_camerawallpaper);
        item_schedulewallpaper = (LinearLayout)moreMenuPopupView.findViewById(R.id.item_schedulewallpaper);
        item_shakeswitch = (LinearLayout)moreMenuPopupView.findViewById(R.id.item_shakeswitch);
        item_about = (LinearLayout)moreMenuPopupView.findViewById(R.id.item_about);
        sw_schedulewallpaper = (Switch)moreMenuPopupView.findViewById(R.id.sw_schedulewallpaper);
        sw_shakeswitch = (Switch)moreMenuPopupView.findViewById(R.id.sw_shakeswitch);

        item_myuploads.setOnClickListener(this);
        item_videowallpaper.setOnClickListener(this);
        item_camerawallpaper.setOnClickListener(this);
        item_schedulewallpaper.setOnClickListener(this);
        item_shakeswitch.setOnClickListener(this);
        item_about.setOnClickListener(this);

        updateSwitchState();
        loadTags();
    }

    private final static String[] preload_tags = new String[]{"美女","性感","跑车","星空","帅哥","简单","创意","搞笑","励志","风景","节日","卡通","S8","S9","Note8","曲面屏","跑马灯"};
    public void loadTags(){
        new AsyncTask<String, Void, ArrayList<String>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                tag_container.removeAllViews();
            }
            @Override
            protected ArrayList<String> doInBackground(String... params) {
                ArrayList<String> tagList = new ArrayList<>();
                if(AppContext.userTagList != null && AppContext.userTagList.size() > 0){
                    tagList.addAll(0, AppContext.userTagList);
                }

                ArrayList<String> hotKeywordsList = ApiClient.getHotKeywords(10);
                if(hotKeywordsList != null && hotKeywordsList.size() > 0){
                    for(String keywords:hotKeywordsList){
                        if(!tagList.contains(keywords)){
                            tagList.add(keywords);
                        }
                    }
                }

                if(tagList.size() < 15){
                    ArrayList<String> preload_tag_list = new ArrayList<>();
                    for(String tag : preload_tags) {
                        preload_tag_list.add(tag);
                    }
                    for(String tag: preload_tag_list){
                        if(!tagList.contains(tag)){
                            tagList.add(tag);
                        }
                    }
                }

                Collections.shuffle(tagList);
                return tagList;
            }

            @Override
            protected void onPostExecute(ArrayList<String> tagList) {
                super.onPostExecute(tagList);
                for(int i=0;i<tagList.size();i++){
                    if(!TextUtils.isEmpty(tagList.get(i))) {
                        TextView tagView = new TextView(mContext);
                        tagView.setText(tagList.get(i));
                        tagView.setBackgroundResource(R.drawable.tag_shape);
                        tagView.setOnClickListener(tagViewClickListener);
                        tag_container.addView(tagView);
                    }
                }
            }
        }.executeOnExecutor(THREAD_POOL_EXECUTOR);
    }

    View.OnClickListener tagViewClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            String tag = ((TextView)v).getText().toString();
            searchbox.setText(tag);
            tag_container.setVisibility(View.GONE);
            searchbox.clearFocus();
            searchByKeywords(tag, null, null);
        }
    };


    public void updateSwitchState(){
        SharedPreferences sp = AppContext.appContext.getSharedPreferences("smartwallpaper_setting", Context.MODE_PRIVATE);
        boolean enableChangeWallpaper = sp.getBoolean("enableScheduleChangeWallpaper", false);
        boolean enableShakeListen = sp.getBoolean("enableShakeListen", true);
        sw_schedulewallpaper.setChecked(enableChangeWallpaper);
        sw_shakeswitch.setChecked(enableShakeListen);
    }

    public void updateWallpaperPreviewUI(int position){
        if(position >= mWallpaperItems.size()){
            return;
        }
        WallpaperItem wallpaperItem = mWallpaperItems.get(position);
        if(wallpaperItem.hasVoteUp()) {
            ib_voteup_icon.setImageResource(R.drawable.vote_up_on);
        }else {
            ib_voteup_icon.setImageResource(R.drawable.vote_up_off);
        }
        if(wallpaperItem.isFavoriteOn()){
            ib_favorite.setImageResource(R.drawable.favorite_on);
        }else {
            ib_favorite.setImageResource(R.drawable.favorite_off);
        }
        int voteup_cnt = wallpaperItem.getVoteupCount();
        String voteup_text = null;
        if(voteup_cnt >= 0 && voteup_cnt < 10000) {
            voteup_text = wallpaperItem.getVoteupCount() + "赞";
        }else if(voteup_cnt >= 10000){
            voteup_text = String.format("%1$.1f", wallpaperItem.getVoteupCount()/10000.0f) + "万赞";
        }else{
            voteup_text = "0赞";
        }
        tv_voteup_count.setText(voteup_text);
        tv_index.setText(String.format("%d/%d", position+1, mWallpaperItems.size()));

        updateAlpha(1.0f);
    }

    public void updateAlpha(float alpha){
        tv_apply.setAlpha(alpha);
        ib_share.setAlpha(alpha);
        ib_voteup_icon.setAlpha(alpha);
        ib_favorite.setAlpha(alpha);
        tv_voteup_count.setAlpha(alpha);
        tv_index.setAlpha(alpha);
    }

    @Override
    public void onExitWallpaperPreview() {
        hideWallpaperPreview();
        updateAlpha(1.0f);
    }

    @Override
    public void onActionDown() {
        updateAlpha(1.0f);
    }

    @Override
    public void onActionMove(float translateY, float scale, int alpha) {
        updateAlpha(alpha/255.0f);
    }

    @Override
    public void onActionUp() {
        updateAlpha(1.0f);
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
//            mWallpaperRecyclerView.setVisibility(GONE);
            tv_empty.setVisibility(VISIBLE);
        }else{
//            mWallpaperRecyclerView.setVisibility(VISIBLE);
            tv_empty.setVisibility(GONE);
        }
    }
    @Override
    public void onClick(View v) {
        int id = v.getId();
        int pos;
        WallpaperItem wallpaperItem;
        SharedPreferences sp;
        SharedPreferences.Editor editor;
        Intent intent;
        switch (id){
            case R.id.ib_voteup_icon:
                pos = mViewPager.getCurrentItem();
                wallpaperItem = mWallpaperItems.get(pos);
                ImageButton voteUpView = ((ImageButton) v);
                if(wallpaperItem.hasVoteUp()) {
                    voteUpView.setImageResource(R.drawable.vote_up_off);
                    wallpaperItem.setVoteUpState(false);
                }else{
                    voteUpView.setImageResource(R.drawable.vote_up_on);
                    wallpaperItem.setVoteUpState(true);
                    Command cmd = new Command();
                    cmd.setRuleId(RuleId.RULE_ID_2);
                    cmd.setAction(ACTION_TOUCH_VOTEUP_WALLPAPER);
                    ArrayList<String> hashCodeList = new ArrayList<>();
                    hashCodeList.add(wallpaperItem.getHashCode());
                    cmd.setHashCodeList(hashCodeList);
                    CommandExecutor.getInstance(mContext).execute(cmd);

                    tv_voteup_count = ((RelativeLayout)v.getParent()).findViewById(R.id.tv_voteup_count);
                    wallpaperItem.voteUp();
                    tv_voteup_count.setText(wallpaperItem.getVoteupCount()+"赞");
                }
                break;
            case R.id.ib_favorite:
                pos = mViewPager.getCurrentItem();
                wallpaperItem = mWallpaperItems.get(pos);
                if(wallpaperItem.isFavoriteOn()){
                    ((ImageView) v).setImageResource(R.drawable.favorite_off);
                    wallpaperItem.setFavoriteOn(false);
                    CommandExecutor.getInstance(mContext).executeUnFavoriteTask(wallpaperItem.getHashCode());
                }else{
                    ((ImageView) v).setImageResource(R.drawable.favorite_on);
                    wallpaperItem.setFavoriteOn(true);
                    SmartWallpaperHelper.favoriteWallpaper(wallpaperItem.getWallpaperDrawable(), wallpaperItem.getHashCode());
                }
                break;
            case R.id.tv_apply:
                pos = mViewPager.getCurrentItem();
                wallpaperItem = mWallpaperItems.get(pos);
                CommandExecutor.getInstance(mContext).executeApplyWallpaperTask(wallpaperItem.getWallpaperDrawable(), wallpaperItem.getHashCode());
                break;
            case R.id.ib_share:
                pos = mViewPager.getCurrentItem();
                wallpaperItem = mWallpaperItems.get(pos);
                SmartWallpaperHelper.getInstance(mContext).shareWallpaper(wallpaperItem.getWallpaperDrawable());
                break;

            case R.id.iv_voice:
                intent = new Intent(this, ASRDialog.class);
                startActivity(intent);
                break;

            case R.id.ll_favoritelist:
                intent = new Intent(mContext, FavoriteListActivity.class);
                mContext.startActivity(intent);
                break;
            case R.id.ll_nextbatch:
                String tag1 = null;
                String tag2 = null;
                String tag3 = null;
                if(mParams != null && TextUtils.isEmpty(searchbox.getText())){
                    tag1 = mParams.get("tag1");
                    tag2 = mParams.get("tag2");
                    tag3 = mParams.get("tag3");
                }else{
                    tag1 = searchbox.getText().toString();
                }
                searchByKeywords(tag1, tag2, tag3);
                break;
            case R.id.ll_settings:
                updateSwitchState();
                morePopupWindowHelper.showAsPopUp(v, -20,40);
                break;
            case R.id.ib_close:
                morePopupWindowHelper.dismiss();
                finish();
                break;
            case R.id.item_myuploads:
                intent = new Intent(mContext, UploadListActivity.class);
                mContext.startActivity(intent);
                morePopupWindowHelper.dismiss();
                break;
            case R.id.item_videowallpaper:
                SmartWallpaperHelper.getInstance(mContext).setLiveWallpaper(VideoLiveWallpaper.class);
                morePopupWindowHelper.dismiss();
                break;
            case R.id.item_camerawallpaper:
                SmartWallpaperHelper.getInstance(mContext).setLiveWallpaper(CameraLiveWallpaper.class);
                morePopupWindowHelper.dismiss();
                break;
            case R.id.item_schedulewallpaper:
                sw_schedulewallpaper.setChecked(!sw_schedulewallpaper.isChecked());
                if(ChangeWallpaperService.useJobScheduler()){
                    if(sw_schedulewallpaper.isChecked()) {
                        ChangeWallpaperService.startScheduleJob(false);
                    }else{
                        ChangeWallpaperService.stopScheduleJob(false);
                    }
                }else {
                    intent = new Intent(this, ChangeWallpaperService.class);
                    if (sw_schedulewallpaper.isChecked()) {
                        //启动切换壁纸
                        intent.setAction(Action.ACTION_ENABLE_SCHEDULE_CHANGE_WALLPAPER);
                    } else {
                        intent.setAction(Action.ACTION_DISABLE_SCHEDULE_CHANGE_WALLPAPER);
                    }
                    startService(intent);
                }
                break;
            case R.id.item_shakeswitch:
                sw_shakeswitch.setChecked(!sw_shakeswitch.isChecked());

                if(ChangeWallpaperService.useJobScheduler()){
                    ChangeWallpaperService.enableShakeListen(sw_shakeswitch.isChecked(), false);
                }else {
                    intent = new Intent(this, ChangeWallpaperService.class);
                    if (sw_shakeswitch.isChecked()) {
                        intent.setAction(Action.ACTION_ENABLE_SHAKE_LISTEN);
                    } else {
                        intent.setAction(Action.ACTION_DISABLE_SHAKE_LISTEN);
                    }
                    intent.putExtra("first_time", false);
                    startService(intent);
                }
                break;
            case R.id.item_about:
                intent = new Intent(this, AppInfoActivity.class);
                startActivity(intent);
                morePopupWindowHelper.dismiss();
                break;
        }
    }

    private void searchByKeywords(String tag1, String tag2, String tag3){
        Log.i(TAG, "searchByKeywords-tag1="+tag1+",tag2="+tag2+",tag3="+tag3);
        new AsyncTask<String, Void, ArrayList<Object>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                ll_nextbatch.setEnabled(false);
            }
            @Override
            protected ArrayList<Object> doInBackground(String... params) {
                String tag1 = params[0];
                String tag2 = params[1];
                String tag3 = params[2];
                ArrayList<String> exclude_hashcode_list = null;
                if(mWallpaperItems != null && mWallpaperItems.size() > 0){
                    exclude_hashcode_list = new ArrayList<>();
                    for(WallpaperItem item:mWallpaperItems){
                        exclude_hashcode_list.add(item.getHashCode());//本批次已显示过的hashcode
                    }
                }
                return ApiClient.searchWallpaperByKeywords(tag1, tag2, tag3, exclude_hashcode_list);
            }

            @Override
            protected void onPostExecute(ArrayList<Object> lst) {
                super.onPostExecute(lst);
                ll_nextbatch.setEnabled(true);
                if (lst != null && lst.size() ==2 ) {
                    loadWallpaperItems((ArrayList<String>)lst.get(0), (ArrayList<Integer>)lst.get(1));
                } else {
                    loadWallpaperItems(null, null);
//                    Toast.makeText(AppContext.appContext, "hashcodeList:null", Toast.LENGTH_SHORT).show();
                }
            }
        }.executeOnExecutor(THREAD_POOL_EXECUTOR, tag1, tag2, tag3);
    }

    public static String WALLPAPER_PRELOAD_PATH = "wallpaper/preload";
    private void loadPreloadWallpaper(){
        Log.i(TAG, "loadPreloadWallpaper");
        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mWallpaperItems.clear();
            }

            @Override
            protected Boolean doInBackground(String... params) {
                String[] fileNames = null;
                try {
                    fileNames = mContext.getResources().getAssets().list(WALLPAPER_PRELOAD_PATH);
                    if (fileNames.length > 0) {
                        for (String fileName : fileNames) {
                            Log.i(TAG, "loadPreloadWallpaper-fileName="+fileName);
                            WallpaperItem item = new WallpaperItem();
                            item.setWallpaperAssertPath(WALLPAPER_PRELOAD_PATH + File.separator + fileName);
                            item.setHashCode(fileName.substring(0, fileName.indexOf(".")));
                            item.setVoteupCount(0);
                            mWallpaperItems.add(item);
                        }
                        Collections.shuffle(mWallpaperItems);
                        int removeCnt = mWallpaperItems.size() - 10;
                        for(int i=0;i<removeCnt;i++){
                            mWallpaperItems.remove(i);
                        }
                        return true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if(mWallpaperItems.size() == 0){
                    showEmptyView(true);
                    return;
                }

                showEmptyView(false);
                mGridAdapter.setWallpaperItems(mWallpaperItems);
                mWallpaperRecyclerView.scrollToPosition(0);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    @Override
    public void onItemVoteUp(int position) {
        Log.i(TAG, "onItemVoteUp");

    }

    @Override
    public void onItemFavorite(int position) {
        Log.i(TAG, "onItemFavorite");
    }

    @Override
    public void onItemApply(int position) {
        Log.i(TAG, "onItemApply");
    }

    @Override
    public void onItemClick(int position) {
        Log.i(TAG, "onItemClick");
        showWallpaperPreview(position);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (searchbox.hasFocus()) {
                searchbox.clearFocus();
//                tag_container.setVisibility(View.GONE);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        boolean consumed = super.onTouchEvent(event);
//        if(!consumed) {
//            switch (event.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    touchDownY = event.getY();
//                    latestY = event.getY();
//                    break;
//                case MotionEvent.ACTION_MOVE:
//                    moveTo(event.getY() - touchDownY);
//                    break;
//                case MotionEvent.ACTION_UP:
//                case MotionEvent.ACTION_CANCEL:
//                default:
//                    if(mDecorView.getTranslationY() >= mDecorView.getHeight()/8) {
//                        startHideAnimation();
//                    }else{
//                        startShowAnimation();
//                    }
//                    break;
//            }
//        }
        return consumed;
    }

    private void moveTo(float y){
        if(y < 0){
            y = 0;
        }else if(y > mDecorView.getHeight()/2) {
            y = mDecorView.getHeight()/2;
        }
        mDecorView.setTranslationY(y);
    }

    @Override
    public void onBackPressed() {
        if(fl_wallpaper_preview.getVisibility() == View.VISIBLE){
            hideWallpaperPreview();
            return;
        }

        if(searchbox.hasFocus()){
            searchbox.clearFocus();
            tag_container.setVisibility(View.GONE);
            return;
        }

        super.onBackPressed();
    }

    public void showWallpaperPreview(int pos) {
        Log.i(TAG, "showWallpaperPreview");
        mPhotoViewPagerAdapter = new PhotoViewPagerAdapter(mContext);
        mPhotoViewPagerAdapter.setWallpaperItems(mWallpaperItems);
        mPhotoViewPagerAdapter.setCallBack(this);
        mPhotoViewPagerAdapter.setDragPhotoViewCallBack(this);
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
        Log.i(TAG, "hideWallpaperPreview");
        fl_wallpaper_preview.setVisibility(View.GONE);
        mGridAdapter.notifyDataSetChanged();
    }

    private void showHint(String hintText){
        Log.d(TAG, "showHint-hintText="+hintText);
        if(TextUtils.isEmpty(hintText)){
            return;
        }
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


    private void startShowAnimation(){
        Log.d(TAG, "startShowAnimation");
        TranslateAnimation showTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, mDecorView.getTranslationY(),
                Animation.RELATIVE_TO_SELF,0.0f);
        showTranslateAnimation.setDuration(200);
        showTranslateAnimation.setFillAfter(false);
        showTranslateAnimation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                mDecorView.setVisibility(VISIBLE);
                mDecorView.setTranslationY(0);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mDecorView.startAnimation(showTranslateAnimation);
    }

    private void startHideAnimation(){
        Log.d(TAG, "startHideAnimation");
        TranslateAnimation hideTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, mDecorView.getTranslationY(),
                Animation.RELATIVE_TO_SELF, mDecorView.getHeight());
        hideTranslateAnimation.setDuration(200);
        hideTranslateAnimation.setFillAfter(false);
        hideTranslateAnimation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                mDecorView.setTranslationY(mDecorView.getHeight());
                WallpaperListActivity.this.finish();
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mDecorView.startAnimation(hideTranslateAnimation);
    }
}
