package com.smarttechnologies.app.blackoverlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

//import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
//import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to check for forced updates using a single Firebase Remote Config JSON parameter.
 *
 * NOTE: This class assumes the single parameter 'app_update_info' is configured in Firebase
 * with a JSON string value, as shown in your last screenshot.
 */
 public class UpdateCheckerUtil {

	/*private static final String TAG = "ForceUpdateChecker";
	private static final String RC_KEY_UPDATE_INFO = "app_update_info";
	private final Activity activity;
	private final FirebaseRemoteConfig remoteConfig;

	/**
	 * The single JSON structure expected from Remote Config.
	 * The Default Value ensures the app doesn't crash if Remote Config fails to fetch.
	 *//*
	private static final String DEFAULT_UPDATE_JSON = "{\"latest_version_code\": 1, \"update_type\": \"NONE\", \"apk_download_url\": \"\"}";

	// --- Constructor and Initialization ---

	public UpdateCheckerUtil(Activity activity) {
		/*this.activity = activity;
		this.remoteConfig = FirebaseRemoteConfig.getInstance();
		initializeRemoteConfig();*//*
	}

	/**
	 * Initializes Firebase Remote Config settings, including a default map.
	 *//*
	private void initializeRemoteConfig() {
		// 1. Set up a default map for parameters (crucial for quick startup)
		Map<String, Object> defaultMap = new HashMap<>();
		defaultMap.put(RC_KEY_UPDATE_INFO, DEFAULT_UPDATE_JSON);
		remoteConfig.setDefaultsAsync(defaultMap);

		// 2. Set config settings (e.g., fetch interval)
		// Set minimumFetchIntervalInSeconds to 0 during development/testing
		// In production, use a higher value like 3600 (1 hour)
		FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
				.setMinimumFetchIntervalInSeconds(0).build();
		remoteConfig.setConfigSettingsAsync(configSettings);
	}

	// --- Core Logic ---

	/**
	 * Fetches the latest config from the server and then checks for updates.
	 *//*
	public void fetchAndCheckForUpdates() {
		Log.d(TAG, "Attempting to fetch Remote Config...");

		remoteConfig.fetchAndActivate().addOnCompleteListener(activity, task -> {
			if (task.isSuccessful()) {
				Log.i(TAG, "Remote Config fetch and activate successful.");
				// The config is now activated, check the update status
				checkUpdateStatus();
			} else {
				Log.e(TAG, "Remote Config fetch failed. Using cached values.", task.getException());
				// Even on failure, check the cached/default status
				checkUpdateStatus();
			}
		});
	}

	/**
	 * Retrieves the current update information from the activated config
	 * and performs the force update check.
	 *//*
	private void checkUpdateStatus() {
		try {
			// Get the current version code of the running app
			int currentVersionCode = getAppVersionCode(activity);

			// 1. Get the single JSON string from Remote Config
			String configString = remoteConfig.getString(RC_KEY_UPDATE_INFO);

			// 2. Parse the JSON string
			JSONObject updateInfoJson = new JSONObject(configString);

			int latestVersionCode = updateInfoJson.optInt("latest_version_code", currentVersionCode);
			String updateType = updateInfoJson.optString("update_type", "NONE").toUpperCase();
			String downloadUrl = updateInfoJson.optString("apk_download_url", "");

			Log.d(TAG, "RC Latest Version: " + latestVersionCode + ", Update Type: " + updateType);

			// 3. Apply the Forced Update Logic
			if (updateType.equals("FORCE") && latestVersionCode > currentVersionCode) {
				// FORCE UPDATE IS REQUIRED
				showForceUpdateDialog(latestVersionCode, downloadUrl);
			} else if (updateType.equals("RECOMMEND") && latestVersionCode > currentVersionCode) {
				// RECOMMENDED UPDATE IS AVAILABLE
				Log.i(TAG, "Recommended update (v" + latestVersionCode + ") available.");
				// You would typically show a non-blocking dialog here
			} else {
				// APP IS UP TO DATE or update not required
				Log.i(TAG, "App is up to date or update not required.");
			}

		} catch (Exception e) {
			Log.e(TAG, "Error processing Remote Config update information.", e);
			// Fallback: Use the app as normal if parsing fails
		}
	}

	// --- Utility Methods ---

	/**
	 * Gets the version code of the installed application.
	 * @param context The application context.
	 * @return The version code, or 0 if not found.
	 *//*
	private int getAppVersionCode(Context context) {
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			// For modern Android (API 28+), use getLongVersionCode() and cast if needed
			// For simplicity and compatibility, we use the deprecated getVersionCode()
			return pInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Could not get package info.", e);
			return 0;
		}
	}

	/**
	 * Displays a blocking dialog for a forced update.
	 *//*
	private void showForceUpdateDialog(int latestVersionCode, String downloadUrl) {
		// This MUST be a blocking dialog that prevents the user from continuing
		new AlertDialog.Builder(activity).setTitle("Update Required")
				.setMessage(
						"A critical update (v" + latestVersionCode + ") is required to continue using the application.")
				.setPositiveButton("Update Now", (dialog, which) -> {
					// Redirect to the download URL (e.g., Google Play or direct APK link)
					try {
						activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)));
					} catch (Exception e) {
						Toast.makeText(activity, "Could not open download link.", Toast.LENGTH_LONG).show();
						Log.e(TAG, "Failed to open update link.", e);
					}
					// Crucially, close the current activity/app if they don't update
					activity.finishAffinity();
				}).setCancelable(false) // Prevents user from dismissing the dialog
				.show();
	}
*/
}