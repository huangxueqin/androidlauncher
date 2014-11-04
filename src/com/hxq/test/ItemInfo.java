package com.hxq.test;

public abstract class ItemInfo {
	public static final String TAG = "ItemInfo TAG";
	public static final int INVALID_POSITION = -1;
	public static final int INVALID_PAGE = -1;
	
	long id;
	int itemType;
	int page;
	int position;
	
	public ItemInfo () {
		itemType = LauncherSettings.ItemType.TYPE_NONE;
		page = INVALID_PAGE;
		position = INVALID_POSITION;
	}
	
	public ItemInfo(int itemType, int page, int position) {
		this.itemType = itemType;
		this.page = page;
		this.position = position;
	}
	
	public ItemInfo(ItemInfo info) {
		this.itemType = info.itemType;
		this.page = info.page;
		this.position = info.position;
	}
	
	public void unbind() {
		
	}

	@Override
	public String toString() {
		return "itemType = " + itemType + ", page = " + page + ", position = " + position;
	}
	
}
