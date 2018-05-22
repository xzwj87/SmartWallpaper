package com.samsung.app.smartwallpaper.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import com.samsung.app.smartwallpaper.R;

public class TagContainer extends ViewGroup{
    private static final String TAG = "TagContainer";

    private Context mContext;

    public TagContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }
    protected void init() {
    }
    @Override
    public void onViewAdded(View child) {
        Log.i(TAG, "onViewAdded");
        if(child instanceof TextView) {
            TextView tagView = (TextView)child;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.weight = LayoutParams.WRAP_CONTENT;
            lp.height = LayoutParams.WRAP_CONTENT;
            tagView.setLayoutParams(lp);

            tagView.setTextSize(22.0f);
            tagView.setPadding(24, 0, 24, 12);
            tagView.setSingleLine(true);
//            tagView.setMaxLines(2);
            tagView.setTextDirection(View.TEXT_DIRECTION_LTR);
            tagView.setEllipsize(TextUtils.TruncateAt.END);
            tagView.setGravity(Gravity.CENTER_VERTICAL);
            tagView.setBackgroundResource(R.drawable.bt_shape);
        }
        super.onViewAdded(child);
    }
    protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec) {
//        // 计算出所有的childView的宽和高
////        if(getChildCount() > 0) {
////            TextView tagView = (TextView) getChildAt(0);
////            String tag = tagView.getText().toString();
////            Rect rect = new Rect();
////            mTagPaint.getTextBounds(tag, 0, tag.length(), rect);
////            setMeasuredDimension(widthMeasureSpec, MeasureSpec.makeMeasureSpec(rect.height() * 3 + 20, MeasureSpec.EXACTLY));
////        }else {
            measureChildren(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension( getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                    getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
////        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int child_cnt = this.getChildCount();
        Log.i(TAG, "onLayout-child_cnt="+child_cnt);
        if(child_cnt == 0){
            return;
        }
        int parentWidth = right - left;
        int parentHeight = bottom - top;
        Log.i(TAG, "parentWidth="+parentWidth);
        Log.i(TAG, "parentHeight="+parentHeight);
        int startX = getPaddingLeft();
        int startY = getPaddingTop();
        int margin = 30;
        int line = 1;
        Rect tagRect = new Rect();
        for(int i=0;i<child_cnt;i++) {
            TextView tagView = (TextView)getChildAt(i);
            Log.i(TAG, "tagView.getMeasuredWidth()="+tagView.getMeasuredWidth());
            int w = tagView.getMeasuredWidth();
            int h = tagView.getMeasuredHeight();

            tagRect.left = startX;
            tagRect.top = startY;
            tagRect.right = tagRect.left + w;
            tagRect.bottom = tagRect.top + h;
            //判断是否需要换行
            if(tagRect.right > parentWidth - margin - getPaddingRight()){
                line++;
                Log.i(TAG, "line="+line);
                if(line > 3){//最大只允许3行
                    break;
                }

                startX = getPaddingLeft();
                startY = tagRect.bottom + margin;
                tagRect.left = startX;
                tagRect.top = startY;
                tagRect.right = tagRect.left + w;
                tagRect.bottom = tagRect.top + h;
            }
            Log.i(TAG, "tagRect="+tagRect.toString());
            tagView.layout(tagRect.left, tagRect.top, tagRect.right, tagRect.bottom);
            startX = tagRect.right + margin;
        }
    }
}
