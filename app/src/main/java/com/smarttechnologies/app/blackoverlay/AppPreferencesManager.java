package com.smarttechnologies.app.blackoverlay;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferencesManager {

	private static AppPreferencesManager instance;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;

	private static final String PREF_NAME = "black_overlay_prefs";
	private static final String KEY_TOTAL_DENIALS = "total_permission_denials"; // Global counter for all prompts shown & denied/dismissed
	private static final String KEY_INITIAL_LAUNCH_PROMPT_COUNT = "initial_launch_prompt_count"; // Counter for prompts shown specifically on onCreate
	private static final String KEY_PREVENT_TOUCH = "preventTouch";
	private static final String KEY_MEDIA_CONTROL_ENABLED = "mediaEnabled";

	public static final int MAX_TOTAL_DENIALS = 9; // Max total prompts allowed across all sessions and launches
	public static final int MAX_INITIAL_LAUNCH_PROMPTS = 3; // Max times to show the prompt on first app open (onCreate)

	private AppPreferencesManager(Context context) {
		sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		editor = sharedPreferences.edit();
	}

	public static synchronized AppPreferencesManager getInstance(Context context) {
		if (instance == null) {
			instance = new AppPreferencesManager(context);
		}
		return instance;
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

}