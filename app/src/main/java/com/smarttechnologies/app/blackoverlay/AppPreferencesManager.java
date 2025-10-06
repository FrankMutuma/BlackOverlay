package com.smarttechnologies.app.blackoverlay;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class AppPreferencesManager {

	private static AppPreferencesManager instance;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;

	//General keys

	//private static final String PREF_NAME = "black_overlay_prefs";
	private static final String KEY_TOTAL_DENIALS = "total_permission_denials"; // Global counter for all prompts shown & denied/dismissed
	private static final String KEY_INITIAL_LAUNCH_PROMPT_COUNT = "initial_launch_prompt_count"; // Counter for prompts shown specifically on onCreate
	private static final String KEY_PREVENT_TOUCH = "preventTouch";
	private static final String KEY_MEDIA_CONTROL_ENABLED = "mediaEnabled";

	public static final int MAX_TOTAL_DENIALS = 9; // Max total prompts allowed across all sessions and launches
	public static final int MAX_INITIAL_LAUNCH_PROMPTS = 3; // Max times to show the prompt on first app open (onCreate)
	//floating lock size
	private static final String KEY_FLOATING_LOCK_SIZE = "floating_lock_size";
	private static final int DEFAULT_FLOATING_LOCK_SIZE = 60; // Default size in dp
	private static final int MIN_FLOATING_LOCK_SIZE = 10; // Minimum size in dp
	private static final int MAX_FLOATING_LOCK_SIZE = 100; // Maximum size in dp
	//overlay management

	// --- Settings Preference Keys ---

	public static final String KEY_LOCK_TYPE = "lock_type";
	public static final String KEY_THEME = "theme";
	public static final String KEY_ALWAYS_ON_DISPLAY = "always_on_display";
	public static final String KEY_SKIP_UNLOCK_SCREEN = "skip_unlock_screen";
	public static final String KEY_TAPS_TO_WAKE = "taps_to_wake";
	public static final String KEY_HIDE_FLOATING_BUTTON = "hide_floating_button";
	public static final String KEY_FLOATING_BUTTON_ACTION = "floating_button_action";
	public static final String KEY_BIOMETRIC_AUTH = "biometric_auth";
	public static final String KEY_POCKET_DETECTION = "pocket_detection";
	public static final String KEY_REDUCE_BRIGHTNESS = "reduce_brightness";
	public static final String KEY_OLED_BURN_IN_PROTECTION = "oled_burn_in_protection";
	public static final String KEY_QUICK_TILES_INSTANT_BLACKOUT = "quick_tiles_instant_blackout";
	public static final String KEY_DISABLE_BATTERY_OPTIMIZATION = "disable_battery_optimization";
	public static final String KEY_APP_LANGUAGE = "app_language";




	private AppPreferencesManager(Context context) {
		//sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		sharedPreferences = androidx.preference.PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		editor = sharedPreferences.edit();

	}

	public static synchronized AppPreferencesManager getInstance(Context context) {
		if (instance == null) {
			instance = new AppPreferencesManager(context);
		}
		return instance;
	}

	//General getters

	public static int getMinFloatingLockSize() {
		return MIN_FLOATING_LOCK_SIZE;
	}

	public static int getMaxFloatingLockSize() {
		return MAX_FLOATING_LOCK_SIZE;
	}

	// --- Global Total Denials ---
	public int getTotalDenials() {
		return sharedPreferences.getInt(KEY_TOTAL_DENIALS, 0);
	}

	public void incrementTotalDenials() {
		int currentDenials = getTotalDenials();
		editor.putInt(KEY_TOTAL_DENIALS, currentDenials + 1).apply();
	}

	public void resetTotalDenials() {
		editor.putInt(KEY_TOTAL_DENIALS, 0).apply();
	}

	// --- Initial Launch Prompt Count ---
	public int getInitialLaunchPromptCount() {
		return sharedPreferences.getInt(KEY_INITIAL_LAUNCH_PROMPT_COUNT, 0);
	}

	public void incrementInitialLaunchPromptCount() {
		int currentCount = getInitialLaunchPromptCount();
		editor.putInt(KEY_INITIAL_LAUNCH_PROMPT_COUNT, currentCount + 1).apply();
	}

	public void resetInitialLaunchPromptCount() {
		editor.putInt(KEY_INITIAL_LAUNCH_PROMPT_COUNT, 0).apply();
	}

	//look and feel
	//floating lock size
	public void setFloatingLockSize(int size) {
		// Enforce minimum and maximum size
		size = Math.max(MIN_FLOATING_LOCK_SIZE, Math.min(MAX_FLOATING_LOCK_SIZE, size));
		editor.putInt(KEY_FLOATING_LOCK_SIZE, size).apply();
	}

	public int getFloatingLockSize() {
		int size = sharedPreferences.getInt(KEY_FLOATING_LOCK_SIZE, DEFAULT_FLOATING_LOCK_SIZE);
		// Ensure the retrieved size is within bounds
		return Math.max(MIN_FLOATING_LOCK_SIZE, Math.min(MAX_FLOATING_LOCK_SIZE, size));
	}

	public void setPreventTouch(Boolean preventTouch) {
		editor.putBoolean(KEY_PREVENT_TOUCH, preventTouch).apply();
	}

	public boolean getPreventTouch() {
		return sharedPreferences.getBoolean(KEY_PREVENT_TOUCH, true);
	}

	//media controls trial implenentation
	public void setMediaControlsEnabled(boolean mediaEnabled) {
		editor.putBoolean(KEY_MEDIA_CONTROL_ENABLED, mediaEnabled).apply();
	}

	public boolean getMediaControlsEnabled() {
		return sharedPreferences.getBoolean(KEY_MEDIA_CONTROL_ENABLED, false);
	}

	// --- Settings Preference getters ---

	// lock_type (ListPreference, Default: "blackoverlay")

	public String getLockType() {
		// Use the new consistent default value
		return sharedPreferences.getString(KEY_LOCK_TYPE, "black_overlay");
	}

	// theme (ListPreference, Default: "system_default")
	public String getTheme() {
		return sharedPreferences.getString(KEY_THEME, "system_default");
	}

	// always_on_display (SwitchPreferenceCompat, Default: false)
	public boolean isAlwaysOnDisplayEnabled() {
		return sharedPreferences.getBoolean(KEY_ALWAYS_ON_DISPLAY, false);
	}

	// skip_unlock_screen (SwitchPreferenceCompat, Default: false)
	public boolean isSkipUnlockScreenEnabled() {
		return sharedPreferences.getBoolean(KEY_SKIP_UNLOCK_SCREEN, false);
	}

	// taps_to_wake (ListPreference, Default: "2")
	// Note: This is an integer in the UI, but ListPreference saves it as a String.
	public String getTapsToWake() {
		return sharedPreferences.getString(KEY_TAPS_TO_WAKE, "2");
	}

	// hide_floating_button (SwitchPreferenceCompat, Default: false)
	public boolean isFloatingButtonHidden() {
		return sharedPreferences.getBoolean(KEY_HIDE_FLOATING_BUTTON, false);
	}

	// floating_button_action (ListPreference, Default: "none")
	public String getFloatingButtonAction() {
		return sharedPreferences.getString(KEY_FLOATING_BUTTON_ACTION, "none");
	}

	// biometric_auth (SwitchPreferenceCompat, Default: false)
	public boolean isBiometricAuthEnabled() {
		return sharedPreferences.getBoolean(KEY_BIOMETRIC_AUTH, false);
	}

	// pocket_detection (SwitchPreferenceCompat, Default: false)
	public boolean isPocketDetectionEnabled() {
		return sharedPreferences.getBoolean(KEY_POCKET_DETECTION, false);
	}

	// reduce_brightness (SwitchPreferenceCompat, Default: false)
	public boolean isReduceBrightnessEnabled() {
		return sharedPreferences.getBoolean(KEY_REDUCE_BRIGHTNESS, false);
	}

	// oled_burn_in_protection (SwitchPreferenceCompat, Default: false)
	public boolean isOLEDProtectionEnabled() {
		return sharedPreferences.getBoolean(KEY_OLED_BURN_IN_PROTECTION, false);
	}

	// quick_tiles_instant_blackout (SwitchPreferenceCompat, Default: false)
	public boolean isQuickTilesInstantBlackout() {
		return sharedPreferences.getBoolean(KEY_QUICK_TILES_INSTANT_BLACKOUT, false);
	}

	// disable_battery_optimization (SwitchPreferenceCompat, Default: false)
	public boolean isBatteryOptimizationDisabled() {
		return sharedPreferences.getBoolean(KEY_DISABLE_BATTERY_OPTIMIZATION, false);
	}

	// app_language (ListPreference, Default: "en")
	public String getAppLanguage() {
		return sharedPreferences.getString(KEY_APP_LANGUAGE, "en");
	}

}