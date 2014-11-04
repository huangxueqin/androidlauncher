package com.hxq.test;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

public class CellLayout extends ViewGroup implements IDesktopPage{

	public static String TAG = "CellLayout TAG";
	
	public static final int CELL_COUNT_X = 4;
	public static final int CELL_COUNT_Y = 5;
	
	private static final int UNKNOWN_PAGE_INDEX = -1;
	private static final int UNKNOWN_CELL_INDEX = -1;
	
	private int mCellCountX;
	private int mCellCountY;
	private int mCellWidth;
	private int mHalfCellWidth;
	private int mCellHeight;
	private int mHalfCellHeight;
	private int mCellWidthGap;
	private int mCellHeightGap;
	
	boolean[] mOccupied;
	boolean mLastDownOnOccupiedCell;
	private int mPageIndex = UNKNOWN_PAGE_INDEX;
	
	private boolean mOnDragMode = false;
	private ArrayList<View> mMovedChildrenInDragMode = new ArrayList<View> ();
	
	private CellLayoutChild mChild;
	
	private Paint mPaint;
	
	private final int animeInterval = 10;
	private Handler mHandler = new Handler();
	
	private Rect mTempRect = new Rect();
	private CellInfo mCellInfo = new CellInfo();
	private int[] mTempXY = new int[2];
	
	public CellLayout(Context context) {
		this(context, null);
	}

	public CellLayout(Context context, AttributeSet attr) {
		this(context, attr, 0);
	}
	
	public CellLayout(Context context, AttributeSet attr, int defStyle) {
		super(context, attr, defStyle);
		mCellCountX = CELL_COUNT_X;
		mCellCountY = CELL_COUNT_Y;
		TypedArray a = context.obtainStyledAttributes(attr, R.styleable.CellLayout, defStyle, 0);
		mCellWidth = a.getDimensionPixelSize(R.styleable.CellLayout_cellWidth, 10);
		mCellHeight = a.getDimensionPixelSize(R.styleable.CellLayout_cellHeight, 10);
		mHalfCellWidth = mCellWidth / 2;
		mHalfCellHeight = mCellHeight / 2;
		mCellWidthGap = a.getDimensionPixelSize(R.styleable.CellLayout_widthGap, 0);
		mCellHeightGap = a.getDimensionPixelSize(R.styleable.CellLayout_heightGap, 0);
		a.recycle();
		initOccupied();
		mPaint = new Paint(Color.BLUE);
		mPaint.setAntiAlias(true);
//		this.setWillNotDraw(false);
		mChild = new CellLayoutChild(context);
		mChild.setCellDimension(mCellCountX, mCellCountY, mCellWidth, mCellHeight, mCellWidthGap, mCellHeightGap);
		
		addView(mChild, 0);
	}
	
	private void initOccupied() {
		mOccupied = new boolean[mCellCountX * mCellCountY];
		for(int y=0;y<mOccupied.length;y++) {
			mOccupied[y] = false;
		}
	}
	
	public void setupCellDimensions(int cellWidth, int cellWidthGap, int cellHeight, int cellHeightGap) {
		mCellWidth = cellWidth;
		mCellHeight = cellHeight;
		mCellWidthGap = cellWidthGap;
		mCellHeightGap = cellHeightGap;
		mHalfCellWidth = cellWidth / 2;
		mHalfCellHeight = cellHeight / 2;
	}
	
	
	public boolean hasMoreSpace() {
		return mChild.getChildCount() < mCellCountX * mCellCountY;
	}
	
	@SuppressWarnings("unused")
	private void dumpViewDimensionInfo() {
		Log.d(TAG, "CellLayoutInfo: [ " + getLeft() + ", " + getTop() + 
				", " + getWidth() + ", " + getHeight() + "]");
		PagedView pagedView = (PagedView) getParent();
		Log.d(TAG, "WorkspaceInfo: [ " + pagedView.getLeft() + ", " + pagedView.getTop() + 
				", " + pagedView.getMeasuredWidth() + ", " + pagedView.getMeasuredHeight() + "]");
		Log.d(TAG, "Workspace scroll: [ " + pagedView.getScrollX() + ", " + pagedView.getScrollY() + "]");
		DragLayer dragLayer = (DragLayer) pagedView.getParent();
		Log.d(TAG, "DragLayerInfo: [ " + dragLayer.getLeft() + ", " + dragLayer.getTop() + 
				", " + dragLayer.getMeasuredWidth() + ", " + dragLayer.getMeasuredHeight() + "]");
		Log.d(TAG, "DragLayerInfo scroll: [ " + dragLayer.getScrollX() + ", " + dragLayer.getScrollY() + "]");
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
//		Log.d(TAG, "onInterceptTouchEvent running");
		boolean result = super.onInterceptTouchEvent(ev);
//		dumpMotionEventInfo(ev);
//		Log.d(TAG, "result = " + result);
//		dumpViewDimensionInfo();
//		Log.d(TAG, "cellLayout onInterceptTouchEvent running");
		int touchX = (int) ev.getX();
		int touchY = (int) ev.getY();
		
		if(ev.getAction() == MotionEvent.ACTION_DOWN) {
			setTagToCellInfoForPoint(touchX, touchY);
		}
		return result;
	}

	private void dumpMotionEventInfo(MotionEvent ev) {
		switch(ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "action_down");
			break;
		case MotionEvent.ACTION_MOVE:
			Log.d(TAG, "action_move");
			break;
		case MotionEvent.ACTION_UP:
			Log.d(TAG, "action_up");
			break;
		}
	}
	
	private void setTagToCellInfoForPoint(int px, int py) {
		boolean found = false;
		int count = mChild.getChildCount();
		View child = null;
		for(int i=0;i<count;i++) {
			child = mChild.getChildAt(i);
			child.getHitRect(mTempRect);
			mTempRect.offset(getPaddingLeft(), getPaddingTop());
			if(mTempRect.contains(px, py)) {
				found = true;
				break;
			}
		}
		mLastDownOnOccupiedCell = found;
		CellInfo inf = mCellInfo;
		inf.pageIndex = getPageIndex();
		if(found) {
			inf.id = ((ShortcutInfo) child.getTag()).id;
			LayoutParams lp = (LayoutParams)child.getLayoutParams();
			inf.cell = child;
			inf.position = cellXYToPosition(lp.cellX, lp.cellY);
//			Log.d(TAG, "touch Position is: " + lp.cellX + ", " + lp.cellY + ", " + inf.position);
		}
		else {
			pointToCellExact(px, py, mTempXY);
			inf.cell = null;
			inf.position = cellXYToPosition(mTempXY[0], mTempXY[1]);
//			Log.d(TAG, "touch Position is: " + mTempXY[0] + ", " + mTempXY[1] + ", " + inf.position);
		}
		setTag(inf);
		
	}
	
	private int getPageIndex() {
		if(mPageIndex == UNKNOWN_PAGE_INDEX) {
			updatePageIndexCache();
		}
		return mPageIndex;
	}
	
	private void updatePageIndexCache() {
		ViewParent parent = getParent();
		if(parent instanceof ViewGroup) {
			mPageIndex = ((ViewGroup)parent).indexOfChild(this);
		}
		else {
			mPageIndex = 0;
		}
	}
	
	private void pointToCellExact(int x, int y, int[] result) {
		getRelativeCoord(x, y, mTempXY);
		int coordX = mTempXY[0] - mCellWidthGap / 2;
		int coordY = mTempXY[1] - mCellHeightGap / 2;
		int nx = coordX / (mCellWidth + mCellWidthGap);
		int ny = coordY / (mCellHeight + mCellHeightGap);
		result[0] = coordX >= 0 ? nx : nx - 1;
		result[1] = coordY >= 0 ? ny : ny - 1;
	}
	
	private void getRelativeCoord(int x, int y, int[] rCoord) {
		int oX = getPaddingLeft() + mChild.getPaddingLeft();
		int oY = getPaddingTop() + mChild.getPaddingTop();
		rCoord[0] = x - oX;
		rCoord[1] = y - oY;
	}
	
	private Rect getCellRect(int cellX, int cellY) {
		Rect r = new Rect();
		r.left = (mCellWidth + mCellWidthGap) * cellX;
		r.right = r.left + mCellWidth;
		r.top = (mCellHeight + mCellHeightGap) * cellY;
		r.bottom = r.top + mCellHeight;
		return r;
	}
	
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		getHitRect(mTempRect);
		Log.d(TAG, mTempRect.toString());
		canvas.drawRect(mTempRect, mPaint);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
		
		if(widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
			throw new RuntimeException("CellLayout MeasureSpec Setting error");
		}
		
		int newWidth = widthSpecSize;
		int newHeight = heightSpecSize;
		if(widthSpecMode == MeasureSpec.AT_MOST) {
//			Log.d(TAG, "widthSpecMode is AT_MOST");
			newWidth = getPaddingLeft() + mCellCountX * (mCellWidth + mCellWidthGap) + getPaddingRight();
		} 
		if(heightSpecMode == MeasureSpec.AT_MOST) {
//			Log.d(TAG, "heightSpecMode is AT_MOST");
			newHeight = getPaddingTop() + mCellCountY * (mCellHeight + mCellHeightGap) + getPaddingBottom();
		}
		
		int count = this.getChildCount();
		for(int i=0;i<count;i++) {
			int widthSpecChild = MeasureSpec.makeMeasureSpec(newWidth - getPaddingLeft() - getPaddingRight(), 
					MeasureSpec.AT_MOST);
			int heightSpecChild = MeasureSpec.makeMeasureSpec(newHeight - getPaddingTop() - getPaddingBottom(), 
					MeasureSpec.AT_MOST);
			
			getChildAt(i).measure(widthSpecChild, heightSpecChild);
		}
		setMeasuredDimension(newWidth, newHeight);
//		Log.d(TAG, "width = " + newWidth + ", height = " + newHeight);
	}

	@Override
	public void removeView(View view) {
		LayoutParams lp = (LayoutParams)view.getLayoutParams();
		markCellAsUnOccupied(lp.cellX, lp.cellY);
		mChild.removeView(view);
	}
	
	public void removeViewOnPosition(int[] cellXY) {
		View view = mChild.findViewByPosition(cellXY);	
		removeView(view);
	}
	
	public void addViewToCellLayout(View child, int index, LayoutParams lp, boolean markCell) {
		if(lp.getCellX() >= mCellCountX || lp.getCellY() >= mCellCountY) {
			throw new RuntimeException("exceptions");
		}
		mChild.addView(child, index, lp);
		if(markCell) {
			markCellAsOccupied(lp.cellX, lp.cellY);
		}
	}
	
	public void addViewToCellLayout(View child, LayoutParams lp, boolean markCell) {
		addViewToCellLayout(child, mChild.getChildCount(), lp, markCell);
	}
	
	public void addViewToCellLayout(View child, int intrW, int intrH) {
		if(!hasMoreSpace()) {
			return;
		}
		findVacuumSpace(mTempXY);
		LayoutParams lp = new LayoutParams(mTempXY[0], mTempXY[1], mCellWidth, mCellHeight);
		addViewToCellLayout(child, lp, true);
	}
	
	public boolean findVacuumSpace(int[] loc) {
		for(int y=0;y<mCellCountY;y++) {
			int offset = y*mCellCountX;
			for(int x=0;x<mCellCountX;x++) {
				if(mOccupied[offset + x] == false) {
					loc[0] = x;
					loc[1] = y;
					return true;
				}
			}
		}
		return false;
	}
	
	public void markCellAsOccupied(int cellX, int cellY) {
		mOccupied[cellY * mCellCountX + cellX] = true;
	}
	
	public void markCellAsUnOccupied(int cellX, int cellY) {
		mOccupied[cellY * mCellCountX + cellX] = false;
	}
	
	public void markCellAreaAsOccupied(int startX, int endX, int startY, int endY) {
		for(int y=startY;y<endY;y++) {
			int offset = y * mCellCountX;
			for(int x=startX;x<endX;x++) {
				mOccupied[offset+x] = true;
			}
		}
	}
	
	public void markCellAreaAsUnOccupied(int startX, int endX, int startY, int endY) {
		for(int y=startY;y<endY;y++) {
			int offset = y * mCellCountX;
			for(int x=startX;x<endX;x++) {
				mOccupied[offset+x] = false;
			}
		}
	}
	
	public boolean isCellOccupied(int x, int y) {
		return mOccupied[y * mCellCountX + x];
	}
	
	/**
	 * actually we will only add one View({@link CellLayoutChild} into it
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		
		int count = getChildCount();
		int verticalPadding = getPaddingTop() + getPaddingBottom();
		int horizentalPadding = getPaddingLeft() + getPaddingRight();
		for(int i=0;i<count;i++) {
			View child = getChildAt(i);
			int childWidth = child.getMeasuredWidth();
			int childHeight = child.getMeasuredHeight();
			int top = getPaddingTop() + (getMeasuredHeight() - verticalPadding - childHeight) / 2;
			int left = getPaddingLeft() + (getMeasuredWidth() - horizentalPadding - childWidth) / 2;
			getChildAt(i).layout(left, top, left + childWidth, top + childHeight);
		}
	}
	
	public boolean findNearestCellWithCoords(int[] coord, int[] cellXY, boolean samePage) {
//		Log.d(TAG, "drop Coords = " + "[" + coord[0] + ", " + coord[1] + "]");
//		Log.d(TAG, "mCellWidth = " + mCellWidth + ", mCellHeight = " + mCellHeight);
//		Log.d(TAG, "mPaddingLeft = " + getPaddingLeft() + ", mPaddingTop = " + getPaddingTop());
//		Log.d(TAG, "mChild.mPaddingLeft = " + mChild.getPaddingLeft() + ", mChild.mPaddingTop = " + mChild.getPaddingTop());
		boolean found = false;
		int origCellX = cellXY[0];
		int origCellY = cellXY[1];
		if(samePage) {
			markCellAsUnOccupied(origCellX, origCellY);
		}
		found = findNearestVacuumCellOnPage_v1(coord, cellXY);
		if(samePage) {
			markCellAsOccupied(origCellX, origCellY);
		}
		return found;
	}
	
	/**
	 * this is ineffective version of the search algorithm
	 * @param coord the coordinates from which we want to find closest vacuum position 
	 * @param cellXY the result position
	 * @return true if found such a position, false otherwise
	 */
	private boolean findNearestVacuumCellOnPage_v1(int[] coord, int[] cellXY) {
		boolean found = false;
		int minDist = Integer.MAX_VALUE;
		int[] centerXY = { getPaddingLeft() + mChild.getPaddingLeft() - mHalfCellWidth - mCellWidthGap / 2 ,
				getPaddingTop() + mChild.getPaddingTop() - mHalfCellHeight - mCellHeightGap };
//		Log.d(TAG, "init centerXY = [" + centerXY[0] + ", " + centerXY[1] + "]");
		for(int y=0;y<mCellCountY;y++) {
			centerXY[1] += (mCellHeight + mCellHeightGap);
			centerXY[0] = getPaddingLeft() + mChild.getPaddingLeft() - mHalfCellWidth - mCellWidthGap / 2;
			for(int x=0;x<mCellCountX;x++) {
				centerXY[0] += (mCellWidth + mCellWidthGap);
				if(!isCellOccupied(x, y)) {
					int dist = norm(coord, centerXY);
//					Log.d(TAG, "cell UnOccupied + " + "(" + x + ", " + y + ") = [" + centerXY[0] + ", " + centerXY[1] + "], dist = " + dist);
					if(minDist > dist) {
						minDist = dist;
						cellXY[0] = x;
						cellXY[1] = y;
						found = true;
					}
				}
			}
		}
		return found;
	}
	
	/**
	 * the effective ones
	 * @param coord
	 * @param cellXY
	 * @return
	 */
	private boolean findNearestVacuumCellOnPage_v2(int[] coord, int[] cellXY) {
		
		return false;
	}

	private int norm(int[] from, int[] to) {
		int deltaX = from[0] - to[0];
		int deltaY = from[1] - to[1];
		return deltaX * deltaX + deltaY * deltaY;
	}
	
	public void showChildViewInNewPosition(View child, int[] newLoc) {
		LayoutParams lp = (LayoutParams)child.getLayoutParams();
		markCellAsUnOccupied(lp.cellX, lp.cellY);
		lp.cellX = newLoc[0];
		lp.cellY = newLoc[1];
		markCellAsOccupied(newLoc[0], newLoc[1]);
		mChild.requestLayout();
	}
	
	@Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        mChild.setChildrenDrawingCacheEnabled(enabled);
    }

    @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        mChild.setChildrenDrawnWithCacheEnabled(enabled);
    }
	
	public static class LayoutParams extends ViewGroup.MarginLayoutParams {
		
		int cellX;
		int cellY;
		
		int intrinsicWidth;
		int intrinsicHeight;
		
		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		public LayoutParams(LayoutParams source) {
			super(source);
			cellX = source.cellX;
			cellY = source.cellY;
			intrinsicWidth = source.intrinsicWidth;
			intrinsicHeight = source.intrinsicHeight;
		}
		
		public LayoutParams(int cellX, int cellY, int intrinsicWidth, int intrinsicHeight) {
			super(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			this.cellX = cellX;
			this.cellY = cellY;
			this.intrinsicWidth = intrinsicWidth;
			this.intrinsicHeight = intrinsicHeight;
		}
		
		public void setCellX(int cellX) {
			this.cellX = cellX;
		}
		
		public int getCellX() {
			return cellX;
		}
		
		public void setCellY(int cellY) {
			
			this.cellY = cellY;
		}
		
		public int getCellY() {
			return cellY;
		}
		
		public int getWidth() {
			return width;
		}
		
		public int getHeight() {
			return height;
		}
	}

	public static int getCellLayoutIconCapacity() {
		return CELL_COUNT_X * CELL_COUNT_Y;
	}
	
	public int cellXYToPosition(int cellX, int cellY) {
		return cellY * mCellCountX + cellX;
	}
	
	public void positionToCellXY(int position, int[] cellXY) {
		int numIcons = position + 1;
		cellXY[1] = numIcons / mCellCountX;
		cellXY[0] = numIcons - cellXY[1] * mCellCountX;
		if(cellXY[0] == 0) {
			cellXY[1]--;
			cellXY[0] = mCellCountX - 1;
		}
		else {
			cellXY[0]--;
		}
//		Log.d(TAG, "Position = " + position + "cellXY = " + "[" + cellXY[0] + "," + cellXY[1] + "]");
	}
	
	/**
	 * the following methods are all built for the interface
	 * IDesktopPage
	 */
	
	@Override
	public int getIconCapacity() {
		return mCellCountX * mCellCountY;
	}

	@Override
	public void addViewToDesktop(View view, int position, int viewW, int viewH) {
		if(position < 0 || position >= getIconCapacity()) {
			Log.d(TAG, "Err " + position);
			throw new RuntimeException();
		}
		positionToCellXY(position, mTempXY);
		LayoutParams lp = new LayoutParams(mTempXY[0], mTempXY[1], viewW, viewH);
		addViewToCellLayout(view, lp, true);
	}

	@Override
	public void addViewToDesktop(View view, int iconViewW,
			int iconViewH) {
		
		addViewToCellLayout(view, iconViewW, iconViewH);
	}

	@Override
	public boolean checkDesktopPostionOccupied(int position) {
		positionToCellXY(position, mTempXY);
		return isCellOccupied(mTempXY[0], mTempXY[1]);
	}

	@Override
	public CellInfo getTouchCellInfo() {
		return (CellInfo)this.getTag();
	}

	@Override
	public ViewGroup getViewGroup() {
		return this;
	}

	@Override
	public int findNearestVacuumPositionFromGivenCoord(int[] coords,
			int origPos, boolean notSamePage) {
		
		int[] origCellXY = new int[2];
		positionToCellXY(origPos, origCellXY);
		boolean found = this.findNearestCellWithCoords(coords, origCellXY, notSamePage);
		if(found) {
			return cellXYToPosition(origCellXY[0], origCellXY[1]);
		}
		else {
			return IDesktopPage.INVALID_POSITION;
		}
	}

	@Override
	public void moveViewToNewPosition(View view, int newPos) {
		positionToCellXY(newPos, mTempXY);
		Log.d(TAG, "newPos = " + mTempXY[0] + ", " + mTempXY[1]);
		showChildViewInNewPosition(view, mTempXY);
	}

	@Override
	public void removeItemOnPosition(int position) {
		int[] cellXY = mTempXY;
		positionToCellXY(position, cellXY);
		if(isCellOccupied(cellXY[0], cellXY[1])) {
			removeViewOnPosition(cellXY);
		}
	}

	@Override
	public void removeAllItems() {
		mChild.removeAllViews();
		for(int i=0;i<mOccupied.length;i++) {
			mOccupied[i] = false;
		}
	}

	@Override
	public int findUnoccupiedPosition() {
		int position = IDesktopPage.INVALID_POSITION;
		boolean found = false;
		boolean breaked = false;
		int offset = 0;
		for(int y = mCellCountY-1;y>=0;y--) {
			if(breaked) {
				break;
			}
			offset = y * mCellCountX;
			for(int x = mCellCountX - 1;x>=0;x--) {
				if(!mOccupied[offset + x]) {
					mTempXY[0] = x;
					mTempXY[1] = y;
					found = true;
				}
				else {
					breaked = true;
					break;
				}
			}
		}
		if(found) {
			position = cellXYToPosition(mTempXY[0], mTempXY[1]);
		}
		return position;
	}

	@Override
	public void handleDragOver(int coordX, int coordY) {
		if(mOnDragMode) {
			getRelativeCoord(coordX, coordY, mTempXY);
			
		}
	}

	@Override
	public void setOnDragMode(boolean isOnDrag) {
		if (this.mOnDragMode != isOnDrag) {
			if (isOnDrag) {
				onIntoDragMode();
			} else {
				onLeaveDragMode();
			}
			this.mOnDragMode = isOnDrag;
		}

	}

	public void onIntoDragMode() {
//		Log.d(TAG, "Page" + getPageIndex() + " into drag mode");
		mMovedChildrenInDragMode.clear();
	}

	public void onLeaveDragMode() {
//		Log.d(TAG, "Page" + getPageIndex() + " leave drag mode");
		
	}

	@Override
	public boolean getOnDragMode() {
		return this.mOnDragMode;
	}

	@Override
	public void startDropItemAnime(View dropView) {
		int childCount = mChild.getChildCount();
		for(int i=0;i<childCount;i++) {
			final View child = mChild.getChildAt(i);
			if(!child.equals(dropView)) {
				final int delay = animeInterval * i;
				AnimeRunnable aRunnable = new AnimeRunnable(child);
				mHandler.postDelayed(aRunnable, delay);
			}
		}
	}
	
	private class AnimeRunnable implements Runnable {
		View view;
		public AnimeRunnable(View view) {
			this.view = view;
		}
		@Override
		public void run() {
			Animation mScaleAnime = new ScaleAnimation(1.0f, 1.1f, 1.0f, 1.1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			mScaleAnime.setRepeatCount(1);
			mScaleAnime.setDuration(100);
			mScaleAnime.setRepeatMode(Animation.REVERSE);
			view.startAnimation(mScaleAnime);
		}
		
	}

	@Override
	public void getItemExactCoordByPosition(IDesktopPageItem item, int position, int[] coord) {
		positionToCellXY(position, mTempXY);
		int coordX = mCellWidthGap / 2 + mTempXY[0] * (mCellWidth + mCellWidthGap);
		int coordY = mCellHeightGap /2 + mTempXY[1] * (mCellHeight + mCellHeightGap);
		int offsetX = (mCellWidth - item.getIconWidth()) / 2;
		int offsetY = (mCellHeight - item.getIconHeight()) / 2;
		coord[0] = coordX + offsetX + getPaddingLeft();
		coord[1] = coordY + offsetY + getPaddingTop();
	}

}
