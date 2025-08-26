package com.smarttechnologies.app.blackoverlay;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class BrightnessManager {
	private static final String TAG = "BrightnessManager";
	private static final int SYSTEM_BRIGHTNESS_MIN = 1;
	private static final float WINDOW_BRIGHTNESS_ABSOLUTE_MIN = 0.00f;

	private Context context;
	private Window window;

	private int originalSystemBrightnessValue = -1;
	private int originalSystemBrightnessMode = -1;
	private float originalWindowBrightness = -2.0f;
	public boolean isSystemBrightnessControlled = false;

	public BrightnessManager(Context context, Window window) {
		this.context = context.getApplicationContext();
		this.window = window;
		// Capture original window brightness on creation
		originalWindowBrightness = window.getAttributes().screenBrightness;
	}

	public void applyCombinedBrightness() {
		Log.d(TAG, "Applying combined brightness control.");
		applySystemBrightnessOnly();
		if (isSystemBrightnessControlled) {
			setWindowBrightness(WINDOW_BRIGHTNESS_ABSOLUTE_MIN);
		} else {
			applyInAppWindowBrightness();
		}
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
		setWindowBrightness(WINDOW_BRIGHTNESS_ABSOLUTE_MIN);
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

	private void setWindowBrightness(float brightnessValue) {
		WindowManager.LayoutParams layoutParams = window.getAttributes();
		layoutParams.screenBrightness = brightnessValue;
		window.setAttributes(layoutParams);
	}

	private void restoreInAppWindowBrightness() {
		setWindowBrightness(originalWindowBrightness);
	}

	public boolean canWriteSystemSettings() {
		// Check for WRITE_SETTINGS permission (API 23+)
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			return Settings.System.canWrite(context);
		}
		return true; // Permission not required on older APIs
	}
}