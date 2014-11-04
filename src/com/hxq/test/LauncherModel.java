package com.hxq.test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class LauncherModel extends BroadcastReceiver{
	public static final String TAG = "LauncherModel TAG";
	public static final String TAG_BR = "LauncherModelReceiver TAG";
	
	private static final int ACTION_NONE = -1;
	private static final int ACTION_PACKAGE_ADDED = 0;
	private static final int ACTION_PACKAGE_REMOVED = 1;
	private static final int ACTION_PACKAGE_CHANGED = 2;
	
	private static final HandlerThread sWorkThread = new HandlerThread("launcherModelWorkThread");
	static {
        sWorkThread.start();
    }
	
    private static final Handler sWorker = new Handler(sWorkThread.getLooper());
    
    private LauncherApplication mApplication;
    private AllAppsList mAllApps;
    private IconCache mIconCache;

    private DeferredHandler mHandler = new DeferredHandler();
    private WeakReference<Callback> mCallback;
    private LoadTask mLoadTask;
    private boolean mAllAppsLoaded = false;
    private ArrayList<ItemInfo> tempBindItems = null;
    private static int mCellCountX;
    private static int mCellCountY;
    private static int BATCH_NUM = CellLayout.CELL_COUNT_X;
//    private static int mAllAppsLoadDelay = 10;

    private Object mLock = new Object();

    @Override
    public void onReceive(Context context, Intent intent) {
    	DumpInfo.dumpBroadcastReceiverInfo(TAG_BR, intent);
    	final String action = intent.getAction();
    	if(action.equals(Intent.ACTION_PACKAGE_ADDED) || action.equals(Intent.ACTION_PACKAGE_CHANGED) ||
    			action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
    		final String packageName = intent.getData().getSchemeSpecificPart();
    		int actionType = ACTION_NONE;
    		if(action.equals(Intent.ACTION_PACKAGE_ADDED)) {
    			actionType = ACTION_PACKAGE_ADDED;
    		}
    		else if(action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
    			actionType = ACTION_PACKAGE_REMOVED;
    		}
    		else {
    			actionType = ACTION_PACKAGE_CHANGED;
    		}
    		mHandler.post(new BroadcastReceiveTask(packageName, actionType));
    	}
    }
    
    private class BroadcastReceiveTask implements Runnable {
    	int actionType;
    	String pkgName;
    	public BroadcastReceiveTask(final String packageName, int actionType) {
    		this.pkgName = packageName;
    		this.actionType = actionType;
    	}
		@Override
		public void run() {
			ArrayList<ItemInfo> optionShortcuts = new ArrayList<ItemInfo>();
			switch(actionType){
				case ACTION_PACKAGE_ADDED:
					List<ResolveInfo> rslvInfos = AllAppsList.findActivitiesForPackage(mApplication, pkgName);
					PackageManager pm = mApplication.getPackageManager();
					for(ResolveInfo info : rslvInfos) {
						if(info.activityInfo != null) {
							ShortcutInfo shortcut = new ShortcutInfo(pm, info, mApplication.getIconCache());
							shortcut.itemType = LauncherSettings.ItemType.TYPE_SHORTCUT;
							optionShortcuts.add(shortcut);
							mAllApps.add(shortcut);
						}
					}
					break;
				case ACTION_PACKAGE_REMOVED:
					mAllApps.removePackage(pkgName);
					for(ShortcutInfo shortcut : mAllApps.removed) {
						String packageName = shortcut.getComponentName().getPackageName();
						if(packageName.equals(pkgName)) {
							optionShortcuts.add(shortcut);
						}
					}
					break;
				case ACTION_PACKAGE_CHANGED:
					;
					break;
				default:
					;
					break;
			}
			if(mCallback != null) {
				Callback cbk = mCallback.get();
				if(cbk != null) {
					switch(actionType) {
						case ACTION_PACKAGE_ADDED:
							cbk.bindItemsAdded(optionShortcuts);
							break;
						case ACTION_PACKAGE_REMOVED:
							cbk.bindItemRemoved(optionShortcuts);
							break;
						case ACTION_PACKAGE_CHANGED:
							;
							break;
						default:
							;
							break;
					}
				}
			}
		}
    	
    }
    
    public interface Callback {
        public boolean setLoadOnResume();
        public void bindItems(final ArrayList<ItemInfo> shortcuts, int start, int end);
        public void bindItemsUpdate(ArrayList<ItemInfo> apps);
        public void bindItemsAdded(ArrayList<ItemInfo> apps);
        public void bindItemRemoved(ArrayList<ItemInfo> apps);
        public void startBinding();
        public void bindCompleted();
    }

    public LauncherModel(LauncherApplication app, IconCache iconCache) {
        mApplication = app;
        mIconCache = iconCache;
        mAllApps = new AllAppsList(mIconCache);
    }

    public void setCallback(Callback cbk) {
    	synchronized (mLock) {
            mCallback = new WeakReference<Callback>(cbk);
        }
    }
    
    
    public void addItemIntoDatabase(Context context, long id, String title, Intent intent,
    		int itemType, int page, int position, boolean notify) {
    	final ContentResolver resolver = context.getContentResolver();
    	final ContentValues values = new ContentValues();
    	final Uri uri = notify ? LauncherSettings.CONTENT_URI_NOTIFY : LauncherSettings.CONTENT_URI;
    	
    	values.put(LauncherSettings._ID, id);
    	values.put(LauncherSettings.TITLE, title);
    	values.put(LauncherSettings.INTENT, intent.toUri(0));
    	values.put(LauncherSettings.PAGE, page);
    	values.put(LauncherSettings.CELL_POS, position);
    	values.put(LauncherSettings.ITEM_TYPE, itemType);
    	values.put(LauncherSettings.ICON_TYPE, LauncherSettings.ICON_TYPE_RESOURCE);
    	values.put(LauncherSettings.ICON_PACKAGE, intent.getComponent().getPackageName());
    	sWorker.post(new Runnable() {
			@Override
			public void run() {
				resolver.insert(uri, values);
			}
    		
    	});
    }
    
    public void deleteItemFromDatabase(Context context, final long id, final boolean notify) {
    	final ContentResolver cr = context.getContentResolver();
    	sWorker.post(new Runnable() {
			@Override
			public void run() {
				final Uri uri = notify ? LauncherSettings.CONTENT_URI_NOTIFY : LauncherSettings.CONTENT_URI;
				String where = "_id=?";
				String[] args = {String.valueOf(id)};
				cr.delete(uri, where, args);
			}
    		
    	});
    }
    
    public void moveItemInDatabase(Context context, final long id, final int page, final int position, final boolean notify) {
    	final ContentResolver cr = context.getContentResolver();
    	sWorker.post(new Runnable () {

			@Override
			public void run() {
				final Uri uri = notify ? LauncherSettings.CONTENT_URI_NOTIFY : LauncherSettings.CONTENT_URI;
				String where = "_id=?";
				String[] args = {String.valueOf(id)};
				ContentValues values = new ContentValues();
				values.put(LauncherSettings.PAGE, page);
				values.put(LauncherSettings.CELL_POS, position);
				cr.update(uri, values, where, args);
			}
    		
    	});
    }
    
    public void startLoader(Context context, boolean needReload) {
        synchronized (mLock) {
        	Log.d(TAG, "startLoader running");
            if (mCallback != null && mCallback.get() != null) {
                stopLoader();
                mLoadTask = new LoadTask(context, needReload);
                sWorker.post(mLoadTask);
            }
        }
    }

    public void stopLoader() {
        synchronized (mLock) {
            if(mLoadTask != null) {
                mLoadTask.stopLocked();
                mLoadTask = null;
            }
        }
    }

    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, Context context,
                                        Cursor c, int iconIndex, int titleIndex) {
        Bitmap icon = null;
        final ShortcutInfo info = new ShortcutInfo();
        ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            return null;
        }
        try {
            PackageInfo pi = manager.getPackageInfo(componentName.getPackageName(), 0);
            if (!pi.applicationInfo.enabled) {
                return null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(Launcher.TAG, "getPackInfo failed for package " + componentName.getPackageName());
        }
        
        /**
         * set up ShortcutInfo
         */
        info.setActivity(componentName, 0);
        final ResolveInfo resolveInfo = manager.resolveActivity(intent, 0);
        if (resolveInfo != null) {
            icon = mIconCache.getIcon(componentName, resolveInfo);
        }
        // the db
        if (icon == null) {
            if (c != null) {
                icon = getIconFromCursor(c, iconIndex, context);
            }
        }
        // the fall back icon
        if (icon == null) {
            icon = getFallbackIcon();
        }
        info.setIcon(icon);

        // from the resource
        if (resolveInfo != null) {
            info.setTitle(resolveInfo.activityInfo.loadLabel(manager).toString());
        }
        // from the db
        if (info.getTitle() == null) {
            if (c != null) {
                info.setTitle(c.getString(titleIndex));
            }
        }
        // fall back to the class name of the activity
        if (info.getTitle() == null) {
            info.setTitle(componentName.getClassName());
        }
        return info;
    }

    Bitmap getIconFromCursor(Cursor c, int iconIndex, Context context) {
        byte[] data = c.getBlob(iconIndex);
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            return null;
        }
    }

    public Bitmap getFallbackIcon() {
        return Bitmap.createBitmap(IconCache.mDefaultIcon);
    }

    static void updateDesktopLayoutCells(int cellCountX, int cellCountY) {
        mCellCountX = cellCountX;
        mCellCountY = cellCountY;
    }

    static int getCellCountX() {
        return mCellCountX;
    }

    static int getCellCountY() {
        return mCellCountY;
    }

    static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
        if (info.activityInfo != null) {
            return new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
        } else {
            return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
        }
    }

    public class LoadTask implements Runnable {

        private Context mContext;
        private boolean mStopped;
        private boolean mNeedReload;

        public LoadTask(Context context) {
            this(context, false);
        }
        
        private LoadTask(Context context, boolean reload) {
        	mContext = context;
        	mStopped = false;
        	mNeedReload = reload;
        }
        
        @Override
		public void run() {
        	int origPriority = android.os.Process.getThreadPriority(android.os.Process.myTid());
        	Log.d(TAG, "LoadTask run running");
			synchronized (mLock) {
				android.os.Process.setThreadPriority(origPriority > android.os.Process.THREAD_PRIORITY_DEFAULT ? 
						android.os.Process.THREAD_PRIORITY_DEFAULT : origPriority);
			}
			
			loadAndBindDesktop();
			
			synchronized (mLock) {
				android.os.Process
						.setThreadPriority(origPriority);
			}
			mContext = null;
			synchronized (mLock) {
				if (mLoadTask == this)
					mLoadTask = null;
			}
		}

        public void stopLocked() {
            synchronized (LoadTask.this) {
                if (!mStopped) {
                    mStopped = true;
                }
            }
        }

        private void loadAndBindDesktop() {
        	Log.d(TAG, "loadAndBindDesktop running");
            if (mNeedReload || !mAllAppsLoaded) {
                loadDesktop();
                synchronized (LoadTask.this) {
                    if (mStopped) {
                        return;
                    }
                    mAllAppsLoaded = true;
                }
            }
            BindDesktop();
        }
        
        private void loadDesktop() {
        	mAllApps.onlyClearAllApps();
        	PackageManager pm = mContext.getPackageManager();
        	ContentResolver resolver = mContext.getContentResolver();
        	Log.d(TAG, "start Query");
        	final Cursor cursor = resolver.query(LauncherSettings.CONTENT_URI, new String[] {LauncherSettings._ID, 
        			LauncherSettings.TITLE, LauncherSettings.INTENT, LauncherSettings.PAGE, LauncherSettings.CELL_POS, 
        			LauncherSettings.ITEM_TYPE, LauncherSettings.ICON }
        	, null, null, null);
        	try {
        		final int idIndex = cursor.getColumnIndexOrThrow(LauncherSettings._ID);
        		final int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.TITLE);
        		final int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.INTENT);
        		final int pageIndex = cursor.getColumnIndexOrThrow(LauncherSettings.PAGE);
        		final int positionIndex = cursor.getColumnIndexOrThrow(LauncherSettings.CELL_POS);
        		final int itemTypeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.ITEM_TYPE);
        		final int iconIndex = cursor.getColumnIndexOrThrow(LauncherSettings.ICON);
        		while(!mStopped && cursor.moveToNext()) {
        			//by now the flags parameter is 0
        			Intent intent = Intent.parseUri(cursor.getString(intentIndex), 0);
        			long id = cursor.getLong(idIndex);
        			int itemType = cursor.getInt(itemTypeIndex);
        			int page = cursor.getInt(pageIndex);
        			int position = cursor.getInt(positionIndex);
        			ShortcutInfo shortcut = getShortcutInfo(pm, intent, mContext, cursor, iconIndex, titleIndex);
        			if(shortcut != null) {
        				shortcut.id = id;
	        			shortcut.page = page;
	        			shortcut.position = position;
	        			shortcut.itemType = itemType;
	        			mAllApps.add(shortcut);
//	        			Log.d(TAG, shortcut.toString());
        			}
        		}
        		
        	} catch(Exception e) {
        		Log.e(TAG, "LoadTask: Exception happens when try to load all apps from database");
        		e.printStackTrace();
        	}
        }
        
		@SuppressWarnings("unchecked")
		private void BindDesktop() {
            final Callback cbk = mCallback.get();
            if(cbk == null) {
                return;
            }
			tempBindItems = (ArrayList<ItemInfo>) mAllApps.data.clone();
            int appNum = tempBindItems.size();
            int startIndex = 0;
            int unbindAppNum = appNum - startIndex;
            while(unbindAppNum > 0 && !mStopped) {
            	int batch = unbindAppNum > BATCH_NUM ? BATCH_NUM : unbindAppNum;
            	BindItemRunnable bindItemRunnable = new BindItemRunnable(cbk, tempBindItems, startIndex,
            			startIndex + batch);
            	if(startIndex == 0) {
            		bindItemRunnable.setStartMark(true);
            	}
            	unbindAppNum -= batch;
            	if(unbindAppNum <= 0) {
            		bindItemRunnable.setFinalMark(true);
            	}
            	mHandler.post(bindItemRunnable);
            	startIndex += batch;
            }
        }
        
        private class BindItemRunnable implements Runnable {

        	boolean isBindStart;
        	boolean isBindFinished;
        	Callback cbk;
        	ArrayList<ItemInfo> itemsToBind;
        	int start;
        	int end;
        	public BindItemRunnable(Callback cbk, ArrayList<ItemInfo> items, int start, int end, boolean bindStart,
        			boolean bindFinished) {
        		this.isBindStart = bindStart;
        		this.isBindFinished = bindFinished;
        		this.cbk = cbk;
        		this.itemsToBind = items;
        		this.start = start;
        		this.end = end;
        	}
        	public BindItemRunnable(Callback cbk, ArrayList<ItemInfo> items, int start, int end) {
        		this(cbk, items, start, end, false, false);
        	}
        	
        	public void setFinalMark(boolean bindFinished) {
        		this.isBindFinished = bindFinished;
        	}
        	
        	public void setStartMark(boolean bindStart) {
        		this.isBindStart = bindStart;
        	}
        	
			@Override
			public void run() {
				if(isBindStart) {
					onBindTaskStarted();
				}
				cbk.bindItems(itemsToBind, start, end);
				if(isBindFinished) {
					onBindTaskCompleted();
				}
			}
			
			private void onBindTaskStarted() {
				cbk.startBinding();
			}
        	
			private void onBindTaskCompleted() {
	        	cbk.bindCompleted();
	        	tempBindItems = null;
	        }
			
        }
        
        

        Callback tryGetCallbacks(Callback oldCallback) {
            synchronized (mLock) {
                if (mStopped) {
                    return null;
                }

                if (mCallback == null) {
                    return null;
                }

                final Callback callback = mCallback.get();
                if (callback != oldCallback) {
                    return null;
                }
                if (callback == null) {
                    Log.w(Launcher.TAG, "no mCallbacks");
                    return null;
                }

                return callback;
            }
        }

    }
}
