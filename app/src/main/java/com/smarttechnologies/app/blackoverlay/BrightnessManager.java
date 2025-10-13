package com.smarttechnologies.app.blackoverlay;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

public class BrightnessManager {
	private static final String TAG = "BrightnessManager";
	//private static final int SYSTEM_BRIGHTNESS_MIN = 1;// the lowest system brightness is 0/1 depending
	private static final float WINDOW_BRIGHTNESS_ABSOLUTE_MIN = 0.0f;//0.0f><1.0 is lowest inapp brightness value

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
			setAbsoluteMinimumBrightness(cResolver);
			isSystemBrightnessControlled = true;

		} catch (Exception e) {
			Log.e(TAG, "Failed to control system brightness: " + e.getMessage());
			isSystemBrightnessControlled = false;
		}
	}

	/**
		* Attempts to set the screen brightness to 0. If reading the value back
		* shows the OS rejected 0, it sets the brightness to 1.
		* Executes the brightness setting logic on a new background thread.
	*/
	public void setAbsoluteMinimumBrightness(ContentResolver contentResolver) {
		String BRIGHTNESS_KEY = Settings.System.SCREEN_BRIGHTNESS;

		int TARGET_MIN = 0;
		int NEXT_MIN = 1;
		new Thread(new Runnable() {
			@Override
			public void run() {
				// NOTE: This ContentResolver must be safe to use from a background thread.
				// A long-lived Application Context's resolver is generally safer.

				// 1. Attempt to set the brightness to Target min
				Settings.System.putInt(contentResolver, BRIGHTNESS_KEY, TARGET_MIN);

				// Introduce the small delay to let the system apply the change.
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// IMPORTANT: Always re-interrupt the thread
					Thread.currentThread().interrupt();
					Log.e(TAG, "Thread interrupted during sleep.");
					return; // Exit the runnable
				}

				// 2. Read the current system brightness value back
				int actualBrightness;
				try {
					actualBrightness = Settings.System.getInt(contentResolver, BRIGHTNESS_KEY);
				} catch (Settings.SettingNotFoundException e) {
					Log.e(TAG, "Brightness setting not found.");
					return;
				}

				// 3. Compare the read-back value with the target
				if (actualBrightness != TARGET_MIN) {
					Log.w(TAG, "OS rejected 0, setting to 1 as the effective minimum.");
					Settings.System.putInt(contentResolver, BRIGHTNESS_KEY, NEXT_MIN);
					isSystemBrightnessControlled = true;
					Log.i(TAG, "Request sent to set brightness to 1.");
				} else {
					isSystemBrightnessControlled = true;
					Log.i(TAG, "Successfully set brightness to 0.");

				}
			}
		}).start(); // Starts the new thread and executes run()
	}

	public void applyInAppWindowBrightness() {
		if (overlayParams != null) {
			if ((WINDOW_BRIGHTNESS_ABSOLUTE_MIN >= 0.0f) && (WINDOW_BRIGHTNESS_ABSOLUTE_MIN <= 1.0))
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
				Log.i(TAG, "System brightness restored");
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