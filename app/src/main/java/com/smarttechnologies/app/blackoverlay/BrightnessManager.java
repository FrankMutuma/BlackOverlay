package com.smarttechnologies.app.blackoverlay;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

public class BrightnessManager {
	private static final String TAG = "BrightnessManager";
	private static final int SYSTEM_BRIGHTNESS_MIN = 1;
	private static final float WINDOW_BRIGHTNESS_ABSOLUTE_MIN = 0.00f;

	private Context context;
	private WindowManager.LayoutParams overlayParams;

	private int originalSystemBrightnessValue = -1;
	private int originalSystemBrightnessMode = -1;
	public boolean isSystemBrightnessControlled = false;

	public BrightnessManager(Context context) {
		this.context = context.getApplicationContext();
	}

	// Call this method before applying brightness to set the target overlay params
	public void setOverlayParams(WindowManager.LayoutParams params) {
		this.overlayParams = params;
	}

	public void applyCombinedBrightness() {
		Log.d(TAG, "Applying combined brightness control.");
		applySystemBrightnessOnly();
		applyInAppWindowBrightness();
	}

	public void applySystemBrightnessOnly() {
		ContentResolver cResolver = context.getContentResolver();
		try {
			// Save original values
			originalSystemBrightnessValue = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
			originalSystemBrightnessMode = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);

			// Switch to manual mode if needed
			if (originalSystemBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
				Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
						Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			}
			// Set to min
			Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, SYSTEM_BRIGHTNESS_MIN);
			isSystemBrightnessControlled = true;

		} catch (Exception e) {
			Log.e(TAG, "Failed to control system brightness: " + e.getMessage());
			isSystemBrightnessControlled = false;
		}
	}

	public void applyInAppWindowBrightness() {
		if (overlayParams != null) {
			overlayParams.screenBrightness = WINDOW_BRIGHTNESS_ABSOLUTE_MIN;
			Log.d(TAG, "Window brightness set to: " + WINDOW_BRIGHTNESS_ABSOLUTE_MIN);
		} else {
			Log.e(TAG, "Cannot set window brightness: overlayParams is null");
		}
	}

	public void restoreBrightness() {
		restoreInAppWindowBrightness();
		if (isSystemBrightnessControlled) {
			ContentResolver cResolver = context.getContentResolver();
			try {
				if (originalSystemBrightnessMode != -1) {
					Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
							originalSystemBrightnessMode);
				}
				if (originalSystemBrightnessValue != -1) {
					Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, originalSystemBrightnessValue);
				}
				isSystemBrightnessControlled = false;
			} catch (SecurityException e) {
				Log.e(TAG, "Failed to restore system brightness: " + e.getMessage());
			}
		}
	}

	private void restoreInAppWindowBrightness() {
		if (overlayParams != null) {
			overlayParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
		}
	}

	public boolean canWriteSystemSettings() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			return Settings.System.canWrite(context);
		}
		return true;
	}
}