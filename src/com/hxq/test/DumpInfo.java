package com.hxq.test;

import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;

public class DumpInfo {
	
	/**
	 * does not support multi-touch
	 * @param ev
	 */
	public static void dumpActionInfo(String TAG, MotionEvent ev) {
		switch(ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "action_down");
			break;
		case MotionEvent.ACTION_MOVE:
			Log.d(TAG, "action_move");
			break;
		case MotionEvent.ACTION_CANCEL:
			Log.d(TAG, "action_cancel");
			break;
		case MotionEvent.ACTION_UP:
			Log.d(TAG, "action_up");
			break;
		default:
			Log.d(TAG, "other touch action");
			break;
		}
	}
	
	public static void dumpBroadcastReceiverInfo(String TAG, Intent intent) {
		final String action = intent.getAction();
		if(action.equals(Intent.ACTION_PACKAGE_ADDED)) {
			Log.d(TAG, "action package added");
			Log.d(TAG, "pacakgeName: " + intent.getData().getSchemeSpecificPart());
		}
		else if(action.equals(Intent.ACTION_PACKAGE_CHANGED)) {
			Log.d(TAG, "action package changed");
			Log.d(TAG, "pacakgeName: " + intent.getData().getSchemeSpecificPart());
		}
		else if(action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
			Log.d(TAG, "action package removed");
			Log.d(TAG, "pacakgeName: " + intent.getData().getSchemeSpecificPart());
		}
	}
}
