package com.hxq.test;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.TextView;

public class BubbleTextView extends TextView implements IDesktopPageItem{

	public static final String TAG = "BubbleTextView TAG";
	private int intrWidth;
	private int intrHeight;
	
	private static final int TOP_INDEX = 1;
	private Drawable mDrawable;
	private int mTextHeight;
	private int mDrawablePadding;
	private int mPaddingTop;
	private int mPaddingBottom;
	private int mPaddingLeft;
	private int mPaddingRight;
	
	private int mCellHeight;
	private int mCellWidth;
	
    public BubbleTextView(Context context) {
        super(context);
    }

    public BubbleTextView(Context context, AttributeSet attr) {
        super(context, attr);
    }

    public BubbleTextView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
    }
    
    private void darkenDrawable() {
    	mDrawable.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
    	setCompoundDrawablesWithIntrinsicBounds(null,
                this.mDrawable,
                null, null);
    }
    
    private void unDarkenDrawable() {
    	mDrawable.clearColorFilter();
    	setCompoundDrawablesWithIntrinsicBounds(null,
                this.mDrawable,
                null, null);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
//    	Log.d(TAG, "OnTouchEvent running");
        boolean result = super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            	Log.d(TAG, "onTouchEvent Action_Down");
            	darkenDrawable();
                break;
            case MotionEvent.ACTION_MOVE:
            	
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
//            	Log.d(TAG, "OnTouchEvent ACTION_CANCEL or ACTION_UP");
            	unDarkenDrawable();
                break;
        }
//        Log.d(TAG, "onTouchEvent result = " + result);
        return result;
    }
    
    public static IDesktopPageItem getIconViewFromResource(LayoutInflater inflater, int resId, ViewGroup parent,
    		ShortcutInfo shortcut, IconCache cache) {
    	IDesktopPageItem icon = (IDesktopPageItem) inflater.inflate(resId, parent);
    	icon.applyFromShortcutInfo(shortcut, cache);
    	return icon;
    }

    /* 
     * the following methods are all inherit from IDesktopPageIcon interface 
     */
    
    @Override
    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache) {
        Bitmap b = info.getIcon(iconCache);
        this.mDrawable = new FastBitmapDrawable(b);
        setCompoundDrawablesWithIntrinsicBounds(null,
                this.mDrawable,
                null, null);
        String title = info.getTitle();
        setText(title);
        calIconDimension(title, b);
        setTag(info);
    }
    
    private void calIconDimension(String title, Bitmap b) {
    	
    	Resources res = this.getResources();
        mTextHeight = res.getDimensionPixelSize(R.dimen.middle_app_text_size);
        mDrawablePadding = res.getDimensionPixelSize(R.dimen.text_drawable_padding);
        mPaddingTop = res.getDimensionPixelSize(R.dimen.app_icon_padding_top);
        mPaddingBottom = res.getDimensionPixelSize(R.dimen.app_icon_padding_bottom);
        mPaddingLeft = res.getDimensionPixelSize(R.dimen.app_icon_padding_left);
        mPaddingRight = res.getDimensionPixelSize(R.dimen.app_icon_padding_right);
        mCellHeight = res.getDimensionPixelSize(R.dimen.cell_height);
        mCellWidth = res.getDimensionPixelSize(R.dimen.cell_width);
        
    	TextPaint paint = this.getPaint();
    	int textlen = (int)paint.measureText(title);
    	int bitmapWidth = b.getWidth();
    	int bitmapHeight = b.getHeight();
    	if(textlen < bitmapWidth) {
    		textlen = bitmapWidth;
    	}
    	this.intrWidth = textlen + mPaddingLeft + mPaddingRight;
    	this.intrHeight = bitmapHeight + mDrawablePadding + mTextHeight + mPaddingTop + mPaddingBottom;
    	if(intrWidth > mCellWidth) {
    		intrWidth = mCellWidth;
    	}
    	if(intrHeight > mCellHeight) {
    		intrHeight = mCellHeight;
    	}
    	Log.d(TAG, "intrWidth = " + intrWidth + ", intrHeight = " + intrHeight);
    	Log.d(TAG, "cellWidth = " + mCellWidth + ", cellHeight = " + mCellHeight);
    }
    
	@Override
	public int getIconWidth() {
		return intrWidth;
	}

	@Override
	public int getIconHeight() {
		return intrHeight;
	}

	@Override
	public View getView() {
		return this;
	}

	@Override
	public void setIconVisibility(int visibility) {
		this.setVisibility(visibility);
	}

	@Override
	public Drawable getIconDrawable() {
		return this.getCompoundDrawables()[TOP_INDEX];
	}

	@Override
	public ItemInfo getCorrespondItemInfo() {
		if(getTag() != null)
			return (ItemInfo)getTag();
		else
			return null;
	}
	
    
}
