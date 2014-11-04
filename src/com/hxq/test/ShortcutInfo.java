package com.hxq.test;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;

public class ShortcutInfo extends ItemInfo {
	
	private String mTitle;
	private Bitmap mIcon;
	private Intent mIntent;
	private Intent.ShortcutIconResource mIconReference;
	
	public ShortcutInfo() {
		super();
		super.itemType = LauncherSettings.ItemType.TYPE_SHORTCUT;
	}
	
	public ShortcutInfo(ShortcutInfo info) {
		super(info);
		this.mTitle = info.mTitle;
		this.mIcon = info.mIcon;
		this.mIntent = new Intent(info.mIntent);
		if(info.mIconReference != null) {
			mIconReference = new Intent.ShortcutIconResource();
			mIconReference.packageName = info.mIconReference.packageName;
			mIconReference.resourceName = info.mIconReference.resourceName;
		}
	}
	
	//this method is not safe, should only used when info.activityInfo != null.
	public ShortcutInfo(PackageManager pm, ResolveInfo info, IconCache iconCache) {
		final String packageName = info.activityInfo.applicationInfo.packageName;
        ComponentName cn = new ComponentName(packageName, info.activityInfo.name);
        this.setActivity(cn,
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        iconCache.getTitleAndIcon(this, info);
	}
	
	public void setIcon(Bitmap icon) {
		this.mIcon = icon;
	}
	
	public Bitmap getIcon(IconCache cache) {
		if(mIcon == null) {
			mIcon = cache.getIcon(mIntent);
		}
		return mIcon;
	}
	
	public void setTitle(String title) {
		this.mTitle = title;
	}
	
	public String getTitle() {
		return this.mTitle;
	}
	
	public ComponentName getComponentName() {
		return mIntent.getComponent();
	}
	
	public void setActivity(ComponentName cn, int launchFlags) {
		mIntent = new Intent(Intent.ACTION_MAIN);
        mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mIntent.setComponent(cn);
        mIntent.setFlags(launchFlags);
	}
	
	public Intent getIntent() {
		return mIntent;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append(", tilte = " + mTitle);
		sb.append(", intent = " + mIntent.toString());
		return sb.toString();
	}
	
	
}
