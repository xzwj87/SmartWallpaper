package com.samsung.app.smartwallpaper.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.samsung.app.smartwallpaper.R;


/**
 * Created by samsung on 2018/5/14.
 * Author: wangmingyuan
 */
public class SearchBox extends EditText implements View.OnFocusChangeListener, TextWatcher {

    private Drawable mSearchIconDrawable;
    private OnSearchIconClickListener mSearchIconClickListener = null;

    public SearchBox(Context context) {
        this(context, null);
    }

    public SearchBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SearchBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        clearFocus();
        mSearchIconDrawable = getCompoundDrawables()[2];
        setOnFocusChangeListener(this);
        addTextChangedListener(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean isTouchOnIcon = false;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (getCompoundDrawables()[2] != null) {
                int start = getWidth() - getTotalPaddingRight() + getPaddingRight(); // 起始位置
                int end = getWidth(); // 结束位置
                isTouchOnIcon = (event.getX() > start) && (event.getX() < end);
                if (isTouchOnIcon) {
                    if(mSearchIconClickListener != null){
                        mSearchIconClickListener.onClick(this);
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(hasFocus){
            setCursorVisible(true);
            v.getRootView().findViewById(R.id.tag_container).setVisibility(View.VISIBLE);
        }else{
            setCursorVisible(false);
//            v.getRootView().findViewById(R.id.tag_container).setVisibility(View.GONE);
            InputMethodManager im = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    public void setOnSearchIconClickListener(OnSearchIconClickListener listener){
        mSearchIconClickListener = listener;
    }
    public interface OnSearchIconClickListener {
        void onClick(View v);
    }
}
