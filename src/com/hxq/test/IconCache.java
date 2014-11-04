package com.hxq.test;

import java.util.HashMap;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

public class IconCache {
	
	public static final String TAG = "IconCache TAG";
	
	private static final int DEFAULT_MAP_CAPACITY = 50;
//	private Context mContext;
	private Application mApplication;
	private PackageManager mPacManager;
	static Bitmap mDefaultIcon;
	private int mIconDpi;
	
	private HashMap<ComponentName, CacheEntry> mCache = new HashMap<ComponentName, CacheEntry>(DEFAULT_MAP_CAPACITY);
	
	public static class CacheEntry {
		Bitmap icon;
		String title;
	}
	
	public IconCache(LauncherApplication mApplication) {
		this.mApplication = mApplication;
		this.mPacManager = mApplication.getPackageManager();
		mIconDpi = mApplication.getResources().getDisplayMetrics().densityDpi;
		dumpDpiInfo();
		IconCache.mDefaultIcon = makeDefaultIcon();
	}
	
	private void dumpDpiInfo() {
		switch(mIconDpi) {
		case DisplayMetrics.DENSITY_LOW:
			Log.d(TAG, "Density Low" + ",Density = " + mIconDpi);
			break;
		case DisplayMetrics.DENSITY_MEDIUM:
			Log.d(TAG, "Density Medium" + ",Density = " + mIconDpi);
			break;
		case DisplayMetrics.DENSITY_HIGH:
			Log.d(TAG, "Density High" + ",Density = " + mIconDpi);
			break;
		case DisplayMetrics.DENSITY_XHIGH:
			Log.d(TAG, "Density XHigh" + ",Density = " + mIconDpi);
			break;
			default :
				Log.d(TAG, "DensityOther" + ",Density = " + mIconDpi);
		}
	}
	private Bitmap makeDefaultIcon() {
		Resources res = mApplication.getResources();
		return BitmapFactory.decodeResource(res, R.drawable.ic_launcher);
	}
	
	public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(),
                R.drawable.ic_launcher);
    }

    public Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }

        return (d != null) ? d : getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(String packageName, int iconId) {
        Resources resources;
        try {
            resources = mPacManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }
    
    public Drawable getFullResIcon(ResolveInfo info) {
        Resources resources;
        try {
            resources = mPacManager.getResourcesForApplication(
                    info.activityInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.activityInfo.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }
    
    public void remove(ComponentName cn) {
    	synchronized(mCache) {
    		mCache.remove(cn);
    	}
    }
    
    public void flush() {
    	synchronized (mCache) {
    		mCache.clear();
    	}
    }
    
    private CacheEntry cacheLocked(ComponentName cn, ResolveInfo info) {
    	CacheEntry entry = mCache.get(cn);
    	if(entry == null) {
    		entry = new CacheEntry();
    		mCache.put(cn, entry);
    		entry.title = info.loadLabel(mPacManager).toString();
    		if(entry.title == null) {
    			entry.title = info.activityInfo.name;
    		}
    		entry.icon = Utilities.createIconBitmapFromDrawable(getFullResIcon(info), mApplication);
    	}
    	return entry;
    }
    
    public static Bitmap getBitmapFromDrawable(Drawable d) {
    	if(d instanceof BitmapDrawable) {
    		return ((BitmapDrawable) d).getBitmap();
    	}
    	Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Config.ARGB_8888);
    	Canvas canvas = new Canvas(b);
    	d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    	d.draw(canvas);
    	return b;
    }
    
    public void getTitleAndIcon(ShortcutInfo shortcut, ResolveInfo info) {
    	synchronized (mCache) {
    		CacheEntry entry = cacheLocked(shortcut.getComponentName(), info);
    		shortcut.setTitle(entry.title);
    		shortcut.setIcon(entry.icon);
    	}
    }
    
	public Bitmap getIcon(ComponentName component, ResolveInfo resolveInfo) {
		synchronized (mCache) {
			if (resolveInfo == null || component == null) {
				return null;
			}
			CacheEntry entry = cacheLocked(component, resolveInfo);
			return entry.icon;
		}
	}
	
	public Bitmap getIcon(Intent intent) {
        synchronized (mCache) {
            final ResolveInfo resolveInfo = mPacManager.resolveActivity(intent, 0);
            ComponentName component = intent.getComponent();

            if (resolveInfo == null || component == null) {
                return mDefaultIcon;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo);
            return entry.icon;
        }
    }
}
