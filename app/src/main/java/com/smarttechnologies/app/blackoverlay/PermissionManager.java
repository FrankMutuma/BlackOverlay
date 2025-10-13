package com.smarttechnologies.app.blackoverlay;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PermissionManager {

	private static final String TAG = "PermissionManager";
	private final AppCompatActivity activity;
	private final AppPreferencesManager prefsManager;

	// Launchers for the three distinct permission types
	private final ActivityResultLauncher<Intent> overlayPermissionLauncher;
	private final ActivityResultLauncher<Intent> writeSettingsLauncher;
	private final ActivityResultLauncher<String> notificationPermissionLauncher;

	// Listener fields for primary startup flow and single, on-demand requests
	private PermissionCallback callback;
	private SinglePermissionResultListener singleUseWriteSettingsListener;

	// --- Interfaces for Callbacks ---

	public interface PermissionCallback {
		void onAllPermissionsGranted(); // All 3 permissions granted

		void onEssentialPermissionGranted(); // Essential (Overlay + Notification) granted, Write Settings denied

		void onPermissionsDenied(); // Critical permission (Overlay) denied
	}

	/**
	* Simple listener used for optional, on-demand requests (like Write Settings).
	*/
	public interface SinglePermissionResultListener {
		void onResult(boolean granted);
	}

	// --- Constructor and Initialization ---

	public PermissionManager(final AppCompatActivity activity, PermissionCallback callback) {
		this.activity = activity;
		this.callback = callback;
		this.prefsManager = AppPreferencesManager.getInstance(activity);

		// 1. Launcher for NOTIFICATION (Runtime)
		notificationPermissionLauncher = activity
				.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
					if (isGranted) {
						Log.d(TAG, "Notification permission granted. Proceeding to Overlay check.");
						checkOverlayPermission();
					} else {
						Log.d(TAG, "Notification permission denied. Showing critical warning dialog.");
						showNotificationCriticalWarning();
					}
				});

		// 2. Launcher for OVERLAY (Special)
		overlayPermissionLauncher = activity
				.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						if (Settings.canDrawOverlays(activity)) {
							Log.d(TAG, "Overlay permission granted.");
							// Overlay is granted, now check for Write Settings
							checkWriteSettingsPermission();
						} else {
							Log.d(TAG, "Overlay permission denied. Cannot function without overlay.");
							handlePermissionDenial("Overlay");
						}
					}
				});

		// 3. Launcher for WRITE_SETTINGS (Special)
		writeSettingsLauncher = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
				result -> {
					boolean granted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
							&& Settings.System.canWrite(activity);

					if (singleUseWriteSettingsListener != null) {
						// Scenario 1: Handling an on-demand request
						singleUseWriteSettingsListener.onResult(granted);
						singleUseWriteSettingsListener = null; // Clear listener
					} else {
						// Scenario 2: Handling the initial startup chain request
						if (granted) {
							Log.d(TAG, "Startup Write Settings granted.");
							prefsManager.resetTotalDenials();
							if (callback != null) {
								callback.onAllPermissionsGranted();
							}
						} else {
							Log.d(TAG, "Startup Write Settings denied.");
							if (callback != null) {
								callback.onEssentialPermissionGranted();
							}
						}
					}
				});
	}

	/**
	* Main entry point: Checks and requests all necessary permissions sequentially.
	*/
	public void checkAndRequestPermissions() {
		// 1. Notification (API 33+)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (!hasNotificationPermission()) {
				requestNotificationPermission();
				return;
			}
		}

		// 2. Overlay (Always essential)
		checkOverlayPermission();
	}

	// --- PUBLIC On-Demand Request Method ---

	/**
	* Public method to specifically check and request the Write Settings permission
	* when a user tries to activate an enhanced feature later in the app lifecycle.
	* * @param listener A simple listener to notify the caller of the result.
	*/
	public void requestWriteSettingsOnDemand(SinglePermissionResultListener listener) {
		// 1. Check current status
		if (hasWriteSettingsPermission()) {
			listener.onResult(true);
			return;
		}

		// 2. Temporarily store the single-use listener
		this.singleUseWriteSettingsListener = listener;

		// 3. Show rationale and request
		showWriteSettingsExplanationOnDemand();
	}

	// --- Notification Permission Flow ---

	public boolean hasNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return ContextCompat.checkSelfPermission(activity,
					Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
		}
		return true;
	}

	private void requestNotificationPermission() {
		Log.d(TAG, "Requesting Notification permission (API 33+)...");
		notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
	}

	private void showNotificationCriticalWarning() {
		new AlertDialog.Builder(activity).setTitle("Warning: Control Mechanism Missing").setMessage(
				"You have denied the Notification permission. Without this, you will **not** have the easy 'stop/hide overlay' controls in the notification drawer.\n\n"
						+ "You must rely only on the optional dismissal method. We strongly recommend granting this permission for safety.")
				.setPositiveButton("Continue Anyway", (dialog, which) -> {
					checkOverlayPermission();
				}).setNegativeButton("Go to Settings", (dialog, which) -> {
					Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
					intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
					activity.startActivity(intent);
					checkOverlayPermission();
				}).setCancelable(false).show();
	}

	// --- Overlay Permission Flow (Essential) ---

	private void checkOverlayPermission() {
		if (!hasOverlayPermission()) {
			showOverlayExplanation();
		} else {
			checkWriteSettingsPermission();
		}
	}

	public boolean hasOverlayPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return Settings.canDrawOverlays(activity);
		}
		return true;
	}

	/**
	* Rationale: Explains *why* Overlay is needed and *how* to enable it.
	*/
	private void showOverlayExplanation() {
		Log.d(TAG, "Showing overlay permission rationale...");
		// Note: Using a generic name for robust compilation, ideally use the app's real label.
		String appName = "Black Overlay App";

		new AlertDialog.Builder(activity).setTitle("Crucial Step: Enable Screen Overlay").setMessage(
				"This app's entire purpose is to darken your screen. To work, it requires the 'Display over other apps' (Overlay) permission.\n\n"
						+ "**How to enable it (READ CAREFULLY):**\n" + "1. Tap 'Grant' below to go to Settings.\n"
						+ "2. Find this app's name ('" + appName + "') in the list.\n"
						+ "3. Toggle the switch to ON.\n\n" + "Without this, the app cannot function.")
				.setPositiveButton("Grant", (dialog, which) -> {
					requestOverlayPermission();
				}).setCancelable(false).show();
	}

	private void requestOverlayPermission() {
		Log.d(TAG, "Launching overlay settings intent...");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
					Uri.parse("package:" + activity.getPackageName()));
			overlayPermissionLauncher.launch(intent);
		}
	}

	// --- Write Settings Permission Flow (Optional/Enhanced) ---

	public boolean hasWriteSettingsPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return Settings.System.canWrite(activity);
		}
		return true;
	}

	// For Startup Chain (checks denial limit)
	private void checkWriteSettingsPermission() {
		if (!hasWriteSettingsPermission()) {
			showWriteSettingsExplanationStartup();
		} else {
			// All permissions are granted from the chain
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
	* Shows rationale for the STARTUP CHAIN (respects denial count).
	*/
	private void showWriteSettingsExplanationStartup() {
		if (prefsManager.getTotalDenials() >= AppPreferencesManager.MAX_TOTAL_DENIALS) {
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

	/**
	* Shows rationale for the ON-DEMAND request (does NOT respect denial count).
	*/
	private void showWriteSettingsExplanationOnDemand() {
		new AlertDialog.Builder(activity).setTitle("Enhanced Dimming Required").setMessage(
				"To enable the darkest screen settings, the 'Modify system settings' permission is needed. This allows the app to safely lower system brightness.")
				.setPositiveButton("Grant", (dialog, which) -> {
					requestWriteSettingsPermission();
				}).setNegativeButton("Cancel", (dialog, which) -> {
					// Cancel means denied for this specific feature request
					if (singleUseWriteSettingsListener != null) {
						singleUseWriteSettingsListener.onResult(false);
						singleUseWriteSettingsListener = null;
					}
				}).setCancelable(true).show();
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