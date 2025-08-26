package com.smarttechnologies.app.blackoverlay;

import android.app.Activity;
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

	public interface PermissionCallback {
		void onAllPermissionsGranted();

		void onEssentialPermissionGranted(); // Overlay is granted but not Write Settings

		void onPermissionsDenied();
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
							// Now check for Write Settings permission
							checkWriteSettingsPermission();
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
						prefsManager.resetTotalDenials();
						if (callback != null) {
							callback.onAllPermissionsGranted();
						}
					} else {
						Log.d(TAG, "Write Settings permission denied.");
						// Even if Write Settings is denied, we can still function with overlay
						if (callback != null) {
							callback.onEssentialPermissionGranted();
						}
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
		} else {
			// Overlay is already granted, check Write Settings
			checkWriteSettingsPermission();
		}
	}

	public boolean hasOverlayPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return Settings.canDrawOverlays(activity);
		}
		return true;
	}

	public boolean hasWriteSettingsPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return Settings.System.canWrite(activity);
		}
		return true;
	}

	private void requestOverlayPermission() {
		Log.d(TAG, "Requesting overlay permission...");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
					Uri.parse("package:" + activity.getPackageName()));
			overlayPermissionLauncher.launch(intent);
		}
	}

	private void checkWriteSettingsPermission() {
		if (!hasWriteSettingsPermission()) {
			showWriteSettingsExplanation();
		} else {
			// Both permissions are granted
			if (callback != null) {
				callback.onAllPermissionsGranted();
			}
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
	* Shows a rational dialog explaining why Write Settings permission is needed.
	*/
	private void showWriteSettingsExplanation() {
		// Check if we've hit the limit for asking
		if (prefsManager.getTotalDenials() >= AppPreferencesManager.MAX_TOTAL_DENIALS) {
			// We've asked too many times, just proceed with overlay only
			if (callback != null) {
				callback.onEssentialPermissionGranted();
			}
			return;
		}

		new AlertDialog.Builder(activity).setTitle("Enhanced Dimming").setMessage(
				"For the darkest possible screen and battery savings, this app needs the 'Modify system settings' permission. This allows it to lower your system brightness beyond the normal limit.")
				.setPositiveButton("Grant", (dialog, which) -> {
					requestWriteSettingsPermission();
				}).setNegativeButton("Skip", (dialog, which) -> {
					prefsManager.incrementTotalDenials();
					if (callback != null) {
						callback.onEssentialPermissionGranted();
					}
				}).setCancelable(false).show();
	}

	private void handlePermissionDenial(String permissionType) {
		prefsManager.incrementTotalDenials();
		Toast.makeText(activity, permissionType + " permission is required for basic functionality.", Toast.LENGTH_LONG)
				.show();
		if (callback != null) {
			callback.onPermissionsDenied();
		}
	}

	public void clearCallback() {
		this.callback = null;
	}
}