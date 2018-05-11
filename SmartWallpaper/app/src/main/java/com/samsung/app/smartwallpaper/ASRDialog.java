package com.samsung.app.smartwallpaper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.baidu.speech.asr.SpeechConstant;
import com.samsung.app.smartwallpaper.broadcastreceiver.MyBroadcastReceiver;
import com.samsung.app.smartwallpaper.control.MyRecognizer;
import com.samsung.app.smartwallpaper.network.ApiClient;
import com.samsung.app.smartwallpaper.recognization.ChainRecogListener;
import com.samsung.app.smartwallpaper.recognization.CommonRecogParams;
import com.samsung.app.smartwallpaper.recognization.IRecogListener;
import com.samsung.app.smartwallpaper.recognization.MessageStatusRecogListener;
import com.samsung.app.smartwallpaper.recognization.RecogResult;
import com.samsung.app.smartwallpaper.recognization.offline.OfflineRecogParams;
import com.samsung.app.smartwallpaper.recognization.online.OnlineRecogParams;
import com.samsung.app.smartwallpaper.utils.PermisionUtil;
import com.samsung.app.smartwallpaper.utils.ShortcutHelper;
import com.samsung.app.smartwallpaper.view.ASRProgressBar;
import com.samsung.app.smartwallpaper.view.VoiceWaveAnimationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static android.view.View.VISIBLE;
import static com.samsung.app.smartwallpaper.recognization.IStatus.STATUS_NONE;

/**
 * Created by ASUS on 2017/12/21.
 */

public class ASRDialog  extends Activity {
    private final static String TAG = "ASRDialog";
    private static MyBroadcastReceiver mMyBroadcastReceiver = null;

    protected MyRecognizer myRecognizer;
    protected CommonRecogParams apiParams;
    private ChainRecogListener listener;
    Map<String, Object> params;
    protected Handler handler;
    protected boolean enableOffline = false;
    protected boolean running = false;
    protected static final int ERROR_NONE = 0;
    private static final int ERROR_NETWORK_UNUSABLE = 0x90000;
    private static final int ENGINE_TYPE_ONLINE = 0;
    private static final int ENGINE_TYPE_OFFLINE = 1;
    private CharSequence mErrorRes = "";

    public static final int STATUS_None = 0;
    public static final int STATUS_WaitingReady = 2;
    public static final int STATUS_Ready = 3;
    public static final int STATUS_Speaking = 4;
    public static final int STATUS_Recognition = 5;
    protected int status = STATUS_None;

    private Bundle mParams = new Bundle();

    private Context mContext = null;
    private View mContentRoot, mDecorView;
    private View mMainLayout;
    private View mErrorLayout;
    private TextView mTipsTextView;
    private TextView mWaitNetTextView;
    private TextView mFinishTextView;
    private TextView mCancelTextView;
    private TextView mRetryTextView;
    private VoiceWaveAnimationView mVoiceWaveView;
    private ASRProgressBar mASRProgressBar;
    private TextView mErrorTipsTextView;
    private TextView mLogoText1;
    private TextView mLogoText2;
    private ImageButton mCancelBtn;

    private TextView mSuggestionTips;
    private TextView mSuggestionTips2;
    private View mRecognizingView;
    private EditText mInputEdit;

    private ImageButton main_btn;
    private ImageButton user_manual_btn;


    private Drawable mBg;
    private StateListDrawable mButtonBg = new StateListDrawable();//说完了
    private StateListDrawable mLeftButtonBg = new StateListDrawable();
    private StateListDrawable mRightButtonBg = new StateListDrawable();
    private StateListDrawable mHelpButtonBg = new StateListDrawable();

    private ColorStateList mButtonColor;
    private ColorStateList mButtonReverseColor;
    private int mTheme = 0;


    private final int BAR_ONEND = 0;
    private final int BAR_ONFINISH = 1;
    private int step = 0;
    private int delayTime = 0;// 3秒不出识别结果，显示网络不稳定,15秒转到重试界面
    private volatile int mEngineType = 0;// 当前活跃的引擎类型
    Message mMessage = Message.obtain();
    private int mErrorCode;

    private Handler mHandler = new Handler();
    private String mPrefix;

    private static ASRDialog mASRDialog;
    public static ASRDialog getASRDialogInstance(){
        return mASRDialog;
    }

    float touchDownY, latestY;

    protected void handleMsg(Message msg) {
    }
    protected CommonRecogParams getApiParams() {
        return new OnlineRecogParams(this);
    }

    public Bundle getParams() {
        return mParams;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mASRDialog = this;
        // setStrictMode();
        handler = new Handler() {
            /*
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleMsg(msg);
            }

        };
        initPermission();

        initRecog();
        initView();
        startRecognition();
    }
    protected void initRecog() {
        Log.i(TAG, "initRecog");
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mParams.putAll(extras);
        }

        listener = new ChainRecogListener();
        // DigitalDialogInput 输入 ，MessageStatusRecogListener可替换为用户自己业务逻辑的listener
        listener.addListener(new MessageStatusRecogListener(handler));
        listener.addListener(new DialogListener());
        myRecognizer = new MyRecognizer(this, listener); // DigitalDialogInput 输入
        apiParams = getApiParams();
        status = STATUS_NONE;
        if (enableOffline) {
            myRecognizer.loadOfflineEngine(OfflineRecogParams.fetchOfflineParams());
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        params = apiParams.fetch(sp);  // params可以手动填入
    }
    private void initResources() {

        // 配色方案选择
        Integer buttonRecognizingBgName;
        final Integer buttonNormalBgName = getResources().getIdentifier("btn_normal", "drawable", getPackageName());
        final Integer buttonPressedBgName = getResources().getIdentifier("btn_pressed", "drawable", getPackageName());
        Integer leftButtonNormalBgName = null;
        Integer leftButtonPressedBgName = null;
        final Integer rightButtonNormalBgName = getResources().getIdentifier("right_normal", "drawable", getPackageName());
        final Integer rightButtonPressedBgName = getResources().getIdentifier("right_pressed", "drawable", getPackageName());
        Integer bgName = null;


        // 按下、不可用、其它状态颜色
        int[] colors = new int[3];
        // 按下、不可用、其它状态颜色
        int[] colors_reverse = new int[3];
        bgName = getResources().getIdentifier("digital_bg", "drawable", getPackageName());
        leftButtonNormalBgName = getResources().getIdentifier("left_normal", "drawable", getPackageName());
        leftButtonPressedBgName = getResources().getIdentifier("left_pressed", "drawable", getPackageName());

        buttonRecognizingBgName = getResources().getIdentifier("btn_recognizing", "drawable", getPackageName());

        colors[0] = 0xff474747;
        colors[1] = 0xffe8e8e8;
        colors[2] = 0xff474747;
        colors_reverse[0] = 0xffffffff;
        colors_reverse[1] = 0xffbebebe;
        colors_reverse[2] = 0xffffffff;

        mHelpButtonBg.addState(new int[]{
                android.R.attr.state_pressed
        }, getResources().getDrawable(getResources().getIdentifier("help_pressed_light", "drawable", getPackageName())));
        mHelpButtonBg.addState(new int[]{},
                getResources().getDrawable(getResources().getIdentifier("help_light", "drawable", getPackageName())));

        mBg = getResources().getDrawable(bgName);
        mButtonBg.addState(new int[]{
                android.R.attr.state_pressed, android.R.attr.state_enabled
        }, getResources().getDrawable(buttonPressedBgName));
        mButtonBg.addState(new int[]{
                -android.R.attr.state_enabled
        }, getResources().getDrawable(buttonRecognizingBgName));
        mButtonBg.addState(new int[]{},
                getResources().getDrawable(buttonNormalBgName));
        mLeftButtonBg.addState(new int[]{
                android.R.attr.state_pressed
        }, getResources().getDrawable(leftButtonPressedBgName));
        mLeftButtonBg.addState(new int[]{},
                getResources().getDrawable(leftButtonNormalBgName));
        mRightButtonBg.addState(new int[]{
                android.R.attr.state_pressed
        }, getResources().getDrawable(rightButtonPressedBgName));
        mRightButtonBg.addState(new int[]{},
                getResources().getDrawable(rightButtonNormalBgName));
        int[][] states = new int[3][];
        states[0] = new int[]{
                android.R.attr.state_pressed, android.R.attr.state_enabled
        };
        states[1] = new int[]{
                -android.R.attr.state_enabled
        };
        states[2] = new int[1];

        mButtonColor = new ColorStateList(states, colors);
        mButtonReverseColor = new ColorStateList(states, colors_reverse);

    }
    private void initView() {
        initResources();

        mContentRoot = LayoutInflater.from(mContext).inflate(R.layout.asr_dialog_layout,null);
        Window window = this.getWindow();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (window != null) { //设置dialog的布局样式 让其位于底部

            window.setGravity(Gravity.BOTTOM);
            WindowManager.LayoutParams lp = window.getAttributes();
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setAttributes(lp);
            window.setContentView(mContentRoot);

            ViewGroup.LayoutParams layoutParams = mContentRoot.getLayoutParams();
            layoutParams.width = this.getResources().getDisplayMetrics().widthPixels;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;//(int)(this.getResources().getDisplayMetrics().heightPixels * 0.4f);
            mContentRoot.setLayoutParams(layoutParams);

            mDecorView = window.getDecorView();
        }
        mMainLayout = mContentRoot.findViewById(R.id.main_reflect);
        mErrorLayout = mContentRoot.findViewById(R.id.error_reflect);

        mTipsTextView = (TextView) mContentRoot.findViewById(R.id.tips_text);
        mWaitNetTextView = (TextView) mContentRoot.findViewById(R.id.tips_wait_net);
        mWaitNetTextView.setVisibility(View.INVISIBLE);
        mLogoText1 = (TextView) mContentRoot.findViewById(R.id.logo_1);
        mLogoText2 = (TextView) mContentRoot.findViewById(R.id.logo_2);

        mSuggestionTips = (TextView) mContentRoot.findViewById(R.id.suggestion_tips);
        mSuggestionTips2 = (TextView) mContentRoot.findViewById(R.id.suggestion_tips_2);

        // 进度条
        mASRProgressBar = (ASRProgressBar) mContentRoot.findViewById(R.id.progress);
        mASRProgressBar.setVisibility(View.INVISIBLE);
        mASRProgressBar.setTheme(mTheme);
        mFinishTextView = (TextView) mContentRoot.findViewById(R.id.speak_finish);
        mFinishTextView.setOnClickListener(mClickListener);
        mFinishTextView.setBackgroundDrawable(mButtonBg);
        mFinishTextView.setTextColor(mButtonReverseColor);

        mCancelTextView = (TextView) mContentRoot.findViewById(R.id.cancel_text_btn);
        mCancelTextView.setOnClickListener(mClickListener);
        mCancelTextView.setBackgroundDrawable(mLeftButtonBg);
        mCancelTextView.setTextColor(mButtonColor);
        mRetryTextView = (TextView) mContentRoot.findViewById(R.id.retry_text_btn);
        mRetryTextView.setOnClickListener(mClickListener);

        mRetryTextView.setBackgroundDrawable(mRightButtonBg);
        mRetryTextView.setTextColor(mButtonReverseColor);

        mErrorTipsTextView = (TextView) mContentRoot.findViewById(R.id.error_tips);
        mCancelBtn = (ImageButton) mContentRoot.findViewById(R.id.cancel_btn);
        mCancelBtn.setOnClickListener(mClickListener);

        mVoiceWaveView = (VoiceWaveAnimationView) mContentRoot.findViewById(R.id.voicewave_view);
        mVoiceWaveView.setThemeStyle(mTheme);

        mVoiceWaveView.setVisibility(View.INVISIBLE);
        mRecognizingView = mContentRoot.findViewById(R.id.recognizing_reflect);

        mInputEdit = (EditText) mContentRoot.findViewById(R.id.partial_text);
//        mInputEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if(actionId == EditorInfo.IME_ACTION_DONE){
//                    onEndOfSpeech();
//                    return true;
//                }
//                return false;
//            }
//        });
//        mInputEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if(hasFocus) {
//                    cancleRecognition();
//                }else{
//                    onEndOfSpeech();
//                }
//            }
//        });
        Bitmap changeWallpaperIcon = BitmapFactory.decodeResource(getResources(), R.drawable.asr_mic);
        ShortcutHelper.addShortCut(ASRDialog.this, "换一张", changeWallpaperIcon);

        Bitmap shareIcon = BitmapFactory.decodeResource(getResources(), R.drawable.asr_mic);
        ShortcutHelper.addShortCut(ASRDialog.this, "分享", shareIcon);

        Bitmap restoreIcon = BitmapFactory.decodeResource(getResources(), R.drawable.asr_mic);
        ShortcutHelper.addShortCut(ASRDialog.this, "还原", restoreIcon);

        Bitmap voteUpIcon = BitmapFactory.decodeResource(getResources(), R.drawable.asr_mic);
        ShortcutHelper.addShortCut(ASRDialog.this, "点赞", voteUpIcon);

        Bitmap favoriteListIcon = BitmapFactory.decodeResource(getResources(), R.drawable.asr_mic);
        ShortcutHelper.addShortCut(ASRDialog.this, "查看收藏夹", favoriteListIcon);

        Bitmap selfDefineIcon = BitmapFactory.decodeResource(getResources(), R.drawable.asr_mic);
        ShortcutHelper.addShortCut(ASRDialog.this, "拍摄壁纸", selfDefineIcon);


        main_btn = (ImageButton) mContentRoot.findViewById(R.id.main_btn);
        user_manual_btn = (ImageButton) mContentRoot.findViewById(R.id.user_manual_btn);
        main_btn.setOnClickListener(mClickListener);
        user_manual_btn.setOnClickListener(mClickListener);
    }
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        registerHomeKeyReceiver(this);
    }
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        unregisterHomeKeyReceiver(this);
        super.onPause();

//        myRecognizer.release();
        running= false;
    }
    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestory");
        myRecognizer.release();
        running = false;
        super.onDestroy();
        mASRDialog = null;
    }

    private void startRecognition() {
        Log.i(TAG, "startRecognition");
        running = true;
        onRecognitionStart();
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, true);
        params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 2000);//长语音，说完后等待3s，如果没说了，就结束ASR，否则继续
        myRecognizer.start(params);
    }
    public void start() {
        Log.i(TAG, "start");
        step = 0;
        delayTime = 0;
        mInputEdit.setVisibility(View.GONE);
        mASRProgressBar.setVisibility(View.INVISIBLE);
        startRecognition();
    }
    private void stop() {
        myRecognizer.stop();
    }
    private void cancel() {
        myRecognizer.cancel();
    }
    protected void speakFinish() {
        myRecognizer.stop();
    }
    protected void cancleRecognition() {
        myRecognizer.cancel();
        status = STATUS_None;
    }

    private static void registerHomeKeyReceiver(Context context) {
        Log.i(TAG, "registerHomeKeyReceiver");
        mMyBroadcastReceiver = new MyBroadcastReceiver();
        final IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        context.registerReceiver(mMyBroadcastReceiver, homeFilter);
    }
    private static void unregisterHomeKeyReceiver(Context context) {
        Log.i(TAG, "unregisterHomeKeyReceiver");
        if (null != mMyBroadcastReceiver) {
            context.unregisterReceiver(mMyBroadcastReceiver);
        }
    }
    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        Log.i(TAG, "initPermission");
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.SET_WALLPAPER,
                Manifest.permission.CAMERA,
                Manifest.permission.RECEIVE_BOOT_COMPLETED
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm :permissions){
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.

            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()){
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }

    private void setStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());

    }

    public void onCommandFinish(boolean handled){
        if(!handled) {
            cancleRecognition();
            onFinish(SpeechRecognizer.ERROR_NO_MATCH, 0);
        }else {
//            cancleRecognition();
//            onFinish(0, 0);
            //finish();
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            switch (id){
                case R.id.speak_finish:
                    if (mFinishTextView.isEnabled()) {
                        if (status == STATUS_Speaking) {
                            speakFinish();
                            onEndOfSpeech();// TODO
                        } else {
                            cancleRecognition();
                            onFinish(SpeechRecognizer.ERROR_NO_MATCH, 0);
                        }
                    }
                    break;
                case R.id.cancel_btn:
                case R.id.cancel_text_btn:
                    myRecognizer.release();
                    finish();
                    break;
                case R.id.retry_text_btn:
                    step = 0;

                    // 3秒不出识别结果，显示网络不稳定,15秒转到重试界面
                    delayTime = 0;
                    mInputEdit.setVisibility(View.GONE);
                    mASRProgressBar.setVisibility(View.INVISIBLE);

                    startRecognition();
                    break;
                case R.id.main_btn:
                    ApiClient.requestTS("任意换一张壁纸");
                    if(ASRDialog.getASRDialogInstance() != null){
                        ASRDialog.getASRDialogInstance().finish();
                    }
                    break;
                case R.id.user_manual_btn:

                    break;
            }
        }
    };

    protected void onRecognitionStart() {
        Log.i(TAG, "onRecognitionStart");
        barHandler.removeMessages(BAR_ONFINISH);
        barHandler.removeMessages(BAR_ONEND);

        step = 0;

        // 3秒不出识别结果，显示网络不稳定,15秒转到重试界面
        delayTime = 0;
        mInputEdit.setText("");
        mInputEdit.setVisibility(View.INVISIBLE);
        mVoiceWaveView.setVisibility(VISIBLE);
        mVoiceWaveView.startInitializingAnimation();
        mTipsTextView.setText("稍等一会儿");
        mErrorLayout.setVisibility(View.INVISIBLE);
        mMainLayout.setVisibility(VISIBLE);
        mFinishTextView.setText("麦克风正在初始化...");
        mFinishTextView.setEnabled(false);

        // mInputEdit.setVisibility(View.GONE);
        mTipsTextView.setVisibility(VISIBLE);
        mASRProgressBar.setVisibility(View.INVISIBLE);
        mWaitNetTextView.setVisibility(View.INVISIBLE);

        mRecognizingView.setVisibility(VISIBLE);
        mLogoText1.setVisibility(VISIBLE);
    }

    protected void onPrepared() {
        Log.i(TAG, "onPrepared");
        mVoiceWaveView.startPreparingAnimation();
        mTipsTextView.setText("启动识别");

        mFinishTextView.setText("结束");
        mFinishTextView.setEnabled(true);

        main_btn.setEnabled(true);
    }

    protected void onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech");
        mTipsTextView.setText("监听中...");
        mVoiceWaveView.startRecordingAnimation();
    }

    protected void onVolumeChanged(float volume) {
        mVoiceWaveView.setCurrentDBLevelMeter(volume);
    }

    protected void onEndOfSpeech() {
        Log.i(TAG, "onEndOfSpeech");
        mTipsTextView.setText("识别中...");
        mFinishTextView.setText("识别中..");
        mFinishTextView.setEnabled(false);

        main_btn.setEnabled(false);

        mASRProgressBar.setVisibility(VISIBLE);
        barHandler.sendEmptyMessage(BAR_ONEND);

        startRecognizingAnimation();
    }
    private void stopRecognizingAnimation() {
        mVoiceWaveView.resetAnimation();
    }

    private void startRecognizingAnimation() {
        mVoiceWaveView.startRecognizingAnimation();
    }
    protected void onFinish(int errorType, int errorCode) {
        Log.i(TAG, "onFinish-"+String.format("onError:errorType %1$d,errorCode %2$d ", errorType, errorCode));
        mErrorCode = errorType;

        barHandler.removeMessages(BAR_ONEND);
        barHandler.sendEmptyMessage(BAR_ONFINISH);
        mWaitNetTextView.setVisibility(View.INVISIBLE);
        stopRecognizingAnimation();
        if (errorType != ERROR_NONE) {

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, String.format("onError:errorType %1$d,errorCode %2$d ", errorType,
                        errorCode));
            }
            barHandler.removeMessages(BAR_ONFINISH);
            mSuggestionTips2.setVisibility(View.GONE);
            switch (errorType) {
                case SpeechRecognizer.ERROR_NO_MATCH:
                    mErrorRes = "没有匹配的识别结果";
                    break;
                case SpeechRecognizer.ERROR_AUDIO:
                    mErrorRes = "启动录音失败";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    mErrorRes = "听不太清楚，请大声点.";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    SpannableString spanString = new SpannableString("网络超时，再试一次");
                    URLSpan span = new URLSpan("#") {
                        @Override
                        public void onClick(View widget) {
                            startRecognition();
                        }
                    };
                    spanString.setSpan(span, 5, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    mErrorRes = spanString;
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    if (errorCode == ERROR_NETWORK_UNUSABLE) {
                        mErrorRes = "网络不可用，请检查网络.";
                    } else {
                        mErrorRes = "网络异常，请重试.";
                    }
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    mErrorRes = "客户端错误"; // TODO
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    mErrorRes = "权限不足，请检查设置";// TODO
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    mErrorRes = "引擎忙"; // TODO
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    mErrorRes = "服务器异常，请重试.";
                    break;
                default:
                    mErrorRes = "没有获取语音，请重试.";
                    break;
            }
            mCancelTextView.setText("取消");
            mWaitNetTextView.setVisibility(View.INVISIBLE);
            mErrorTipsTextView.setMovementMethod(LinkMovementMethod.getInstance());
            mErrorTipsTextView.setText(mErrorRes);
            mErrorLayout.setVisibility(VISIBLE);
            mMainLayout.setVisibility(View.INVISIBLE);

            main_btn.setEnabled(true);
        }
        mVoiceWaveView.setVisibility(View.INVISIBLE);
    }

    protected void onPartialResults(String[] results) {
        Log.i(TAG, "onPartialResults");
        if (results != null) {
            if (results != null && results.length > 0) {
                if (mInputEdit.getVisibility() != VISIBLE) {
                    mInputEdit.setVisibility(VISIBLE);
                    mWaitNetTextView.setVisibility(View.INVISIBLE);
                    mTipsTextView.setVisibility(View.INVISIBLE);
                }
                mInputEdit.setText(results[0]);
                mInputEdit.setSelection(mInputEdit.getText().length());
                delayTime = 0;
            }
        }
    }
    protected void onFinalResults(String[] results) {
        Log.i(TAG, "onFinalResults");
        if (results != null) {
            if (results != null && results.length > 0) {
                if (mInputEdit.getVisibility() != VISIBLE) {
                    mInputEdit.setVisibility(VISIBLE);
                    mWaitNetTextView.setVisibility(View.INVISIBLE);
                    mTipsTextView.setVisibility(View.INVISIBLE);
                }
                mInputEdit.setText(results[0]);
                mInputEdit.setSelection(mInputEdit.getText().length());
                delayTime = 0;
            }
        }
    }

    Handler barHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BAR_ONEND) {
                if (delayTime >= 3000) {
                    if (mInputEdit.getVisibility() == VISIBLE) {
                        mInputEdit.setVisibility(View.INVISIBLE);
                    }
                    mTipsTextView.setVisibility(View.INVISIBLE);
                    // 仅在线时显示“网络不稳定”
                    if (mEngineType == ENGINE_TYPE_ONLINE) {
                        mWaitNetTextView.setText("网络不稳定");
                        mWaitNetTextView.setVisibility(VISIBLE);
                    }
                } else {
                    if (mInputEdit.getVisibility() == VISIBLE) {
                        mTipsTextView.setVisibility(View.INVISIBLE);
                        mWaitNetTextView.setVisibility(View.INVISIBLE);
                    } else {
                        mTipsTextView.setVisibility(VISIBLE);
                        mWaitNetTextView.setVisibility(View.INVISIBLE);
                    }
                }
                mMessage.what = BAR_ONEND;
                if (step <= 30) {
                    delayTime = delayTime + 10;
                    step = step + 1;
                    barHandler.sendEmptyMessageDelayed(BAR_ONEND, 10);
                } else if (step < 60) {
                    delayTime = delayTime + 100;
                    step = step + 1;
                    barHandler.sendEmptyMessageDelayed(BAR_ONEND, 100);
                } else {
                    if (delayTime >= 15000) {
                        cancleRecognition();
                        onFinish(SpeechRecognizer.ERROR_NETWORK, ERROR_NETWORK_UNUSABLE);
                        step = 0;
                        delayTime = 0;
                        mASRProgressBar.setVisibility(View.INVISIBLE);
                        barHandler.removeMessages(BAR_ONEND);
                    } else {
                        step = 60;
                        delayTime = delayTime + 100;
                        barHandler.sendEmptyMessageDelayed(BAR_ONEND, 100);
                    }
                }
                mASRProgressBar.setProgress(step);
            } else if (msg.what == BAR_ONFINISH) {
                if (step <= 80) {
                    step = step + 3;
                    barHandler.sendEmptyMessageDelayed(BAR_ONFINISH, 1);
                } else {
                    step = 0;
                    delayTime = 0;
                    mInputEdit.setVisibility(View.GONE);
                    mASRProgressBar.setVisibility(View.INVISIBLE);
                    if (mErrorCode == ERROR_NONE) {
                        //finish();
                    }
                    barHandler.removeMessages(BAR_ONFINISH);
                }
                mASRProgressBar.setProgress(step);
            }
        }
    };

    protected class DialogListener implements IRecogListener {

        @Override
        public void onAsrReady() {//引擎准备完毕，启动识别
            Log.i(TAG, "onAsrReady");
            status = STATUS_Ready;
            onPrepared();
        }
        @Override
        public void onAsrBegin() {//onAsrReady后检查到用户开始说话, 监听中...
            Log.i(TAG, "onAsrBegin");
            status = STATUS_Speaking;
            onBeginningOfSpeech();
        }
        @Override
        public void onAsrPartialResult(String[] results, RecogResult recogResult) {//onAsrBegin 后 随着用户的说话，返回的临时结果
            Log.i(TAG, "onAsrPartialResult");
            onPartialResults(results);
        }
        @Override
        public void onAsrFinalResult(String[] results, RecogResult recogResult) {//最终的识别结果
            Log.i(TAG, "onAsrFinalResult");
            onFinalResults(results);
            status = STATUS_None;
            running = false;

            //onPartialResults(results);
            onFinish(0, 0);// TODO

            Intent intentResult = new Intent();
            ArrayList list = new ArrayList();
            list.addAll(Arrays.asList(results));
            intentResult.putStringArrayListExtra("results", list);
            setResult(RESULT_OK, intentResult);
            ApiClient.requestTS(results[0]);
//            finish();
        }
        @Override
        public void onAsrEnd() {//检查到用户开始说话停止，或者ASR_STOP 输入事件调用后，识别中...
            Log.i(TAG, "onAsrEnd");
            status = STATUS_Recognition;
            onEndOfSpeech();
        }

        @Override
        public void onAsrFinish(RecogResult recogResult) {
            Log.i(TAG, "onAsrFinish");
            running = false;
            //finish();
        }

        @Override
        public void onAsrFinishError(int errorCode, int subErrorCode, String errorMessage, String descMessage,RecogResult recogResult) {
            Log.i(TAG, "onAsrFinishError");
            onFinish(errorCode, subErrorCode);
        }

        @Override
        public void onAsrLongFinish() {//长语音识别结束
            Log.i(TAG, "onAsrLongFinish");
            running = false;
            //finish();
        }

        @Override
        public void onAsrVolume(int volumePercent, int volume) {
            Log.i(TAG, "onAsrVolume");
            onVolumeChanged(volumePercent);
        }

        @Override
        public void onAsrAudio(byte[] data, int offset, int length) {
            Log.i(TAG, "onAsrAudio");
        }

        @Override
        public void onAsrExit() {
            Log.i(TAG, "onAsrExit");
        }

        @Override
        public void onAsrOnlineNluResult(String nluResult) {
            Log.i(TAG, "onAsrOnlineNluResult");
        }

        @Override
        public void onOfflineLoaded() {
            Log.i(TAG, "onOfflineLoaded");
        }

        @Override
        public void onOfflineUnLoaded() {
            Log.i(TAG, "onOfflineUnLoaded");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
                    if(mContentRoot.getY() >= mContentRoot.getHeight()/8) {
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
        }else if(y > mContentRoot.getHeight()/2) {
            y = mContentRoot.getHeight()/2;
        }
        mContentRoot.setY(y);
    }
    private void startShowAnimation(){
        Log.d(TAG, "startShowAnimation");
        TranslateAnimation showTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.ABSOLUTE, mContentRoot.getY(),
                Animation.ABSOLUTE,0.0f);
        showTranslateAnimation.setDuration(200);
        showTranslateAnimation.setFillAfter(false);
        showTranslateAnimation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                mContentRoot.setVisibility(VISIBLE);
                mContentRoot.setY(0);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mContentRoot.startAnimation(showTranslateAnimation);
    }
    private void startHideAnimation(){
        Log.d(TAG, "startHideAnimation");
        TranslateAnimation hideTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.ABSOLUTE, mContentRoot.getY(),
                Animation.ABSOLUTE, mContentRoot.getHeight());
        hideTranslateAnimation.setDuration(200);
        hideTranslateAnimation.setFillAfter(false);
        hideTranslateAnimation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                mContentRoot.setY(mContentRoot.getHeight());
                ASRDialog.this.finish();
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mContentRoot.startAnimation(hideTranslateAnimation);
    }
}
