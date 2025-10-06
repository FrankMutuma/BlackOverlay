package com.smarttechnologies.app.blackoverlay;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

/**
* A utility class to handle all vibration logic in a safe, centralized, and
* API-compatible manner.
* * NOTE: This class assumes you have added the required permission
* <uses-permission android:name="android.permission.VIBRATE" />
* to your AndroidManifest.xml file.
*/
public class VibrationHelper {

	private static final String TAG = "VibrationHelper";

	// --- CONVENIENCE METHOD (The one you call most often) ---

	/**
	* Executes a short (100ms) default vibration.
	* This method includes a safe try-catch block for the SecurityException.
	* * @param context The Context (e.g., Activity or Service) required
	* to get system services.
	*/
	public static void vibrateDefault(Context context) {
		// Wrap the call in a try-catch to prevent a crash if the
		// VIBRATE permission is missing in the Manifest.
		try {
			vibrate(context, 100);
		} catch (SecurityException e) {
			Log.e(TAG, "FATAL: VIBRATE permission missing in AndroidManifest.xml!", e);
			// App continues to run, but vibration is silently skipped.
		}
	}

	// --- CORE IMPLEMENTATION (Contains all the API logic) ---

	/**
	* Executes a vibration for a custom duration.
	* * @param context The Context required to get system services.
	* @param durationMillis The duration of the vibration in milliseconds.
	* @throws SecurityException if the VIBRATE permission is not in the Manifest.
	*/
	public static void vibrate(Context context, long durationMillis) throws SecurityException {

		// 1. Get the Vibrator instance (Handles API 31+ vs. older)
		Vibrator vibrator = getVibrator(context);

		// 2. Execute the vibration (Handles missing hardware and API 26+ vs. older)
		if (vibrator != null) {

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				// Modern, preferred method for API 26 (Oreo) and above
				VibrationEffect effect = VibrationEffect.createOneShot(durationMillis,
						VibrationEffect.DEFAULT_AMPLITUDE);
				vibrator.vibrate(effect);
			} else {
				// Deprecated method for API 25 and below (Still safe in your minSdk 30 scenario,
				// but good practice if you ever lower minSdk)
				@SuppressWarnings("deprecation")
				long deprecatedDuration = durationMillis;
				vibrator.vibrate(deprecatedDuration);
			}

		} else {
			Log.w(TAG, "Vibrator service not available on this device.");
		}
	}

	// --- PRIVATE HELPER METHOD ---

	private static Vibrator getVibrator(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			// New method for Android 12 (API 31) and above (using VibratorManager)
			VibratorManager vibratorManager = (VibratorManager) context
					.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
			return vibratorManager.getDefaultVibrator();
		} else {
			// Deprecated method for API 30 and below
			@SuppressWarnings("deprecation")
			Vibrator deprecatedVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
			return deprecatedVibrator;
		}
	}
}