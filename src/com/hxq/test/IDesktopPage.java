package com.hxq.test;

import android.view.View;
import android.view.ViewGroup;

public interface IDesktopPage{
	
	public static final int PAGE_CELL_COUNT = CellLayout.CELL_COUNT_X * CellLayout.CELL_COUNT_Y;
	public static final int INVALID_POSITION = -1;
	
	public int getIconCapacity();
	public void addViewToDesktop(View view, int index, int iconViewW, int iconViewH);
	public void addViewToDesktop(View view, int iconViewW, int iconViewH);
	public void moveViewToNewPosition(View view, int newPos);
	public void removeItemOnPosition(int position);
	public void removeAllItems();
	public boolean checkDesktopPostionOccupied(int position);
	public ViewGroup getViewGroup();
	public CellInfo getTouchCellInfo();
	public int findUnoccupiedPosition();
	public void getItemExactCoordByPosition(IDesktopPageItem item, int position, int[] coord);
	public void handleDragOver(int coordX, int coordY);
	
	public void setOnDragMode(boolean isOnDrag);
	public boolean getOnDragMode();
	public void startDropItemAnime(View dropView);
	
	/**
	 * find nearest vacuum position according to the given coordinates
	 * @param coordX
	 * @param coordY
	 * @param origPos
	 * @param ignoreSelf if ignore the position the given icon occupied
	 * @return {@link INVALID_POSITION} if there is no appropriate position, else then returns a natural number
	 */
	public int findNearestVacuumPositionFromGivenCoord(int[] coords, int origPos, boolean samePage);
	
	public static class CellInfo {
		long id;
		View cell;
		int position;
		int pageIndex;
	}
	
	//currently, we support page < 256, pos < 256
	public static class DesktopPos {
		private int id;
		private int page;
		private int pos;
		public DesktopPos(int page, int pos) {
			this.page = page;
			this.pos = pos;
			id = generateUniqueID(page, pos);
		}
		
		public int getPage() {
			return page;
		}
		
		public void setPage(int page) {
			if(page != this.page) {
				this.page = page;
				this.id = generateUniqueID(page, pos);
			}
		}
		
		public int getPos() {
			return pos;
		}
		
		public void setPos(int pos) {
			if(this.pos != pos) {
				this.pos = pos;
				this.id = generateUniqueID(page, pos);
			}
		}
		
		public int getId() {
			return id;
		}
		
		public void moveToNext() {
			if(++pos >= IDesktopPage.PAGE_CELL_COUNT) {
				page++;
				pos = 0;
			}
			this.id = generateUniqueID(page, pos);
		}
		public boolean equalTo(DesktopPos rhs) {
			return rhs.page == page && rhs.pos == pos;
		}
		public static int generateUniqueID(int page, int pos) {
			if(page >= 256 || pos >=256) {
				throw new RuntimeException("Desktop Position excess the limit");
			}
			else {
				return (pos | (page << 8)) & 0xFFFF;
			}
		}
		public static DesktopPos getDpFromId(int id) {
			id = id & 0xFFFF;
			int pos = id & 0xFF;
			int page = (id >> 8) & 0xFF;
			return new DesktopPos(page, pos);
		}
	}
}
