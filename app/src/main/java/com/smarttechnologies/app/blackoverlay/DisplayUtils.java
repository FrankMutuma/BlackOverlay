package com.smarttechnologies.app.blackoverlay;

import android.content.Context;

public class DisplayUtils {

	public static int dpToPx(Context context, int dp) {
		float density = context.getResources().getDisplayMetrics().density;
		return Math.round(dp * density);
	}

	public static int pxToDp(Context context, int px) {
		float density = context.getResources().getDisplayMetrics().density;
		return Math.round(px / density);
	}
}