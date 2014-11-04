package com.hxq.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

public class DragLayer extends FrameLayout{
	
	public static String TAG = "DragLayer Tag";
	
	private Context mContext;
	private int[] mTmpXY = new int[2];
	Rect tmpRect = new Rect();
	
	public boolean mIsDragging = false;
	
	private int mMotionDownX;
	private int mMotionDownY;
	
	private DragView mDragView;
	private DropTarget.DragObject mDragObject = new DropTarget.DragObject();
	private DropTarget mLastDropTarget;
	private DropTarget mFirstDropTarget;
	
	public DragLayer(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		// Disable multitouch across the workspace/all apps/customize tray
        setMotionEventSplittingEnabled(false);
	}

	public void startDrag(Bitmap b, Rect viewRect, int[] touchXY, Object data) {
		mIsDragging = true;
		mDragObject.data = data;
		mDragObject.dragX = touchXY[0];
		mDragObject.dragY = touchXY[1];
		int registrationX = touchXY[0] - viewRect.left;
		int registrationY = touchXY[1] - viewRect.top;
		Log.d(TAG, "registrationX = " + registrationX + ", registrationY = " + registrationY);
		Log.d(TAG, "viewRect.left = " + viewRect.left + ", viewRect.top = " + viewRect.top);
		int viewWidth = viewRect.right - viewRect.left;
		int viewHeight = viewRect.bottom - viewRect.top;
		mDragView = new DragView(mContext, this, b, viewWidth, viewHeight, registrationX, registrationY);
		mDragObject.dragView = mDragView;
		mDragView.show(touchXY[0], touchXY[1]);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
//		DumpInfo.dumpActionInfo(TAG, ev);
		final int action = ev.getAction();
		int dragLayerX = (int) ev.getX();
		int dragLayerY = (int) ev.getY();
		switch(action) {
		case MotionEvent.ACTION_DOWN:
			mMotionDownX = (int) ev.getX();
			mMotionDownY = (int) ev.getY();
			mFirstDropTarget = mLastDropTarget = updateDropTargetByCoord(mMotionDownX, mMotionDownY, this);
			break;
		case MotionEvent.ACTION_MOVE:
			;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if(mIsDragging) {
				dropTarget(dragLayerX, dragLayerY);
			}
			break;
		}
		return mIsDragging;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
//		Log.d(TAG, "onTouchEvent running");
//		DumpInfo.dumpActionInfo(TAG, event);
		if(mIsDragging) {
			int dragLayerX = (int) event.getX();
			int dragLayerY = (int) event.getY();
			switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mMotionDownX = dragLayerX;
				mMotionDownY = dragLayerY;
				break;
			case MotionEvent.ACTION_MOVE:
				handleMoveEvent(dragLayerX, dragLayerY);
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				dropTarget(dragLayerX, dragLayerY);
				break;
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	private void handleMoveEvent(int x, int y) {
//		Log.d(TAG, "x = " + x + ", y = " + y);
		mDragObject.dragX = x;
		mDragObject.dragY = y;
		mDragView.move(x, y);
		DropTarget oldTarget = mLastDropTarget;
		mLastDropTarget = updateDropTargetByCoord(x, y, this);
		if(mLastDropTarget != oldTarget) {
			if(oldTarget != null) {
				oldTarget.onDragOut(mDragObject);
			}
			if(mLastDropTarget != null) {
				mLastDropTarget.onDragEnter(mDragObject);
			}
		}
		if(mLastDropTarget != null) {
			mLastDropTarget.onDragOver(mDragObject);
			if(mLastDropTarget.isNearEdge(mDragObject.dragX)) {
				mLastDropTarget.onDragNearEdge(mDragObject.dragX);
			}
			else {
				mLastDropTarget.onDragFarAwayFromEdge(mDragObject.dragX);
			}
		}
	}
	
	private DropTarget updateDropTargetByCoord(int x, int y, ViewGroup parent) {
//		Log.d(TAG, "dropCoords = [" + x + ", " + y + "]");
		int count = parent.getChildCount();
		for(int i=0;i<count;i++) {
			View child = parent.getChildAt(i);
			if(findCoordInView(child, x, y)) {
				if(child instanceof DropTarget) {
					return (DropTarget)child;
				}
				else {
					if(child instanceof ViewGroup) {
						return updateDropTargetByCoord(x, y, (ViewGroup) child);
					}
				}
			}
		}
		return null;
	}
	
	private boolean findCoordInView(View view, int x, int y) {
		getDescendantRectRelativeToSelf(view, tmpRect);
		return tmpRect.contains(x, y);
	}
	
	private void dropTarget(int touchX, int touchY) {
		Log.d(TAG, "dropTarget running");
		mIsDragging = false;
		mDragObject.dragX = touchX;
		mDragObject.dragY = touchY;
		if(mLastDropTarget != null) {
			if(mLastDropTarget.isNearEdge(mDragObject.dragX)) {
				mLastDropTarget.onDragFarAwayFromEdge(mDragObject.dragX);
			}
			mLastDropTarget.onDrop(mDragObject);
			
		}
		else {
			if(mFirstDropTarget != null) {
				mFirstDropTarget.onDrop(mDragObject);
			}
			Log.d(TAG, "mLastDropTarget == null");
		}
		mFirstDropTarget = mLastDropTarget = null;
	}
	
	public void removeDragView() {
		if(mDragView != null) {
			this.removeView(mDragView);
			mDragView = null;
		}
	}
	
	public static class LayoutParams extends FrameLayout.LayoutParams {
		
		public int x;
		public  int y;
		public boolean mIsCustomLayout = false;
		
		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
		}
		
		public LayoutParams(int w, int h) {
			super(w, h);
		}
		
		public LayoutParams(int w, int h, int x, int y) {
			this(w, h);
			this.x = x;
			this.y = y;
		}
		
		public void setX(int x) {
			this.x = x;
		}
		public int getX() {
			return this.x;
		}
		public void setY(int y) {
			this.y = y;
		}
		public int getY() {
			return this.y;
		}
		public void setWidth(int width) {
			this.width = width;
		}
		public int getWidth() {
			return this.width;
		}
		public void setHeight(int height) {
			this.height = height;
		}
		public int getHeight() {
			return this.height;
		}
		public void setCustomLayout(boolean custom) {
			this.mIsCustomLayout = custom;
		}
		public boolean isCustomLayout() {
			return this.mIsCustomLayout;
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		int count = getChildCount();
		for(int i=0;i<count;i++) {
			View view = getChildAt(i);
			FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams)view.getLayoutParams();
			if(flp instanceof LayoutParams) {
				LayoutParams lp = (LayoutParams)flp;
				if(lp.isCustomLayout()) {
					view.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
				}
			}
		}
	}

	public float getDescendantRectRelativeToSelf(View descendant, Rect r) {
		mTmpXY[0] = 0;
		mTmpXY[1] = 0;
		float scale = getDescendantCoordRelativeToSelf(descendant, mTmpXY);
		r.set(mTmpXY[0], mTmpXY[1], mTmpXY[0] + (int)(descendant.getWidth()*scale), mTmpXY[1] + (int)(descendant.getHeight()*scale));
		return scale;
	}
	
	public void getLocationInDragLayer(View child, int[] loc) {
        loc[0] = 0;
        loc[1] = 0;
        getDescendantCoordRelativeToSelf(child, loc);
    }
	
	public float getDescendantCoordRelativeToSelf(View descendant, int[] coord) {
		float scale = 1.0f;
		float[] pt = {coord[0], coord[1]};
		descendant.getMatrix().mapPoints(pt);
		scale *= descendant.getScaleX();
		pt[0] += descendant.getLeft();
		pt[1] += descendant.getTop();
		
		ViewParent parent = descendant.getParent();
		while(parent instanceof View && parent != this) {
			View view = (View)parent;
			view.getMatrix().mapPoints(pt);
			pt[0] += view.getLeft();
			pt[1] += view.getTop();
			scale *= view.getScaleX();
			parent = parent.getParent();
		}
		
		coord[0] = Math.round(pt[0]);
		coord[1] = Math.round(pt[1]);
		return scale;
	}
	
	public void getViewRectRelativeToSelf(View v, Rect r) {
        int[] loc = new int[2];
        getLocationInWindow(loc);
        int x = loc[0];
        int y = loc[1];

        v.getLocationInWindow(loc);
        int vX = loc[0];
        int vY = loc[1];

        int left = vX - x;
        int top = vY - y;
        r.set(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
    }
}
