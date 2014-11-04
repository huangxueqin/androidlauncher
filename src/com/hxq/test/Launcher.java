package com.hxq.test;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class Launcher extends Activity implements OnClickListener, OnLongClickListener, LauncherModel.Callback{
	

	public static final String TAG = "Launcher LOG";
	
	DragView view;
	DragLayer mDragLayer;
	Workspace mWorkspace;
	LauncherModel mModel;
	IconCache mIconCache;
	
	LayoutInflater mInflater;
	Handler mHandler;
	//using when loading icons
	ProgressDialog mProDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate executes");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.launcher);
		LauncherApplication app = (LauncherApplication)getApplication();
		mModel = app.getLauncherModel();
		mIconCache = app.getIconCache();
		mInflater = getLayoutInflater();
		initViews(app);
		mModel.setCallback(this);
		mModel.startLoader(this, true);
		if(mProDialog == null) {
			mProDialog = new ProgressDialog(this);
			mProDialog.setMessage("loading...");
			mProDialog.setIndeterminate(false);
			mProDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProDialog.setCancelable(true);
		}
		mProDialog.show();
	}

	private void initViews(LauncherApplication app) {
		mDragLayer = (DragLayer)findViewById(R.id.id_draglayer);
		mWorkspace = (Workspace)findViewById(R.id.id_workspace);
		mWorkspace.setDragLayer(mDragLayer);
		mWorkspace.setLayoutInflater(mInflater);
		mWorkspace.setLauncherApplication(app);
		mWorkspace.setOnLongClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onClick running");
		if(v instanceof IDesktopPageItem) {
			IDesktopPageItem item = (IDesktopPageItem)v;
			ItemInfo info = item.getCorrespondItemInfo();
			final int itemType = info.itemType;
			switch(itemType) {
			case LauncherSettings.ItemType.TYPE_SHORTCUT:
				ShortcutInfo shortcut = (ShortcutInfo)info;
				Intent intent = shortcut.getIntent();
				this.startActivity(intent);
				break;
			}
		}
	}
	
	@Override
	public boolean onLongClick(View view) {
		Log.d(TAG, "onLongClick running");
		IDesktopPage page;
		if(view instanceof CellLayout) {
			page = (IDesktopPage)view;
		}
		else {
			page= (IDesktopPage)view.getParent().getParent();
		}
		IDesktopPage.CellInfo info = page.getTouchCellInfo();
		if(info.cell == null) {
			return false;
		}
		mWorkspace.startDrag(info);
		return true;
	}

	@Override
	public boolean setLoadOnResume() {
		// TODO Auto-generated method stub
		return false;
	}

	public IDesktopPageItem createDesktopPageItem(ItemInfo info) {
		IDesktopPageItem pageItem = null;
		if(info.itemType == LauncherSettings.ItemType.TYPE_SHORTCUT) {
			ShortcutInfo shortcut = (ShortcutInfo)info;
			pageItem = BubbleTextView.getIconViewFromResource(mInflater, R.layout.iconview, 
					null,
					shortcut, mIconCache);
			pageItem.getView().setOnClickListener(this);
		}
		
		return pageItem;
	}
	
	@Override
	public void bindItems(ArrayList<ItemInfo> unBindItems, int start, int end) {
		for(int i=start;i<end;i++) {
			ItemInfo info = unBindItems.get(i);
			if(info instanceof ShortcutInfo) {
				IDesktopPageItem item = createDesktopPageItem(info);
				if(item != null) {
					mWorkspace.addItemOnDesktop(item, false);
				}
			}
		}
	}

	@Override
	public void bindItemsUpdate(ArrayList<ItemInfo> apps) {
		
	}

	@Override
	public void bindItemsAdded(ArrayList<ItemInfo> apps) {
		if(apps.size() == 0) {
			return;
		}
		Workspace.DesktopPosition dp = null;
		for(ItemInfo itemInfo : apps) {
			if(dp == null) {
				dp = mWorkspace.getUnOccupiedPosition();
			}
			else {
				dp.moveToNextPositionUnBounded();
			}
			IDesktopPageItem pageItem = createDesktopPageItem(itemInfo);
			if(itemInfo.itemType == LauncherSettings.ItemType.TYPE_SHORTCUT) {
				ShortcutInfo shortcut = (ShortcutInfo) itemInfo;
				shortcut.page = dp.pageIndex;
				shortcut.position = dp.positionIndex;
				if(pageItem != null) {
					mWorkspace.addItemOnDesktop(pageItem, false);
				}
				shortcut.id = ((LauncherApplication)getApplication()).getLauncherProvider().generatorNewId();
				mModel.addItemIntoDatabase(this, shortcut.id, shortcut.getTitle(), shortcut.getIntent(),
						shortcut.itemType, dp.pageIndex, dp.positionIndex, false);
			}
		}
	}

	@Override
	public void bindItemRemoved(ArrayList<ItemInfo> apps) {
		for(ItemInfo info : apps) {
			int pageIndex = info.page;
			int positionIndex = info.position;
			IDesktopPage page = (IDesktopPage)mWorkspace.getChildAt(pageIndex);
			page.removeItemOnPosition(positionIndex);
		}
		for(ItemInfo info : apps) {
			long id = info.id;
			mModel.deleteItemFromDatabase(this, id, false);
		}
	}

	@Override
	public void startBinding() {
		mWorkspace.clearAllItemsOnDesktop();
	}

	@Override
	public void bindCompleted() {
		ViewGroup parent = (ViewGroup)mWorkspace.getParent();
		PageIndicator indicator = new PageIndicator(this, mWorkspace.getPageNum());
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT);
		lp.gravity=Gravity.BOTTOM | Gravity.CENTER;
		lp.bottomMargin = 50;
		parent.addView(indicator, lp);
		mWorkspace.setPageSwitchListener(indicator.scrollListener);
		mProDialog.dismiss();
	}

}