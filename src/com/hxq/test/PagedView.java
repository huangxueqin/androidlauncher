package com.hxq.test;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

public class PagedView extends ViewGroup {

	static final String TAG = "PagedView TAG";
	static final boolean DEBUG = true;

	private int mChildPaddingLeft;
	private int mChildPaddingTop;
	private int mChildPaddingRight;
	private int mChildPaddingBottom;

	private int mPageSpacing = -1;
	private int[] mSpacingCache;
	private boolean mUniformSpacing = false;

	private int[] mChildOffset;
	private int[] mChildRelativeOffset;
	private int maxChildWidthSize = -1;
	private int maxChildHeightSize = -1;
	private int minChildWidthSize = -1;
	private int minChildHeightSize = -1;

	Scroller mScroller;

	OnLongClickListener mOnLongClickListener;

	private static final int INVALID_PAGE = -1;
	int mCurrentPage = 0;
	int mNextPage = INVALID_PAGE;
	VelocityTracker mVelocityTracker = null;

	static int sTouchSlop = 0;
	static int sThresholdVelocity = 1500;
	static int sThresholdScrollDist = -1;
	static int sPagingSlop = 0;
	static final boolean sUsingPagingSlop = true;
	static int sMaxOverScroll = -1;
	static final int sMaxOverScrollFactor = 3;

	public final static int TOUCH_STATE_REST = 0;
	public final static int TOUCH_STATE_SCROLLING = 1;

	private int mTouchState = TOUCH_STATE_REST;
	protected int mLastMotionX;
	protected int mLastMotionY;
	private int mActivePointerId;
	
	private PageSwitchListener mPageSwitchListener;

	public PagedView(Context context) {
		this(context, null);
	}

	public PagedView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PagedView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.PagedView, defStyle, 0);
		mChildPaddingLeft = a.getDimensionPixelSize(
				R.styleable.PagedView_pageLayoutPaddingLeft, 0);
		mChildPaddingTop = a.getDimensionPixelSize(
				R.styleable.PagedView_pageLayoutPaddingTop, 0);
		mChildPaddingRight = a.getDimensionPixelSize(
				R.styleable.PagedView_pageLayoutPaddingRight, 0);
		mChildPaddingBottom = a.getDimensionPixelSize(
				R.styleable.PagedView_pageLayoutPaddingBottom, 0);
		a.recycle();
		init();
	}

	private void init() {
		mScroller = new Scroller(getContext());
		mCurrentPage = 0;
		sTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		sPagingSlop = ViewConfiguration.get(getContext())
				.getScaledPagingTouchSlop();

		Resources res = getContext().getResources();
		boolean customSpacing = res.getBoolean(R.bool.use_custom_page_gap_size);
		if (customSpacing) {
			mUniformSpacing = true;
			if (mPageSpacing < 0) {
				setPageSpacing((int) res.getDimension(R.dimen.page_gap));
			}
		}
		// Log.d(TAG, "sTouchSlop = " + sTouchSlop);
		// Log.d(TAG, "sPagingSlop = " + sPagingSlop);
	}

	public interface PageSwitchListener {
		public void onPageSwitch(int newPageIndex);
		public void onPageNumChanged(int newPageNum);
	}
	
	public View getPageForIndex(int i) {
		return getChildAt(i);
	}

	public int getCurrentPageIndex() {
		return mCurrentPage;
	}

	public int getPageCount() {
		return getChildCount();
	}

	public void snapToPrevPage() {
		if (mCurrentPage > 0) {
			snapToScreen(mCurrentPage - 1);
		}
	}
	

	public void snapToNextPage() {
		if (mCurrentPage < getPageCount() - 1) {
			snapToScreen(mCurrentPage + 1);
		}
	}
	
	private void onPageSwitchStart() {
		
	}
	
	private void onPageSwitchEnd() {
		if (mPageSwitchListener != null) {
			mPageSwitchListener.onPageSwitch(mCurrentPage);
		}
	}
	
	public void setPageSwitchListener(PageSwitchListener listener) {
		this.mPageSwitchListener = listener;
	}

	private void determineScrollingStart(MotionEvent ev) {
		final float touchX = ev.getX();
		final float touchY = ev.getY();
		final int xDist = (int) Math.abs(touchX - mLastMotionX);
		final int yDist = (int) Math.abs(touchY - mLastMotionY);
		if (sUsingPagingSlop ? xDist > sPagingSlop : xDist > sTouchSlop) {
			mTouchState = TOUCH_STATE_SCROLLING;
		}
		if (xDist > sTouchSlop || yDist > sTouchSlop) {
			// Log.d(TAG, "cancelcurrentpagelongpress");
			cancelCurrentPageLongPress();
		}
	}

	private void cancelCurrentPageLongPress() {
		View child = getChildAt(mCurrentPage);
		if (child != null) {
			child.cancelLongPress();
		}
	}

	private void changeActivePointerTo(MotionEvent ev, int newPointerIndex) {
		mActivePointerId = ev.getPointerId(newPointerIndex);
		mLastMotionX = (int) ev.getX(newPointerIndex);
		mLastMotionY = (int) ev.getY(newPointerIndex);
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int index = ev.getActionIndex();
		// Log.d(TAG, "non prime point up index = " + index + index +
		// ", newPointerid = " + ev.getPointerId(index));
		final int pointerId = ev.getPointerId(index);
		if (pointerId == mActivePointerId) {
			final int newPointerIndex = index == 0 ? 1 : 0;
			// Log.d(TAG, "find another active pointer, newPointerIndex = " +
			// newPointerIndex + ", newPointerid = " +
			// ev.getPointerId(newPointerIndex));
			changeActivePointerTo(ev, newPointerIndex);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		// LogUtil.Logd(LogUtil.WORKSPACE_LOG, "onInterceptTouchEvent running");
		// LogUtil.Logd(LogUtil.WORKSPACE_LOG, "ev.x = " + ev.getX() +
		// ",ev.y = " + ev.getY());
		int action = ev.getAction();
		if (action == MotionEvent.ACTION_MOVE
				&& mTouchState == TOUCH_STATE_SCROLLING) {
			return true;
		}
		float x = ev.getX();
		float y = ev.getY();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_MOVE:
			// Log.d(TAG, "onInterceptTouchEvent ACTION_MOVE");
			determineScrollingStart(ev);
			break;
		case MotionEvent.ACTION_DOWN:
			// Log.d(TAG, "onInterceptTouchEvent ACTION_DOWN");
			mLastMotionX = (int) x;
			mLastMotionY = (int) y;
			mActivePointerId = ev.getPointerId(0);
			// Log.d(TAG, "onInterceptTouchEvent point count = " +
			// ev.getPointerCount() );
			if (mScroller.isFinished()) {
				// Log.d(TAG, "mscroller is finished");
				mTouchState = TOUCH_STATE_REST;
				mScroller.abortAnimation();
			} else {
				// Log.d(TAG, "mscroller is not finished");
				mTouchState = TOUCH_STATE_SCROLLING;
			}
			// Log.d(TAG, "onInterceptTouchEvent ACTION_DOWN finished");
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			// Log.d(TAG, "onInterceptTouchEvent ACTION_UP");
			// Log.d(TAG, "onInterceptTouchEvent point count = " +
			// ev.getPointerCount());
			mLastMotionX = (int) x;
			mLastMotionY = (int) y;
			break;
		case MotionEvent.ACTION_POINTER_UP:
			// Log.d(TAG,
			// "onInterceptTouch non prime pointer up, point count = " +
			// ev.getPointerCount());
			onSecondaryPointerUp(ev);
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			// Log.d(TAG,
			// "onInterceptTouch non prime pointer down, point count = " +
			// ev.getPointerCount());
			break;
		}
		if (mTouchState != TOUCH_STATE_REST) {
			// Log.d(TAG, "return true");
		} else {
			// Log.d(TAG, "return false");
		}
		return mTouchState != TOUCH_STATE_REST;
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		mOnLongClickListener = l;
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).setOnLongClickListener(l);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Log.d(TAG, "onTouchEvent running");
		final int pointerId = event.findPointerIndex(mActivePointerId);
		float x = event.getX(pointerId);
		acquireVelocityTrackerAndMovement(event);
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_MOVE:
			// Log.d(TAG, "onTouchEvent ACTION_MOVE");
			if (mTouchState == TOUCH_STATE_SCROLLING) {
				// Log.d(TAG,
				// "onTouchEvent ACTION_MOVE mTouchState == TOUCH_STATE_SCROLLING");
				int deltaX = (int) (mLastMotionX - x);
				scrollPageBy(deltaX);
				mLastMotionX = (int) x;
			} else {
				determineScrollingStart(event);
			}
			break;
		case MotionEvent.ACTION_DOWN:
			// Log.d(TAG, "onTouchEvent ACTION_DOWN");
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			// Log.d(TAG, "onTouchEvent ACTION_UP");
			// Log.d(TAG, "onInterceptTouchEvent point count = " +
			// event.getPointerCount());
			// Log.d(TAG, "onInterceptTouchEvent point index = " +
			// event.getActionIndex());
			if (mTouchState == TOUCH_STATE_SCROLLING) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000);
				int velocityX = (int) velocityTracker.getXVelocity();
				// Log.d(TAG, "onTouchEvent ACTION_UP velocityX = " +
				// velocityX);
				if (mCurrentPage > 0 && velocityX > sThresholdVelocity) {
					mNextPage = mCurrentPage - 1;
				} else if ((mCurrentPage < getChildCount() - 1)
						&& (0 - velocityX) > sThresholdVelocity) {
					mNextPage = mCurrentPage + 1;
				} else {
					mNextPage = snapToDestination();
				}
				snapToScreen(mNextPage);
				mNextPage = INVALID_PAGE;
				mTouchState = TOUCH_STATE_REST;
				releaseVelocityTracker();
			}
			break;
		case MotionEvent.ACTION_POINTER_UP:
			// Log.d(TAG,"onEventTouch non prime pointer up, point count = " +
			// event.getPointerCount());
			onSecondaryPointerUp(event);
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			// Log.d(TAG, "onEventTouch non prime pointer down, point count = "
			// + event.getPointerCount());
			break;
		}
		return true;
	}

	private void acquireVelocityTrackerAndMovement(MotionEvent ev) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);
	}

	private void releaseVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	private void scrollPageBy(int deltaX) {
		final int curScrollX = getScrollX();
		if (curScrollX + deltaX > maxScrollX() + sMaxOverScroll) {
			deltaX = maxScrollX() + sMaxOverScroll - curScrollX;
		} else if (curScrollX + deltaX < -sMaxOverScroll) {
			deltaX = -sMaxOverScroll - curScrollX;
		}
		// Log.d(TAG, "deltaX = " + deltaX);
		scrollBy(deltaX, 0);
	}

	private int maxScrollX() {
		int count = getChildCount();
		return count == 0 ? 0 : getChildOffset(count - 1)
				- getChildRelativeOffset(0);
	}

	public void snapToScreen(int index) {
		int offset = getChildOffset(index) - getChildRelativeOffset(index)
				- getScrollX();
		onPageSwitchStart();
		mScroller.startScroll(getScrollX(), getScrollY(), offset, 0);
		mCurrentPage = index;
		invalidate();
	}

	public int snapToDestination() {
		int destPage = mCurrentPage;
		final int offset = getChildOffset(mCurrentPage)
				- getChildRelativeOffset(0) - getScrollX();
		// Log.d(TAG, "sThresholdDistance = " + sPagingSlop);
		if (mCurrentPage > 0 && offset > sThresholdScrollDist) {
			destPage -= 1;
		} else if (mCurrentPage < getChildCount() - 1
				&& (0 - offset) > sThresholdScrollDist) {
			destPage += 1;
		}
		return destPage;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		int verticalPadding = getPaddingTop() + getPaddingBottom()
				+ mChildPaddingTop + mChildPaddingBottom;
		int horizentalPadding = getPaddingLeft() + getPaddingRight()
				+ mChildPaddingLeft + mChildPaddingRight;

		int count = getChildCount();
		maxChildWidthSize = 0;
		maxChildHeightSize = 0;
		minChildWidthSize = Integer.MAX_VALUE;
		minChildWidthSize = Integer.MAX_VALUE;
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			int childWidthMode = MeasureSpec.EXACTLY;
			LayoutParams lp = child.getLayoutParams();
			if (lp.width == LayoutParams.WRAP_CONTENT) {
				childWidthMode = MeasureSpec.AT_MOST;
			}
			int childHeightMode = MeasureSpec.EXACTLY;
			if (lp.height == LayoutParams.WRAP_CONTENT) {
				childHeightMode = MeasureSpec.AT_MOST;
			}
			child.measure(MeasureSpec.makeMeasureSpec(widthSize
					- horizentalPadding, childWidthMode), MeasureSpec
					.makeMeasureSpec(heightSize - verticalPadding,
							childHeightMode));
			maxChildWidthSize = Math.max(maxChildWidthSize,
					child.getMeasuredWidth());
			maxChildHeightSize = Math.max(maxChildHeightSize,
					child.getMeasuredHeight());
			minChildWidthSize = Math.min(minChildWidthSize,
					child.getMeasuredWidth());
			minChildHeightSize = Math.max(minChildHeightSize,
					child.getMeasuredHeight());
		}

		if (widthMode == MeasureSpec.AT_MOST) {
			widthSize = maxChildWidthSize + horizentalPadding;
		}
		if (heightMode == MeasureSpec.AT_MOST) {
			heightSize = maxChildHeightSize + verticalPadding;
		}

		setMeasuredDimension(widthSize, heightSize);
		updateCachedOffset();
		if (!mUniformSpacing) {
			updateCachedSpacing();
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int count = getChildCount();
		int measuredWidth = getMeasuredWidth();
		if (sThresholdScrollDist < 0) {
			sThresholdScrollDist = measuredWidth / 3;
		}
		if (sMaxOverScroll < 0) {
			sMaxOverScroll = measuredWidth / sMaxOverScrollFactor;
		}

		int paddingTop = getPaddingTop();
		int verticalPadding = paddingTop + getPaddingBottom();
		int childLeft = getChildRelativeOffset(0);
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			int childHeight = child.getMeasuredHeight();
			int childWidth = child.getMeasuredWidth();
			// Log.d(TAG, "child " + i + " width = " + childWidth +
			// ", height = " + childHeight);
			int childTop = paddingTop
					+ (getMeasuredHeight() - verticalPadding - childHeight) / 2;
			child.layout(childLeft, childTop, childLeft + childWidth, childTop
					+ childHeight);
			if (i < count - 1) {
				childLeft += childWidth + getSpacingByIndex(i);
			}
		}
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			invalidate();
		}
		if(mScroller.isFinished()) {
			onPageSwitchEnd();
		}
	}

	public void setPageSpacing(int spacing) {
		mPageSpacing = spacing;
	}

	private int getChildOffset(int index) {
		if (mChildOffset != null && mChildOffset[index] != -1) {
			return mChildOffset[index];
		} else {
			int offset = getChildRelativeOffset(0);
			for (int i = 0; i < index; i++) {
				offset += getChildAt(i).getMeasuredWidth()
						+ getSpacingByIndex(i);
			}
			if (mChildOffset != null) {
				mChildOffset[index] = offset;
			}
			return offset;
		}

	}

	private int getChildRelativeOffset(int index) {
		if (mChildRelativeOffset != null && mChildRelativeOffset[index] != -1) {
			return mChildRelativeOffset[index];
		} else {
			int padding = getPaddingLeft() + getPaddingRight();
			int relativeOffset = getPaddingLeft()
					+ (getMeasuredWidth() - padding - getChildAt(index)
							.getMeasuredWidth()) / 2;
			if (mChildRelativeOffset != null) {
				mChildRelativeOffset[index] = relativeOffset;
			}
			return relativeOffset;
		}
	}

	private int getSpacingByIndex(int index) {
		if (mUniformSpacing) {
			return mPageSpacing;
		}
		if (mSpacingCache != null && mSpacingCache[index] != -1) {
			return mSpacingCache[index];
		} else {
			int padding = getPaddingLeft() + getPaddingRight();
			int offsetRight = (getMeasuredWidth() - padding - getChildAt(index)
					.getMeasuredWidth()) / 2;
			int offsetLeft = (getMeasuredWidth() - padding - getChildAt(
					index + 1).getMeasuredWidth()) / 2;
			int spacing = offsetRight + offsetLeft + padding;
			if (mSpacingCache != null) {
				mSpacingCache[index] = spacing;
			}
			return spacing;
		}
	}

	private void updateCachedOffset() {
		int count = getChildCount();
		if (count == 0) {
			mChildOffset = null;
			mChildRelativeOffset = null;
		} else {
			mChildOffset = new int[count];
			mChildRelativeOffset = new int[count];
			for (int i = 0; i < count; i++) {
				mChildOffset[i] = -1;
				mChildRelativeOffset[i] = -1;
			}
		}
	}

	private void updateCachedSpacing() {
		int count = getChildCount() - 1;
		if (count <= 0) {
			mSpacingCache = null;
		} else {
			mSpacingCache = new int[count];
			for (int i = 0; i < count; i++) {
				mSpacingCache[i] = -1;
			}
		}
	}

}
