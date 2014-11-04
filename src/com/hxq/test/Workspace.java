package com.hxq.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

public class Workspace extends PagedView implements DropTarget, DragSource{

	public static final String TAG = "Workspace TAG";
	static final int DEFAULT_PAGE = 0;
	static final int  DEFAULT_PAGE_NUM = 3;
	
	private LauncherApplication mApplication;
	private DragLayer mDragLayer;
	private int mMainPage = 0;
	private LayoutInflater mInflater;
	private int mPageNum = 3;
	
	//for add all application shortcut in desktop
	int[] tmpPoint = new int[2];
	int[] tmpCoord = new int[2];
	Bitmap b = null;
	
	public Workspace(Context context) {
		this(context, null);
	}
	public Workspace(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	public Workspace(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public void snapToMainPage() {
		snapToScreen(mMainPage);
	}
	
	public void setPageNum(int num) {
		this.mPageNum = num;
	}
	
	public int getPageNum() {
		return this.mPageNum;
	}
	
	private void addCellLayoutPageInWorkspace(int numPageAdded) {
		for(int i=0;i<numPageAdded;i++) {
			CellLayout cell = (CellLayout)mInflater.inflate(R.layout.page_celllayout, null);
			super.addView(cell);
			cell.setOnLongClickListener(mOnLongClickListener);
		}
		this.mPageNum += numPageAdded;
	}
	
	private IDesktopPage getPageById(int pageId) {
		return (IDesktopPage) getPageForIndex(pageId);
	}
	
	private IDesktopPage getCurPage() {
		return getPageById(mCurrentPage);
	}
	
	public void clearAllItemsOnDesktop() {
		int count = getChildCount();
		for(int i=0;i<count;i++) {
			IDesktopPage page = (IDesktopPage) getChildAt(i);
			page.removeAllItems();
		}
	}
	
	public void addItemOnDesktop(IDesktopPageItem dItem, boolean checkOccupied) {
		ItemInfo info = dItem.getCorrespondItemInfo();
		int pageIndex = info.page;
		int positionIndex = info.position;
		int itemType = info.itemType;
		if(itemType == LauncherSettings.ItemType.TYPE_SHORTCUT) {
			if(pageIndex >= getPageCount()) {
				int pageAddNum = pageIndex + 1 - getPageCount();
				addCellLayoutPageInWorkspace(pageAddNum);
			}
		} 
		IDesktopPage page = (IDesktopPage) getChildAt(pageIndex);
		if(checkOccupied) {
			page.removeItemOnPosition(positionIndex);
		}
		page.addViewToDesktop(dItem.getView(), positionIndex, dItem.getIconWidth(), dItem.getIconHeight());
//		Log.d(TAG, "rawWidth = " + dItem.getIconWidth() + ", rawHeight = " + dItem.getIconHeight());
		dItem.getView().setOnLongClickListener(mOnLongClickListener);
	}
	
	/**
	 * get an unoccupied position on desktop,
	 * @return here we only return the unoccupied position at the tail 
	 * on the last page. If last page if full, we create a new page. 
	 */
	public DesktopPosition getUnOccupiedPosition() {
		int pageIndex = getChildCount()-1;
		IDesktopPage page = (IDesktopPage)getChildAt(pageIndex);
		int positionIndex = page.findUnoccupiedPosition();
		if(positionIndex == IDesktopPage.INVALID_POSITION) {
			pageIndex++;
			positionIndex = 0;
			addCellLayoutPageInWorkspace(1);
		}
		return new DesktopPosition(this, pageIndex, positionIndex);
	}
	
	public static class DesktopPosition {
		private Workspace workspace;
		int pageIndex;
		int positionIndex;
		
		public DesktopPosition(Workspace workspace, int page, int indexInPage) {
			this.workspace = workspace;
			this.pageIndex = page;
			this.positionIndex = indexInPage;
		}
		
		public void moveToNextPositionBounded() {
			positionIndex++;
			IDesktopPage iPage = (IDesktopPage) workspace.getPageById(pageIndex);
			if(positionIndex >= iPage.getIconCapacity()) {
				if(pageIndex + 1 >= workspace.getPageCount()) {
					positionIndex--;
				}
				else {
					positionIndex = 0;
					pageIndex++;
				}
			}
		}
		
		public void moveToNextPositionUnBounded() {
			positionIndex++;
			IDesktopPage iPage = (IDesktopPage) workspace.getPageById(pageIndex);
			if(positionIndex >= iPage.getIconCapacity()) {
				positionIndex = 0;
				pageIndex++;
			}
			if(pageIndex >= workspace.getPageCount()) {
				workspace.addCellLayoutPageInWorkspace(1);
			}
		}
		
		public void moveToPrevPosition() {
			positionIndex--;
			IDesktopPage iPage = (IDesktopPage) workspace.getPageById(pageIndex);
			if(positionIndex < 0) {
				if(pageIndex == 0) {
					positionIndex = 0;
				}
				else {
					positionIndex = iPage.getIconCapacity() - 1;
					pageIndex--;
				}
			}
		}
		
		public boolean reachEnd() {
			return pageIndex == workspace.getPageCount() - 1 &&
					positionIndex == ((IDesktopPage) workspace.getPageById(pageIndex)).getIconCapacity() - 1;
		}
	}
	
	private Bitmap createBitmapFromPageItem(IDesktopPageItem item) {
		ItemInfo info = item.getCorrespondItemInfo();
		if(info.itemType == LauncherSettings.ItemType.TYPE_SHORTCUT) {
			ShortcutInfo shortcut = (ShortcutInfo) info;
			return shortcut.getIcon(mApplication.getIconCache());
		}
		else {
			//here are other ItemTypes
			return null;
		}
	}
	
	public void startDrag(IDesktopPage.CellInfo info) {
		if (info.cell instanceof IDesktopPageItem) {
			IDesktopPageItem icon = (IDesktopPageItem) info.cell;
			Bitmap b = createBitmapFromPageItem(icon);
			Rect viewRect = new Rect();
			mDragLayer.getDescendantRectRelativeToSelf(icon.getView(), viewRect);
			viewRect.offset(-getScrollX(), -getScrollY());
			int[] touchXY = { mLastMotionX, mLastMotionY };
//			Log.d(PagedView.TAG, "mLastMotionX = " + mLastMotionX
//					+ ", mLastMotionY = " + mLastMotionY);
			mDragLayer.getDescendantCoordRelativeToSelf(this, touchXY);
//			Log.d(PagedView.TAG, "touchXY[0] = " + touchXY[0]
//					+ ", touchXY[1] = " + touchXY[1]);
//			Log.d(TAG, viewRect.toString());
			icon.setIconVisibility(View.GONE);
			mDragLayer.startDrag(b, viewRect, touchXY, info);
			
			IDesktopPage curPage = getCurPage();
			curPage.setOnDragMode(true);
		}
	}
	
	public void setDragLayer(DragLayer dragLayer) {
		this.mDragLayer = dragLayer;
	}
	
	public void setLayoutInflater(LayoutInflater inflater) {
		this.mInflater = inflater;
	}
	
	public void setLauncherApplication(LauncherApplication app) {
		this.mApplication = app;
	}
	
	@Override
	public void snapToScreen(int index) {
		if(mCurrentPage != index) {
			IDesktopPage curPage = getPageById(mCurrentPage);
			IDesktopPage dstPage = getPageById(index);
			curPage.setOnDragMode(false);
			curPage.setOnDragMode(true);
		}
		super.snapToScreen(index);
	}
	
	//interface override method
	@Override
	public void onDrop(DragObject object) {
		Log.d(TAG, "onDrop running");
		IDesktopPage curPage = getCurPage();
		IDesktopPage.CellInfo info = (IDesktopPage.CellInfo)object.data;
		int origPageIndex = info.pageIndex;
		tmpPoint[0] = tmpPoint[1] = 0;
		mDragLayer.getDescendantCoordRelativeToSelf(curPage.getViewGroup(), tmpPoint);
		Log.d(TAG, "tmpPoint = [" + tmpPoint[0] + ", " + tmpPoint[1] + "]");
		int offsetX = tmpPoint[0] - getScrollX();
		int offsetY = tmpPoint[1] - getScrollY();
		tmpPoint[0] = object.dragX - offsetX;
		tmpPoint[1] = object.dragY - offsetY;
		int nearestPos = curPage.findNearestVacuumPositionFromGivenCoord(tmpPoint, info.position, origPageIndex == mCurrentPage);
		if(nearestPos >= 0) {
			if(origPageIndex != mCurrentPage) {
				IDesktopPage origPage = getPageById(origPageIndex);
				int cellW = info.cell.getMeasuredWidth();
				int cellH = info.cell.getMeasuredHeight();
				origPage.getViewGroup().removeView(info.cell);
				curPage.addViewToDesktop(info.cell, nearestPos, cellW, cellH);
			}
			else {
				curPage.moveViewToNewPosition(info.cell, nearestPos);
			}
			if(origPageIndex != mCurrentPage || nearestPos != info.position) {
//				Log.d(TAG, "update position in database: page = " + mCurrentPage + ", position = " + nearestPos);
				mApplication.getLauncherModel().moveItemInDatabase(mApplication,
						info.id, mCurrentPage, nearestPos, false);
			}
		}
		DragView dragView = object.dragView;
		if(nearestPos >= 0) {
			curPage.getItemExactCoordByPosition((IDesktopPageItem)(info.cell), nearestPos, tmpCoord);
			dragView.startDropAnime(0, tmpCoord[0] + offsetX - object.dragX + dragView.mRegistrationX,
					0, tmpCoord[1] + offsetY -object.dragY + dragView.mRegistrationY, info.cell);
		}
		else {
			dragView.startDropAnime(0, -object.dragX-dragView.mRegistrationX, 0, 0, info.cell);
		}
		if(nearestPos >= 0) {
			curPage.startDropItemAnime(info.cell);
		}
		curPage.setOnDragMode(false);
	}
	
	private static final int SCROLL_NONE = 0;
	private static final int SCROLL_PREV = 1;
	private static final int SCROLL_NEXT = 2;
	private static int SCROLL_LEFT_THRESHOLD = 100;
	private static int SCROLL_RIGHT_THRESHOLD = 720 - SCROLL_LEFT_THRESHOLD;
	private static int POST_DELAY_TIME = 1000;
	private boolean actionHasPosted = false;
	private int action = SCROLL_NONE;
	private ScrollRunnable scrollRunnable = null;
	
	@Override
	public boolean isNearEdge(int dragX) {
		boolean result = true;
		if(dragX <= SCROLL_LEFT_THRESHOLD) {
			action = SCROLL_PREV;
		}
		else if(dragX >= SCROLL_RIGHT_THRESHOLD) {
			action = SCROLL_NEXT;
		}
		else {
			action = SCROLL_NONE;
			result = false;
		}
		return result;
	}
	@Override
	public void onDragNearEdge(int dragX) {
		synchronized (this) {
			if (!actionHasPosted) {
				scrollRunnable = new ScrollRunnable(this, dragX); 
				this.getHandler().postDelayed(scrollRunnable, POST_DELAY_TIME);
				actionHasPosted = true;
			}
		}
		
	}
	
	@Override
	public void onDragFarAwayFromEdge(int dragX) {
		this.getHandler().removeCallbacks(scrollRunnable);
		action = SCROLL_NONE;
		actionHasPosted = false;
		
	}
	
	private class ScrollRunnable implements Runnable {
		int dragX;
		private PagedView pagedView;
		public ScrollRunnable(PagedView pagedView, int dragX) {
			this.pagedView = pagedView;
			this.dragX = dragX;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(action == SCROLL_PREV) {
				pagedView.snapToPrevPage();
			}
			else if(action == SCROLL_NEXT) {
				pagedView.snapToNextPage();
			}
			if(isNearEdge(dragX)) {
				actionHasPosted = false;
				onDragNearEdge(dragX);
			}
		}
		
	}

	@Override
	public void onDragOver(DragObject object) {
		
	}
	@Override
	public void onDragEnter(DragObject object) {
		
	}
	@Override
	public void onDragOut(DragObject object) {
		
	}
	
}
