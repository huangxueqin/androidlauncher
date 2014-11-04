package com.hxq.test;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class PageIndicator extends ViewGroup {
	public static final String TAG = "PageIndicator TAG";
	private static final int frontDotResId = R.drawable.frontdot;
	private static final int backDotResId = R.drawable.backdot;
	
	private int dotSpacing;
	private int frontDotSize;
	private int backDotSize;
	private Drawable frontDotDrawable;
	private Drawable backDotDrawable;
	private int pageNum;
    private int currentScreen;
    private ImageView[] dots;
    
    public PagedView.PageSwitchListener scrollListener = new PagedView.PageSwitchListener() {
		@Override
		public void onPageSwitch(int newPageIndex) {
			dots[currentScreen].setImageDrawable(backDotDrawable);
			dots[newPageIndex].setImageDrawable(frontDotDrawable);
			currentScreen = newPageIndex;
			if(frontDotSize != backDotSize) 
				requestLayout();
		}
		@Override
		public void onPageNumChanged(int newPageNum) {
			int oldPageNum = pageNum;
			pageNum = newPageNum;
			if(oldPageNum < pageNum) {
				ImageView[] oldDots = dots;
				dots = new ImageView[pageNum];
				for(int i=0;i<oldPageNum;i++) {
					dots[i] = oldDots[i];
				}
				for(int i=oldPageNum;i<pageNum;i++) {
					dots[i] = new ImageView(getContext());
		    		if(i == currentScreen) {
		    			dots[i].setImageDrawable(frontDotDrawable);
		    		}
		    		else {
		    			dots[i].setImageDrawable(backDotDrawable);
		    		}
		    		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
		    				ViewGroup.LayoutParams.WRAP_CONTENT);
		    		PageIndicator.this.addView(dots[i], lp);
				}
			}
			requestLayout();
		}
	};

    public PageIndicator(Context context, AttributeSet attrs, int pageNum) {
        super(context, attrs);
        Log.d(TAG, "pageIndicator construction running");
        initIndicator(pageNum);
    }

    public PageIndicator(Context context, int pageNum) {
        super(context);
        initIndicator(pageNum);
    }

    private void initIndicator(int numOfPage) {
    	this.pageNum = numOfPage;
    	Log.d(TAG, "pageNum = " + pageNum);
    	this.currentScreen = Workspace.DEFAULT_PAGE;
    	dots = new ImageView[pageNum];
    	Resources res = getContext().getResources();
    	frontDotDrawable = res.getDrawable(R.drawable.frontdot);
    	frontDotSize = frontDotDrawable.getIntrinsicWidth();
    	backDotDrawable = res.getDrawable(backDotResId);
    	dotSpacing = backDotSize = backDotDrawable.getIntrinsicWidth();
    	for(int i=0;i<pageNum;i++) {
    		dots[i] = new ImageView(getContext());
    		if(i == currentScreen) {
    			dots[i].setImageDrawable(frontDotDrawable);
    		}
    		else {
    			dots[i].setImageDrawable(backDotDrawable);
    		}
    		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
    				ViewGroup.LayoutParams.WRAP_CONTENT);
    		this.addView(dots[i], lp);
    	}
    }
    
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		if(widthMeasureMode == MeasureSpec.AT_MOST) {
			widthSize = getPaddingLeft() + (pageNum-1) * (backDotSize + dotSpacing) + frontDotSize + getPaddingRight();
		}
		if(heightMeasureMode == MeasureSpec.AT_MOST) {
			heightSize = getPaddingTop() + Math.max(frontDotSize, backDotSize) + getPaddingBottom();
		}
		int count = getChildCount();
		for(int i=0;i<count;i++) {
			int widthSpec = MeasureSpec.makeMeasureSpec(backDotSize, MeasureSpec.EXACTLY);
			int heightSpec = MeasureSpec.makeMeasureSpec(backDotSize, MeasureSpec.EXACTLY);
			if(i==currentScreen) {
				widthSpec = MeasureSpec.makeMeasureSpec(frontDotSize, MeasureSpec.EXACTLY);
				heightSpec = MeasureSpec.makeMeasureSpec(frontDotSize, MeasureSpec.EXACTLY);
			}
			getChildAt(i).measure(widthSpec, heightSpec);
		}
		this.setMeasuredDimension(widthSize, heightSize);
	}


	@Override
	protected void onLayout(boolean arg0, int l, int t, int r, int b) {
		int count = getChildCount();
		int i = 0;
		int widthOffset = 0;
		while(i < currentScreen) {
			View v = getChildAt(i);
			int left = getPaddingLeft() + widthOffset;
			int top = getPaddingTop();
			v.layout(left, top, left + backDotSize, top + backDotSize);
			widthOffset += backDotSize + dotSpacing;
			i++;
		}
		View front = getChildAt(i);
		front.layout(getPaddingLeft() + widthOffset, getPaddingTop(), getPaddingLeft() + widthOffset + frontDotSize, getPaddingTop() + frontDotSize);
		widthOffset += frontDotSize + dotSpacing;
		i++;
		while(i < count) {
			View v = getChildAt(i);
			int left = getPaddingLeft() + widthOffset;
			int top = getPaddingTop();
			v.layout(left, top, left + backDotSize, top + backDotSize);
			widthOffset += backDotSize + dotSpacing;
			i++;
		}
	}
    
}
