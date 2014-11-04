package com.hxq.test;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

public class Utilities {
	public static final String TAG = "Utilities TAG";
	public static int ICONSIZE_MDPI = 48;
	private static float sIconBitmapSize;
//	private static final int sIconSizeAdjustThrd = 5; 
	
	public static Bitmap createIconBitmapFromDrawable(Drawable d, Context context) {
		Resources res = context.getResources();
		boolean customIconSize = res.getBoolean(R.bool.use_custom_icon_size);
		if(customIconSize) {
			sIconBitmapSize = res.getDimension(R.dimen.app_icon_size);
		}
		else {
			sIconBitmapSize = getIconSizeByDpi(context);
		}
//		Log.d(TAG, "iconsize = " + sIconBitmapSize);
//		Log.d(TAG, "rawIconSize =" + d.getIntrinsicWidth());
		Paint p = new Paint();
		p.setAntiAlias(true);
		Bitmap iconBitmap = getBitmapFromDrawable(d);
		return iconBitmap;
	}
	
	private static int getIconSizeByDpi(Context context) {
		int screenDpi = context.getResources().getDisplayMetrics().densityDpi;
		int mediumDpi = DisplayMetrics.DENSITY_MEDIUM;
		return ICONSIZE_MDPI * screenDpi / mediumDpi;
	}
	
	public static Bitmap getBitmapFromDrawable(Drawable d) {
		//for icon's drawable d is always a bitmapdrawable
    	if(d instanceof BitmapDrawable) {
    		Bitmap origin = ((BitmapDrawable) d).getBitmap();
    		return Bitmap.createScaledBitmap(origin, (int)sIconBitmapSize, (int)sIconBitmapSize, true);
    	}
    	//here never use, actually following code doesn't work well, it doesn't scale the bitmap
    	Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Config.ARGB_8888);
    	Canvas canvas = new Canvas(b);
    	d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    	d.draw(canvas);
    	return b;
    }
}
