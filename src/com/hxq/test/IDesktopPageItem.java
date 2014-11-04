package com.hxq.test;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface IDesktopPageItem {
	public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache);
	public void setIconVisibility(int visibility);
	public int getIconWidth();
	public int getIconHeight();
	public ItemInfo getCorrespondItemInfo();
	public View getView();
	public Drawable getIconDrawable();
}
