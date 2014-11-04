package com.hxq.test;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

public class DragView extends View {
	public static String TAG = "DragView Tag";
	public static int OFFSETX = 0;
	public static int OFFSETY = 30;
	public static int ANIME_DURATION = 100;
	public static float ENLARGE_SCALE = 1.2f;
	
	private Bitmap mBitmap;
	private Matrix mMatrix;
	public int mRegistrationX;
	public int mRegistrationY;
	private int mOffsetX = 0;
	private int mOffsetY = 0;
	
	private DragLayer mDragLayer;
	private DragLayer.LayoutParams mLayoutParams;
	private Paint mPaint;
	
	private ValueAnimator mAnime;
	
	public DragView(Context context, DragLayer dragLayer, Bitmap source, int height, int width, int registrationX, int registrationY) {
		super(context);
		mDragLayer = dragLayer;
		mBitmap = source;
		mMatrix = new Matrix();
		mRegistrationX = registrationX;
		mRegistrationY = registrationY;
		mPaint = new Paint();
		initAnime();
	}
	
	private void initAnime() {
		mAnime = ValueAnimator.ofFloat(0.0f, 1.0f);
		mAnime.setDuration(ANIME_DURATION);
		mAnime.setInterpolator(new DecelerateInterpolator(2.0f));
		mAnime.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator arg0) {
				float value = (Float)arg0.getAnimatedValue();
				int deltaX = (int)(value * OFFSETX - mOffsetX);
				int deltaY = (int)(value * OFFSETY - mOffsetY);
				mOffsetX += deltaX;
				mOffsetY += deltaY;
				if(getParent() != null) {
					ViewGroup parent = (ViewGroup)getParent();
					mLayoutParams.x -= deltaX;
					mLayoutParams.y -= deltaY;
					parent.requestLayout();
				}
			}
			
		});
	}
	
	public void show(int touchX, int touchY) {
		mLayoutParams = new DragLayer.LayoutParams(mBitmap.getWidth(), mBitmap.getHeight(),
				touchX - mRegistrationX, touchY - mRegistrationY);
		mLayoutParams.setCustomLayout(true);
		this.setLayoutParams(mLayoutParams);
		mDragLayer.addView(this);
		startAnime();
	}
	
	public void startAnime() {
		if(mAnime != null) {
			mOffsetX = mOffsetY = 0;
			mAnime.start();
		}
	}
	
	public void startDropAnime(float fromX, float toX, float fromY, float toY, View showView) {
		Resources res = getResources();
		int paddingTop = res.getDimensionPixelSize(R.dimen.app_icon_padding_top);
		int paddintLeft = res.getDimensionPixelSize(R.dimen.app_icon_padding_left);
		
		DropAnimationListener listener = new DropAnimationListener(this, mDragLayer, showView);
		Animation dropAnime = new TranslateAnimation(fromX, toX + OFFSETX + paddintLeft, fromY, toY + OFFSETY + paddingTop);
		dropAnime.setDuration(200);
		dropAnime.setFillAfter(true);
		dropAnime.setAnimationListener(listener);
		this.startAnimation(dropAnime);
	}
	
	private class DropAnimationListener implements AnimationListener {

		private View showView;
		private DragLayer dragLayer;
		private View self;
		
		public DropAnimationListener(View self, DragLayer layer, View showView) {
			this.showView = showView;
			this.self = self;
			this.dragLayer = layer;
		}
		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			dragLayer.removeView(self);
			this.showView.setVisibility(View.VISIBLE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}
		
	}
	
	public void move(int touchX, int touchY) {
		DragLayer.LayoutParams lp = mLayoutParams;
		lp.x = touchX - mRegistrationX - mOffsetX;
		lp.y = touchY - mRegistrationY - mOffsetY;
		
		mDragLayer.requestLayout();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawBitmap(mBitmap, 0f, 0f, mPaint);
	}
	
}
