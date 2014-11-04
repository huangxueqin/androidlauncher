package com.hxq.test;

import android.net.Uri;

public class LauncherSettings {
	public static class ItemType {
		static final int TYPE_NONE = 0;
		static final int TYPE_SHORTCUT = 1;
	}
	
	public static final String _ID = "_id";
    public static final String CELLX = "cellX";
    public static final String CELLY = "cellY";
    public static final String PAGE = "page";
    public static final String CELL_POS = "cellPos";
    public static final String TITLE = "title";
    public static final String INTENT = "intent";
    public static final String ITEM_TYPE = "itemType";
    public static final String ICON_TYPE = "iconType";
    public static final String ICON_PACKAGE = "iconPackage";
    public static final String ICON_RESOURCE = "iconResource";
    public static final String ICON = "icon";

    public static final int ICON_TYPE_RESOURCE = 0;
    public static final int ICON_TYPE_BITMAP = 1;

    static final Uri CONTENT_URI = Uri.parse("content://" +
            LauncherProvider.AUTHORITY + "/" + DatabaseHelper.TABLE_NAME_FAVORITES +
            "?" + LauncherProvider.PARAMETER_NOTIFY + "=false");
    static final Uri CONTENT_URI_NOTIFY = Uri.parse("content://" +
            LauncherProvider.AUTHORITY + "/" + DatabaseHelper.TABLE_NAME_FAVORITES +
            "?" + LauncherProvider.PARAMETER_NOTIFY + "=true");
    static final String CONTENT_URI_STRING = "content://" + LauncherProvider.AUTHORITY + "/" + DatabaseHelper.TABLE_NAME_FAVORITES;
}
