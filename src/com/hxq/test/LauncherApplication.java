package com.hxq.test;

import java.lang.ref.WeakReference;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;

public class LauncherApplication extends Application {
	public static final String TAG = "LauncherApplication TAG";
	
	private IconCache mIconCache;
	private LauncherModel mModel;
	WeakReference<LauncherProvider> providerRef;
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mIconCache = new IconCache(this);
		mModel = new LauncherModel(this, mIconCache);
		
		//register actions for install and remove packages
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addDataScheme("package");
		this.registerReceiver(mModel, filter);
		
		ContentResolver resolver = getContentResolver();
		resolver.registerContentObserver(LauncherSettings.CONTENT_URI, true, mFavoritesObserver);
	}
	
	public void setLauncherProvider(LauncherProvider provider) {
		providerRef = new WeakReference<LauncherProvider>(provider);
	}
	
	public LauncherProvider getLauncherProvider() {
		return providerRef.get();
	}

	private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mModel.startLoader(LauncherApplication.this, true);
        }
    };
	
	public IconCache getIconCache() {
		return mIconCache;
	}
	
	public LauncherModel getLauncherModel() {
		return mModel;
	}
}
