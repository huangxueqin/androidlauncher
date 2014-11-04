package com.hxq.test;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.pm.ResolveInfo;
import android.database.Cursor;

import org.xmlpull.v1.XmlPullParserException;

import com.hxq.test.IDesktopPage.DesktopPos;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.Xml;

public class DatabaseHelper extends SQLiteOpenHelper {

	public static final String TAG = "DatabaseHelper TAG";
	private static final String TAG_FAVORITE = "favorite";

	public static final String DATABASE_NAME = "launcherTest.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME_FAVORITES = "favorites";

    private static final int COLUMN_ID = 0;
    private static final int COLUMN_TITLE = COLUMN_ID + 1;
    private static final int COLUMN_INTENT = COLUMN_TITLE + 1;
    private static final int COLUMN_PAGE = COLUMN_INTENT + 1;
    private static final int COLUMN_CELL_POS = COLUMN_PAGE + 1;
    private static final int COLUMN_ITEMTYPE = COLUMN_CELL_POS + 1;
    private static final int COLUMN_ICONTYPE = COLUMN_ITEMTYPE + 1;
    private static final int COLUMN_ICONPACKAGE = COLUMN_ICONTYPE + 1;
    private static final int COLUMN_ICON = COLUMN_ICONPACKAGE + 1;

    private Context mContext;
    private long maxId = -1l;
    
    private SparseBooleanArray mSba;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        if(maxId == -1) {
            maxId = initializeMaxId(getWritableDatabase());
        }
    }

    private long initializeMaxId(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT MAX(_id) FROM " + TABLE_NAME_FAVORITES, null);
        long id = -1l;

        if(c != null && c.moveToNext()) {
            id = c.getLong(COLUMN_ID);
        }

        if(c != null) {
            c.close();
        }

        if(id == -1l) {
            throw new RuntimeException("cannot initialize maxId");
        }

        return id;
    }

    public long generateNewId() {
        if(maxId < 0) {
            throw new RuntimeException("maxId has not initialized");
        }
        maxId += 1;
        return maxId;
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		maxId = 0l;
        db.execSQL("CREATE TABLE " + TABLE_NAME_FAVORITES +" (" +
            LauncherSettings._ID + " INTEGER PRIMARY KEY," +
            LauncherSettings.TITLE + " TEXT," +
            LauncherSettings.INTENT + " TEXT," +
            LauncherSettings.PAGE + " INTEGER," +
            LauncherSettings.CELL_POS + " INTEGER," +
            LauncherSettings.ITEM_TYPE + " INTEGER," +
            LauncherSettings.ICON_TYPE + " INTEGER," +
            LauncherSettings.ICON_PACKAGE + " TEXT," +
            LauncherSettings.ICON + " BLOB" +
            ");");
        loadFavorites(db, R.xml.default_desktop_settings);
        loadAllApps(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}

	private int loadAllApps(SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		PackageManager pm = mContext.getPackageManager();
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
        sortList(apps, new TitleComparator(pm));
        IDesktopPage.DesktopPos dp = getFirstDp();
        for(ResolveInfo info : apps) {
        	if(info.activityInfo.applicationInfo.packageName.equals(mContext.getApplicationContext().getPackageName()))
        		continue;
        	values.clear();
        	values.put(LauncherSettings._ID, generateNewId());
        	values.put(LauncherSettings.TITLE, getAppTitleFromResolveInfo(info, pm));
        	values.put(LauncherSettings.INTENT, getAppIntentFromResolveInfo(info).toUri(0));
        	values.put(LauncherSettings.PAGE, dp.getPage());
        	values.put(LauncherSettings.CELL_POS, dp.getPos());
//        	Log.d(TAG, "pos = " + dp.pos);
        	values.put(LauncherSettings.ITEM_TYPE, LauncherSettings.ItemType.TYPE_SHORTCUT);
        	values.put(LauncherSettings.ICON_TYPE, LauncherSettings.ICON_TYPE_RESOURCE);
        	values.put(LauncherSettings.ICON_PACKAGE, info.activityInfo.applicationInfo.packageName);
        	db.insert(TABLE_NAME_FAVORITES, null, values);
//        	Log.d(TAG, values.toString());
        	dp.moveToNext();
        	while(!checkValidityOfDp(dp)) {
        		Log.d(TAG, "page = " + dp.getPage() + ", pos = " + dp.getPos());
        		dp.moveToNext();
        	}
        }
        return apps.size();
	}
	
	private DesktopPos getFirstDp() {
		DesktopPos dp = new DesktopPos(0, 0);
		if(this.mSba == null) {
			return dp;
		}
		else {
			while(!checkValidityOfDp(dp)) {
				dp.moveToNext();
			}
			return dp;
		}
	}
	
	private boolean checkValidityOfDp(DesktopPos dp) {
		return this.mSba == null || !this.mSba.get(dp.getId(), false);
	}
	
	public static <T> void sortList(List<T> list, Comparator<? super T> comp) {
		Collections.sort(list, comp);
	}
	
	private static String getAppTitleFromResolveInfo(ResolveInfo info, PackageManager pm) {
		String title = info.loadLabel(pm).toString();
		if(title == null) {
			title = info.activityInfo.name;
		}
		return title;
	}
	
	public static Intent getAppIntentFromResolveInfo(ResolveInfo info) {
		ActivityInfo activityInfo = info.activityInfo;
		ComponentName cn = new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
	            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		intent.setComponent(cn);
		return intent;
	}
	
	private int loadFavorites(SQLiteDatabase db, int resourceId) {
		this.mSba = new SparseBooleanArray();
        XmlResourceParser parser = mContext.getResources().getXml(resourceId);
        AttributeSet attr = Xml.asAttributeSet(parser);
        ContentValues values = new ContentValues();
        PackageManager pm = mContext.getPackageManager();
        int count = 0;
        try {
            int eventType = parser.getEventType();
            while(eventType != XmlResourceParser.END_DOCUMENT) {
                if(eventType == XmlResourceParser.START_TAG) {
                    String name = parser.getName();
                    if(TAG_FAVORITE.equals(name)) {
                        TypedArray ta = mContext.obtainStyledAttributes(attr, R.styleable.Favorite);
                        ComponentName cn = new ComponentName(ta.getString(R.styleable.Favorite_packageName),
                                ta.getString(R.styleable.Favorite_className));
                        ActivityInfo info = pm.getActivityInfo(cn, 0);
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        intent.setComponent(cn);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        values.clear();
                        values.put(LauncherSettings._ID, generateNewId());
                        int page = Integer.valueOf(ta.getString(R.styleable.Favorite_page));
                        int pos = Integer.valueOf(ta.getString(R.styleable.Favorite_pos));
                        values.put(LauncherSettings.PAGE, page);
                        values.put(LauncherSettings.CELL_POS, pos);
                        mSba.put(DesktopPos.generateUniqueID(page, pos), true);
                        values.put(LauncherSettings.INTENT, intent.toUri(0));
                        values.put(LauncherSettings.TITLE, info.loadLabel(pm).toString());
                        values.put(LauncherSettings.ITEM_TYPE, LauncherSettings.ItemType.TYPE_SHORTCUT);
                        values.put(LauncherSettings.ICON_TYPE, LauncherSettings.ICON_TYPE_RESOURCE);
                        values.put(LauncherSettings.ICON_PACKAGE, ta.getString(R.styleable.Favorite_packageName));
                        values.put(LauncherSettings._ID, generateNewId());
                        db.insert(TABLE_NAME_FAVORITES, null, values);
                        count++ ;
                        ta.recycle();
                    }
                }
                parser.next();
                eventType = parser.getEventType();
            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return count;
    }
    
    public static class TitleComparator implements Comparator<ResolveInfo> {
    	PackageManager pm;
    	public TitleComparator(PackageManager pm) {
    		this.pm = pm;
    	}
		@Override
		public int compare(ResolveInfo arg0, ResolveInfo arg1) {
			String title0 = arg0.loadLabel(pm).toString();
			String title1 = arg1.loadLabel(pm).toString();
			return title0.compareTo(title1);
		}
    	
    }
    
}
