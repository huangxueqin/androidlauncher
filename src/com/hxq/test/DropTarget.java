package com.hxq.test;

public interface DropTarget {
	public class DragObject {
		DragView dragView;
		Object data;
		int dragX;
		int dragY;
		
	}
	public void onDragEnter(DragObject object);
	public void onDragOut(DragObject object);
	public void onDragOver(DragObject object);
	public void onDrop(DragObject object);
	public boolean isNearEdge(int dragX);
	public void onDragNearEdge(int dragX);
	public void onDragFarAwayFromEdge(int dragX);
}
