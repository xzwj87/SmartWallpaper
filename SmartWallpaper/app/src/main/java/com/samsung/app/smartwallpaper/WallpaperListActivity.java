package com.samsung.app.smartwallpaper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.app.smartwallpaper.command.Command;
import com.samsung.app.smartwallpaper.command.CommandExecutor;
import com.samsung.app.smartwallpaper.command.RuleId;
import com.samsung.app.smartwallpaper.config.UrlConstant;
import com.samsung.app.smartwallpaper.model.PhotoViewPagerAdapter;
import com.samsung.app.smartwallpaper.model.WallpaperGridAdapter;
import com.samsung.app.smartwallpaper.model.WallpaperItem;
import com.samsung.app.smartwallpaper.network.ApiClient;
import com.samsung.app.smartwallpaper.view.PhotoViewPager;
import com.samsung.app.smartwallpaper.view.WallpaperRecyclerView;
import com.samsung.app.smartwallpaper.wallpaper.SmartWallpaperHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.samsung.app.smartwallpaper.command.Action.ACTION_FAVORITE_WALLPAPER;
import static com.samsung.app.smartwallpaper.command.Action.ACTION_TOUCH_VOTEUP_WALLPAPER;

/**
 * Created by ASUS on 2018/4/22.
 */

public class WallpaperListActivity extends Activity implements View.OnClickListener,
        WallpaperGridAdapter.CallBack, PhotoViewPagerAdapter.CallBack{
    private final String TAG = "WallpaperListActivity";
    private Context mContext;
    private View mDecorView;

    float touchDownY, latestY;
    int deltaY;

    private TextView tv_title;
    private ImageButton ib_myfavoritelist;
    private ImageButton ib_close;
    private TextView tv_hint;
    private TextView tv_empty;
    private WallpaperRecyclerView mWallpaperRecyclerView;
    private TextView  tv_next_batch;

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
    private ImageButton ib_favorite;
    private TextView tv_apply;
    private ImageButton ib_share;
    private TextView tv_index;

    private static WallpaperListActivity mWallpaperListActivity = null;
    public static WallpaperListActivity getInstance(){
        return mWallpaperListActivity;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mWallpaperListActivity = this;
        mWallpaperItems = new ArrayList<>();
        setContentView(R.layout.wallpaper_list_layout);
        initView();
        loadWallpaperItems(getIntent());
    }
    public void loadWallpaperItems(Intent intent) {
        Log.i(TAG, "loadWallpaperItems-intent="+intent);
        ArrayList<String> hashCodeList = null;
        ArrayList<Integer> voteUpCntList = null;
        boolean isTagMatched = false;//搜索结果中，是否全部属于tag匹配到的
        boolean isTagPartialMatched = false;//搜索结果 全匹配或部分匹配
        if(intent != null) {
            Command cmd = (Command)intent.getSerializableExtra("command");
            Log.d(TAG, "cmd="+cmd.toString());
            isTagMatched = cmd.getBooleanExtra("isTagMatched");
            isTagPartialMatched = cmd.getBooleanExtra("isTagPartialMatched");
            hashCodeList = cmd.getHashCodeList();
            voteUpCntList = cmd.getVoteUpCntList();
            mParams = cmd.getParams();
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
            tv_title.setText(String.format(getResources().getString(R.string.tag_search_result),tags));
        }else{//用户没有说 关键词
            hintText = getResources().getString(R.string.hint_notag_matched);
            tv_title.setText(getResources().getString(R.string.recommend_result));
        }
        showHint(hintText);

        loadWallpaperItems(hashCodeList, voteUpCntList);
    }
    public void loadWallpaperItems(ArrayList<String> hashCodeList, ArrayList<Integer> voteUpCntList) {
        Log.i(TAG, "loadWallpaperItems-hashCodeList="+hashCodeList);

        if(hashCodeList == null || hashCodeList.size() ==0 || voteUpCntList == null || voteUpCntList.size() ==0
                || hashCodeList.size() != voteUpCntList.size()){
            return;
        }

        mWallpaperItems.clear();
        for(int i=0;i<hashCodeList.size();i++){
            String hashCode = hashCodeList.get(i);
            int voteUpCnt = voteUpCntList.get(i);
            WallpaperItem item = new WallpaperItem(hashCode);
            item.setHashCode(hashCode);
            item.setVoteupCount(voteUpCnt);
            mWallpaperItems.add(item);
        }
        if(mWallpaperItems.size() == 0){
            return;
        }

        showEmptyView(false);
        mGridAdapter.setWallpaperItems(mWallpaperItems);
        mWallpaperRecyclerView.scrollToPosition(0);
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(ASRDialog.getASRDialogInstance() != null) {
            ASRDialog.getASRDialogInstance().start();
        }
        mWallpaperListActivity = null;
    }

    private void initView() {
        Window window = this.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mDecorView = window.getDecorView();
        }

        tv_title = (TextView)findViewById(R.id.tv_title);
        ib_myfavoritelist = (ImageButton)findViewById(R.id.ib_myfavoritelist);
        ib_close = (ImageButton)findViewById(R.id.ib_close);
        tv_hint = (TextView)findViewById(R.id.tv_hint);
        tv_empty = (TextView)findViewById(R.id.tv_empty);
        mWallpaperRecyclerView = (WallpaperRecyclerView)findViewById(R.id.wallpaper_recycleview);
        tv_next_batch = (TextView)findViewById(R.id.tv_next_batch);

        ib_close.setOnClickListener(this);
        ib_myfavoritelist.setOnClickListener(this);
        tv_next_batch.setOnClickListener(this);

        mGridAdapter = new WallpaperGridAdapter(mContext, mWallpaperRecyclerView);
        mGridLayoutManager = new GridLayoutManager(mContext, 2);
        mWallpaperRecyclerView.setAdapter(mGridAdapter);
        mWallpaperRecyclerView.setLayoutManager(mGridLayoutManager);
        mWallpaperRecyclerView.setItemAnimator(new DefaultItemAnimator());
        showEmptyView(mGridAdapter.getItemCount() == 0);

        mGridAdapter.setCallBack(this);
        mWallpaperRecyclerView.setCallBack(new WallpaperRecyclerView.CallBack() {
            @Override
            public void close() {
                WallpaperListActivity.this.finish();
            }
        });
        mWallpaperRecyclerView.addItemDecoration(new SpaceItemDecoration(0));

        fl_wallpaper_preview = (FrameLayout)findViewById(R.id.fl_wallpaper_preview);
        mViewPager = (PhotoViewPager)findViewById(R.id.view_paper);
        ib_voteup_icon = (ImageButton)findViewById(R.id.ib_voteup_icon);
        tv_voteup_count = (TextView)findViewById(R.id.tv_voteup_count);
        ib_favorite = (ImageButton)findViewById(R.id.ib_favorite);
        tv_apply = (TextView)findViewById(R.id.tv_apply);
        ib_share = (ImageButton)findViewById(R.id.ib_share);
        tv_index = (TextView)findViewById(R.id.tv_index);

        ib_voteup_icon.setOnClickListener(this);
        ib_favorite.setOnClickListener(this);
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

    public void updateWallpaperPreviewUI(int position){
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
        tv_voteup_count.setText(wallpaperItem.getVoteupCount() + "赞");
        tv_index.setText(String.format("%d/%d", position+1, mWallpaperItems.size()));
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
        int pos;
        WallpaperItem wallpaperItem;
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
                CommandExecutor.getInstance(mContext).executeApplyWallpaperTask(wallpaperItem.getWallpaperDrawable());
                break;
            case R.id.ib_share:
                pos = mViewPager.getCurrentItem();
                wallpaperItem = mWallpaperItems.get(pos);
                SmartWallpaperHelper.getInstance(mContext).shareWallpaper(wallpaperItem.getWallpaperDrawable());
                break;
            case R.id.ib_myfavoritelist:
                Intent intent = new Intent(mContext, FavoriteListActivity.class);
                mContext.startActivity(intent);
                break;
            case R.id.ib_close:
                finish();
                break;
            case R.id.tv_next_batch:
                String tag1 = null;
                String tag2 = null;
                String tag3 = null;
                if(mParams != null){
                    tag1 = mParams.get("tag1");
                    tag2 = mParams.get("tag2");
                    tag3 = mParams.get("tag3");
                }
                new AsyncTask<String, Void, ArrayList<Object>>() {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        tv_next_batch.setEnabled(false);
                    }

                    @Override
                    protected ArrayList<Object> doInBackground(String... params) {
                        String tag1 = params[0];
                        String tag2 = params[1];
                        String tag3 = params[2];

                        ArrayList<String> tagList = new ArrayList<>();
                        if (!TextUtils.isEmpty(tag1)) {
                            tagList.add(tag1);
                        }
                        if (!TextUtils.isEmpty(tag2)) {
                            tagList.add(tag2);
                        }
                        if (!TextUtils.isEmpty(tag3)) {
                            tagList.add(tag3);
                        }

                        HashMap<String, Object> paramMap = new HashMap<>();
                        paramMap.put("current_hashcode", "0");
                        paramMap.put("tag_list", tagList);
                        paramMap.put("top_count", 10);

                        JSONObject jsonObject = ApiClient.request_post(UrlConstant.SEARCH_WALLPAPER_URL, paramMap);
                        try {
                            int resultcode = jsonObject.getInt("response_code");
                            if (resultcode == 200) {
                                ArrayList<String> hashcodeList = new ArrayList<>();
                                ArrayList<Integer> voteupcntList = new ArrayList<>();
                                JSONArray hashcodeArray = jsonObject.getJSONArray("result");
                                for (int i = 0; i < hashcodeArray.length(); i++) {
                                    hashcodeList.add(hashcodeArray.getString(i));
                                }
                                JSONArray voteupcntArray = jsonObject.getJSONArray("voteupcnt_list");
                                for (int i = 0; i < voteupcntArray.length(); i++) {
                                    voteupcntList.add(voteupcntArray.getInt(i));
                                }
                                ArrayList<Object> lst = new ArrayList<>();
                                lst.add(hashcodeList);
                                lst.add(voteupcntList);
                                return lst;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "error=" + e.toString());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(ArrayList<Object> lst) {
                        super.onPostExecute(lst);
                        tv_next_batch.setEnabled(true);
                        if (lst != null && lst.size() ==2 ) {
                            loadWallpaperItems((ArrayList<String>)lst.get(0), (ArrayList<Integer>)lst.get(1));
                        } else {
                            Toast.makeText(AppContext.appContext, "hashcodeList:null", Toast.LENGTH_SHORT).show();
                        }
                    }
                }.executeOnExecutor(THREAD_POOL_EXECUTOR, tag1, tag2, tag3);
                break;
        }
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
        Log.i(TAG, "hideWallpaperPreview");
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
