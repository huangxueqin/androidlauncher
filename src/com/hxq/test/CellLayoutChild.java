package com.hxq.test;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class CellLayoutChild extends ViewGroup {
	
	public final String TAG = "CellLayoutChild TAG";
	
	private int mCellCountX;
	private int mCellCountY;
	private int mCellWidth;
	private int mCellHeight;
	private int mCellWidthGap;
	private int mCellHeightGap;

	public CellLayoutChild(Context context) {
		super(context);
	}
	
	public CellLayoutChild(Context context, int cellCountX, int cellCountY, int cellWidth, int cellHeight, int widthGap, int heightGap) {
		this(context);
		setCellDimension(cellCountX, cellCountY, cellWidth, cellHeight, widthGap, heightGap);
	}
	
	public void setCellDimension(int countX, int countY, int cellWidth, int cellHeight, int widthGap, int heightGap) {
		this.mCellCountX = countX;
		this.mCellCountY = countY;
		this.mCellHeight = cellHeight;
		this.mCellHeightGap = heightGap;
		this.mCellWidth = cellWidth;
		this.mCellWidthGap = widthGap;
	}
	
	public View findViewByPosition(int[] cellXY) {
		int count = getChildCount();
		for(int i=0;i<count;i++) {
			View child = getChildAt(i);
			CellLayout.LayoutParams lp = (CellLayout.LayoutParams)child.getLayoutParams();
			if(lp.cellX == cellXY[0] && lp.cellY == cellXY[1]) {
				return child;
			}
		}
		return null;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int count = getChildCount();
		for(int i=0;i<count;i++) {
			View child = getChildAt(i);
			int childWidthSpec = MeasureSpec.makeMeasureSpec(mCellWidth, MeasureSpec.EXACTLY);
			int childHeightSpec = MeasureSpec.makeMeasureSpec(mCellHeight, MeasureSpec.EXACTLY);
			child.measure(childWidthSpec, childHeightSpec);
		}
		int widthSize = mCellCountX * (mCellWidth + mCellWidthGap);
		int heightSize = mCellCountY * (mCellHeight + mCellHeightGap);
//		Log.d(TAG, "widthSize = " + MeasureSpec.getSize(widthMeasureSpec) + ", heightSize = " + MeasureSpec.getSize(heightMeasureSpec)); 
		setMeasuredDimension(widthSize, heightSize);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		int count = getChildCount();
		int offsetL = mCellWidthGap / 2;
		int offsetT = mCellHeightGap /2;
		for(int i=0;i<count;i++) {
			View child = getChildAt(i);
			CellLayout.LayoutParams lp = (CellLayout.LayoutParams)child.getLayoutParams();
			int intrWidth = lp.intrinsicWidth;
			int intrHeight = lp.intrinsicHeight;
			int cl = lp.cellX * (mCellWidth + mCellWidthGap) + offsetL;
			int ct = lp.cellY * (mCellHeight + mCellHeightGap) + offsetT;
			int cw = mCellWidth;
			int ch = mCellHeight;
			if(intrWidth < mCellWidth) {
				cl += (mCellWidth - intrWidth) / 2;
				cw = intrWidth;
			}
			if(intrHeight < mCellHeight) {
				ct += (mCellHeight - intrHeight) / 2;
				ch = intrHeight;
			}
			child.layout(cl, ct, cl + cw, ct + ch);
		}
	}
	
	@Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            // Update the drawing caches
            if (!view.isHardwareAccelerated() && enabled) {
                view.buildDrawingCache(true);
            }
        }
    }

    @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }
	
	

}
