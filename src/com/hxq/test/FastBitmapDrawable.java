package com.hxq.test;

import android.graphics.*;
import android.graphics.drawable.Drawable;

public class FastBitmapDrawable extends Drawable {
	private Bitmap mBitmap;
    private int mAlpha;
    private int mWidth;
    private int mHeight;
    private Paint mPaint = new Paint();
	

    public FastBitmapDrawable(Bitmap b) {
        mAlpha = 225;
        mBitmap = b;
        if(mBitmap != null) {
            mWidth = b.getWidth();
            mHeight = b.getHeight();
        } else {
            mWidth = mHeight = 0;
        }
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public int getMinimumHeight() {
        return mHeight;
    }

    @Override
    public int getMinimumWidth() {
        return mWidth;
    }

    @Override
	public void draw(Canvas arg0) {
        Rect r = getBounds();
        arg0.drawBitmap(mBitmap, r.left, r.top, mPaint);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int arg0) {
		mAlpha = arg0;
        mPaint.setAlpha(mAlpha);
	}

	@Override
	public void setColorFilter(ColorFilter arg0) {
		mPaint.setColorFilter(arg0);
	}

    public void setBitmap(Bitmap b) {
        mBitmap = b;
        if(mBitmap != null) {
            mWidth = b.getWidth();
            mHeight = b.getHeight();
        }
        else {
            mWidth = mHeight = 0;
        }
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

}
