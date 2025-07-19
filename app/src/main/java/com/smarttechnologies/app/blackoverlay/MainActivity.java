package com.smarttechnologies.app.blackoverlay;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "BrightnessControl";
	private static final int SYSTEM_BRIGHTNESS_MIN = 1; // Android's actual minimum system brightness
	private static final float WINDOW_BRIGHTNESS_MIN = 0.0f; // use 0.01f as Smallest value for window brightness (0.0f might be truly invisible)

	private int originalSystemBrightnessValue = -1; // Value of system brightness before our app changed it
	private int originalSystemBrightnessMode = -1; // Mode (manual/auto) of system brightness before our app changed it
	private float originalWindowBrightness = -2.0f; // Value of window brightness before our app changed it (-2.0f as uninitialized marker)

	private boolean isSystemBrightnessControlled = false; // Tracks if system permission was successfully used

	// Variable to track what brightness value the app is *currently* applying to its UI
	private float brightnessTrackValue = 1.0f; // Default to full brightness for the tracker initially

	private TextView txttext;
	private ActivityResultLauncher<Intent> writeSettingsLauncher;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate: Activity created.");

		// Initialize ActivityResultLauncher for WRITE_SETTINGS permission
		writeSettingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
				result -> {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) {
						Log.d(TAG, "onActivityResult: Permission granted by user. Applying combined brightness.");
						applyCombinedBrightness(); // Apply the combined logic if permission granted
						Toast.makeText(this, "Permission granted. Using full brightness control.", Toast.LENGTH_SHORT)
								.show();
					} else {
						Log.d(TAG, "onActivityResult: Permission denied by user. Falling back to in-app only.");
						Toast.makeText(this, "Permission denied. Using in-app brightness control only.",
								Toast.LENGTH_LONG).show();
						applyInAppWindowBrightness(); // Fallback
					}
				});

		// --- Modern Full-Screen & Edge-to-Edge Display Setup (API 30+) ---
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			final WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(),
					getWindow().getDecorView());
			if (controller != null) {
				controller.hide(WindowInsetsCompat.Type.systemBars());
				controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
				Log.d(TAG, "onCreate: Modern system bars hidden.");
			}
		} else {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
			Log.d(TAG, "onCreate: Legacy system bars hidden.");
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			getWindow()
					.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
			Log.d(TAG, "onCreate: Display cutout mode set for short edges.");
		}
		// --- End Modern Full-Screen Setup ---

		setContentView(R.layout.activity_main); // Layout is inflated here

		txttext = findViewById(R.id.txtoutput);
		if (txttext != null) {
			txttext.setText("Brightness Info: Loading...");
		} else {
			Log.e(TAG, "onCreate: txtoutput TextView not found! Check activity_main.xml for @id/txtoutput");
		}

		// Initialize originalWindowBrightness here to capture initial state
		// This is important because the window's screenBrightness can be BRIGHTNESS_OVERRIDE_NONE
		// if the system is managing it, or a specific float if another app set it.
		originalWindowBrightness = getWindow().getAttributes().screenBrightness;
		Log.d(TAG, "onCreate: Initial originalWindowBrightness captured as: " + originalWindowBrightness);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Log.d(TAG, "onCreate: FLAG_KEEP_SCREEN_ON added.");

		// Initial check and apply brightness
		checkAndApplyBrightness();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Toast.makeText(this, "onResume CALLED", Toast.LENGTH_SHORT).show();
		Log.d(TAG, "onResume: Activity resumed.");
		checkAndApplyBrightness(); // Re-apply brightness settings
	}

	@Override
	protected void onPause() {
		super.onPause();
		Toast.makeText(this, "onPause CALLED", Toast.LENGTH_SHORT).show();
		Log.d(TAG, "onPause: Activity paused. Restoring brightness.");
		restoreBrightness(); // Restore brightness
	}

	/**
	* Checks for WRITE_SETTINGS permission and applies appropriate brightness control.
	*/
	private void checkAndApplyBrightness() {
		Log.d(TAG, "checkAndApplyBrightness: Checking WRITE_SETTINGS permission.");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!Settings.System.canWrite(this)) {
				Log.d(TAG, "checkAndApplyBrightness: WRITE_SETTINGS permission not granted. Requesting...");
				Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
						Uri.parse("package:" + getPackageName()));
				writeSettingsLauncher.launch(intent); // Use the modern launcher
				Toast.makeText(this, "Please grant 'Modify system settings' permission for full control.",
						Toast.LENGTH_LONG).show();
				applyInAppWindowBrightness(); // Fallback immediately while waiting for permission result
			} else {
				Log.d(TAG, "checkAndApplyBrightness: WRITE_SETTINGS permission granted. Applying combined brightness.");
				applyCombinedBrightness(); // Permission is granted, apply the full logic
			}
		} else {
			Log.d(TAG, "checkAndApplyBrightness: API < 23, permission not required. Applying combined brightness.");
			applyCombinedBrightness(); // For older APIs, assume permission or not needed.
		}
	}

	/**
	* Applies system brightness (if possible) and then forces window brightness to 0.01f.
	* This is the preferred method when WRITE_SETTINGS permission is available.
	*/
	private void applyCombinedBrightness() {
		Log.d(TAG, "applyCombinedBrightness: Attempting to set system brightness to " + SYSTEM_BRIGHTNESS_MIN
				+ " and window brightness to " + WINDOW_BRIGHTNESS_MIN + ".");

		// Step 1: Attempt to set system brightness
		applySystemBrightnessOnly(); // This method sets isSystemBrightnessControlled

		// Step 2: Apply window brightness override if system control was successfully initiated
		if (isSystemBrightnessControlled) {
			WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
			// originalWindowBrightness was already captured in onCreate().
			// Now, we apply our app's specific override.

			layoutParams.screenBrightness = WINDOW_BRIGHTNESS_MIN; // Force window to darkest (e.g., 0.01f)
			getWindow().setAttributes(layoutParams);
			Log.d(TAG, "applyCombinedBrightness: Window brightness explicitly set to " + WINDOW_BRIGHTNESS_MIN + ".");
			Toast.makeText(this, "System brightness dimmed, window forced to " + WINDOW_BRIGHTNESS_MIN + ".",
					Toast.LENGTH_SHORT).show();
			brightnessTrackValue = WINDOW_BRIGHTNESS_MIN; // Update tracker
		} else {
			// Fallback if applySystemBrightnessOnly failed for some reason (e.g., unexpected exception)
			Log.d(TAG,
					"applyCombinedBrightness: System brightness control failed unexpectedly, falling back to in-app only.");
			applyInAppWindowBrightness();
		}
		displayCurrentBrightness(); // Display info after all settings are applied
	}

	/**
	* Only applies changes to the system's SCREEN_BRIGHTNESS.
	* Sets isSystemBrightnessControlled flag based on success.
	*/
	private void applySystemBrightnessOnly() {
		Log.d(TAG, "applySystemBrightnessOnly: Attempting to set system brightness to " + SYSTEM_BRIGHTNESS_MIN + ".");
		ContentResolver cResolver = getContentResolver();
		try {
			originalSystemBrightnessValue = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
			originalSystemBrightnessMode = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);
			Log.d(TAG, "applySystemBrightnessOnly: Original system brightness saved: " + originalSystemBrightnessValue
					+ ", mode: " + originalSystemBrightnessMode);

			String originalModeName = (originalSystemBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
					? "AUTO"
					: "MANUAL";
			Log.d(TAG, "applySystemBrightnessOnly: Original system brightness: " + originalSystemBrightnessValue
					+ ", Mode: " + originalModeName + " (" + originalSystemBrightnessMode + ")");

			if (originalSystemBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
				Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
						Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
				Log.d(TAG, "applySystemBrightnessOnly: Switched system brightness mode to MANUAL for app control.");
			} else {
				Log.d(TAG, "applySystemBrightnessOnly: Original mode was already MANUAL, no mode change needed.");
			}

			Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, SYSTEM_BRIGHTNESS_MIN);
			Log.d(TAG, "applySystemBrightnessOnly: System brightness set to " + SYSTEM_BRIGHTNESS_MIN + ".");

			isSystemBrightnessControlled = true; // Mark success
		} catch (Settings.SettingNotFoundException e) {
			Log.e(TAG, "applySystemBrightnessOnly: Settings.SettingNotFoundException: " + e.getMessage(), e);
			isSystemBrightnessControlled = false; // Mark failure
		} catch (SecurityException e) {
			Log.e(TAG, "applySystemBrightnessOnly: SecurityException (WRITE_SETTINGS permission issue?): "
					+ e.getMessage(), e);
			isSystemBrightnessControlled = false; // Mark failure
		}
	}

	/**
	* Applies brightness only to the app's window. Used as a fallback.
	*/
	private void applyInAppWindowBrightness() {
		Log.d(TAG, "applyInAppWindowBrightness: Attempting to set in-app window brightness to " + WINDOW_BRIGHTNESS_MIN
				+ ".");
		WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
		// originalWindowBrightness was already captured in onCreate().

		layoutParams.screenBrightness = WINDOW_BRIGHTNESS_MIN; // Set window brightness to darkest
		getWindow().setAttributes(layoutParams); // Apply the new brightness setting
		Toast.makeText(this, "Using in-app window brightness control.", Toast.LENGTH_SHORT).show();
		brightnessTrackValue = WINDOW_BRIGHTNESS_MIN; // Update tracker
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy: Activity destroyed.");
		restoreBrightness(); // Restore original brightness

		// Clear FLAG_KEEP_SCREEN_ON when the activity is destroyed
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Log.d(TAG, "onDestroy: FLAG_KEEP_SCREEN_ON cleared.");
	}

	/**
	* Unified brightness restoration method based on how brightness was controlled.
	*/
	private void restoreBrightness() {
		Log.d(TAG, "restoreBrightness: Restoring brightness based on control type ("
				+ (isSystemBrightnessControlled ? "SYSTEM" : "IN-APP") + ").");
		if (isSystemBrightnessControlled) {
			restoreSystemAndWindowOverrideBrightness();
		} else {
			restoreInAppWindowBrightness();
		}
		// Reset brightnessTrackValue after restoration
		brightnessTrackValue = originalWindowBrightness; // Or to 1.0f if you want it always full bright on exit
		if (brightnessTrackValue == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
			brightnessTrackValue = 1.0f; // Assume full bright if it was deferring to system
		}
		Log.d(TAG, "restoreBrightness: brightnessTrackValue reset to: " + brightnessTrackValue);
	}

	/**
	* Restores system brightness and ensures window brightness is also restored to its original state.
	*/
	private void restoreSystemAndWindowOverrideBrightness() {
		ContentResolver cResolver = getContentResolver();
		try {
			// Restore System Brightness
			if (originalSystemBrightnessValue != -1 && originalSystemBrightnessMode != -1) {
				Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, originalSystemBrightnessValue);
				Log.d(TAG, "restoreSystemAndWindowOverrideBrightness: System brightness restored to: "
						+ originalSystemBrightnessValue);

				String restoredModeName = (originalSystemBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
						? "AUTO"
						: "MANUAL";
				Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, originalSystemBrightnessMode);
				Log.d(TAG, "restoreSystemAndWindowOverrideBrightness: System brightness mode restored to: "
						+ restoredModeName + " (" + originalSystemBrightnessMode + ")");

				Toast.makeText(this, "System brightness restored.", Toast.LENGTH_SHORT).show();
			} else {
				Log.d(TAG,
						"restoreSystemAndWindowOverrideBrightness: No original system brightness values to restore. Skipping system restore.");
			}

			// Restore Window Brightness to its original captured state
			if (originalWindowBrightness != -2.0f) { // Check if original was successfully captured
				WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
				layoutParams.screenBrightness = originalWindowBrightness;
				getWindow().setAttributes(layoutParams);
				Log.d(TAG,
						"restoreSystemAndWindowOverrideBrightness: Window brightness restored to its original value: "
								+ originalWindowBrightness);
			} else {
				Log.d(TAG,
						"restoreSystemAndWindowOverrideBrightness: originalWindowBrightness was not captured, setting to BRIGHTNESS_OVERRIDE_NONE as fallback.");
				WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
				layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; // Ensure it defers to system
				getWindow().setAttributes(layoutParams);
			}

		} catch (Exception e) { // Catching generic Exception to log any issues during restore
			Log.e(TAG, "restoreSystemAndWindowOverrideBrightness: Error restoring brightness.", e);
			Toast.makeText(this, "Error restoring brightness.", Toast.LENGTH_SHORT).show();
		} finally {
			isSystemBrightnessControlled = false; // Reset flag
		}
	}

	/**
	* Restores only the in-app window brightness.
	*/
	private void restoreInAppWindowBrightness() {
		if (originalWindowBrightness != -2.0f) { // Check if original was successfully captured
			Log.d(TAG,
					"restoreInAppWindowBrightness: Restoring in-app window brightness to: " + originalWindowBrightness);
			WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
			layoutParams.screenBrightness = originalWindowBrightness;
			getWindow().setAttributes(layoutParams);
			Toast.makeText(this, "In-app brightness restored.", Toast.LENGTH_SHORT).show();
		} else {
			Log.d(TAG,
					"restoreInAppWindowBrightness: No original in-app window brightness to restore. Setting to BRIGHTNESS_OVERRIDE_NONE as fallback.");
			// If original wasn't captured, ensure it defers to the system as a safe fallback
			WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
			layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
			getWindow().setAttributes(layoutParams);
		}
		isSystemBrightnessControlled = false; // Reset flag
	}

	/**
	* Displays current brightness information in a Toast and the TextView.
	*/
	private void displayCurrentBrightness() {
		ContentResolver cResolver = getContentResolver();
		try {
			int currentSystemBrightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
			int currentBrightnessMode = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);

			String modeText = (currentBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) ? "Automatic"
					: "Manual";

			WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
			float currentWindowBrightness = layoutParams.screenBrightness; // Get actual window brightness attribute

			String message = "System Brightness: " + currentSystemBrightness + " (out of 255)\n" + "System Mode: "
					+ modeText + "\n" + "Window Brightness: " + String.format("%.2f", currentWindowBrightness)
					+ " (out of 1.0)\n" + "App Applied Value: " + String.format("%.2f", brightnessTrackValue); // Use tracked value

			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			Log.d(TAG, message);

			if (txttext != null) {
				txttext.setText(message);
			} else {
				Log.e(TAG, "displayCurrentBrightness: txttext is null. findViewById might have failed.");
			}
		} catch (Settings.SettingNotFoundException e) {
			Log.e(TAG, "displayCurrentBrightness: Settings.SettingNotFoundException: " + e.getMessage(), e);
			Toast.makeText(this, "Could not read system brightness settings.", Toast.LENGTH_SHORT).show();
		} catch (SecurityException e) {
			Log.e(TAG,
					"displayCurrentBrightness: SecurityException (READ_SETTINGS permission issue?): " + e.getMessage(),
					e);
			Toast.makeText(this, "Permission to read settings denied.", Toast.LENGTH_SHORT).show();
		}
	}
}