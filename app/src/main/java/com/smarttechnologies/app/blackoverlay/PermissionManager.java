package com.smarttechnologies.app.blackoverlay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PermissionManager {

	private static final String TAG = "PermissionManager";
	private final AppCompatActivity activity;
	private final AppPreferencesManager prefsManager;
	private final ActivityResultLauncher<Intent> overlayPermissionLauncher;
	private final ActivityResultLauncher<Intent> writeSettingsLauncher;
	private PermissionCallback callback;

	// Interface to communicate results back to the Activity
	public interface PermissionCallback {
		void onOverlayPermissionGranted();

		void onWriteSettingsPermissionGranted();

		void onPermissionDenied();
	}

	public PermissionManager(AppCompatActivity activity, PermissionCallback callback) {
		this.activity = activity;
		this.callback = callback;
		this.prefsManager = AppPreferencesManager.getInstance(activity);

		// Launcher for the OVERLAY permission
		overlayPermissionLauncher = activity
				.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						if (Settings.canDrawOverlays(activity)) {
							Log.d(TAG, "Overlay permission granted.");
							if (callback != null) {
								callback.onOverlayPermissionGranted();
							}
						} else {
							Log.d(TAG, "Overlay permission denied.");
							handlePermissionDenial("Overlay");
						}
					}
				});

		// Launcher for the WRITE_SETTINGS permission
		writeSettingsLauncher = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
				result -> {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(activity)) {
						Log.d(TAG, "Write Settings permission granted.");
						prefsManager.resetTotalDenials(); // Reset on success
						if (callback != null) {
							callback.onWriteSettingsPermissionGranted();
						}
					} else {
						Log.d(TAG, "Write Settings permission denied.");
						handlePermissionDenial("WriteSettings");
					}
				});
	}

	/**
	* Checks and requests all necessary permissions to start the app.
	* This is the main entry point, called from onCreate.
	*/
	public void checkAndRequestPermissions() {
		// 1. First, check Overlay Permission (absolutely essential)
		if (!hasOverlayPermission()) {
			requestOverlayPermission();
			return; // Stop here. We'll continue after overlay is granted.
		}

		// 2. If overlay is granted, check Write Settings (for enhanced functionality)
		if (!hasWriteSettingsPermission()) {
			showWriteSettingsExplanation(); // Explain why we need it first
		} else {
			// If already granted, notify the callback
			if (callback != null) {
				callback.onWriteSettingsPermissionGranted();
			}
		}
	}

	public boolean hasOverlayPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return Settings.canDrawOverlays(activity);
		}
		return true; // Permission not required on older Android versions
	}

	public boolean hasWriteSettingsPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return Settings.System.canWrite(activity);
		}
		return true; // Permission not required on older Android versions
	}

	private void requestOverlayPermission() {
		Log.d(TAG, "Requesting overlay permission...");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
					Uri.parse("package:" + activity.getPackageName()));
			overlayPermissionLauncher.launch(intent);
		}
	}

	private void requestWriteSettingsPermission() {
		Log.d(TAG, "Requesting write settings permission...");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
					Uri.parse("package:" + activity.getPackageName()));
			writeSettingsLauncher.launch(intent);
		}
	}

	/**
	* Shows a rational dialog explaining why Write Settings permission is needed for full brightness control.
	*/
	private void showWriteSettingsExplanation() {
		// Check if we've hit the limit for asking
		if (prefsManager.getTotalDenials() >= AppPreferencesManager.MAX_TOTAL_DENIALS) {
			Toast.makeText(activity, "Using limited brightness control.", Toast.LENGTH_LONG).show();
			if (callback != null) {
				callback.onPermissionDenied(); // Notify that we're proceeding without full perms
			}
			return;
		}

		new AlertDialog.Builder(activity).setTitle("Enhanced Dimming").setMessage(
				"For the darkest possible screen and battery savings, this app needs the 'Modify system settings' permission. This allows it to lower your system brightness beyond the normal limit.")
				.setPositiveButton("Grant", (dialog, which) -> {
					requestWriteSettingsPermission();
					prefsManager.incrementTotalDenials(); // Count this prompt as a denial if user later denies in settings
				}).setNegativeButton("Skip", (dialog, which) -> {
					Toast.makeText(activity, "Using standard dimming.", Toast.LENGTH_SHORT).show();
					if (callback != null) {
						callback.onPermissionDenied();
					}
				}).setCancelable(false).show();
	}

	private void handlePermissionDenial(String permissionType) {
		prefsManager.incrementTotalDenials();
		Toast.makeText(activity, permissionType + " permission is required for full functionality.", Toast.LENGTH_LONG)
				.show();
		if (callback != null) {
			callback.onPermissionDenied();
		}
	}

	// Call this in onDestroy to avoid potential memory leaks
	public void clearCallback() {
		this.callback = null;
	}
}