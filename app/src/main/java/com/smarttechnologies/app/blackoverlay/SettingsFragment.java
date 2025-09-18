package com.smarttechnologies.app.blackoverlay;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsFragment extends PreferenceFragmentCompat {

	private SharedPreferences sharedPreferences;

	@Override
	public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
		// Load the preferences from the XML file
		setPreferencesFromResource(R.xml.fragment_settings, rootKey);

		// Get the default SharedPreferences
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

		// Set up the listeners for various preference items
		setupPreferenceListeners();
	}

	private void setupPreferenceListeners() {
		// Find the "lockt_ype" ListPreference and update its summary
		ListPreference lockTypeListPreference = findPreference("lock_type");
		if (lockTypeListPreference != null) {
			lockTypeListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
				// Update the summary to the new selected value
				lockTypeListPreference.setSummary(newValue.toString());
				Toast.makeText(getContext(), "Theme set to: " + newValue, Toast.LENGTH_SHORT).show();
				return true;
			});
			// Set the initial summary
			lockTypeListPreference.setSummary(lockTypeListPreference.getEntry());
		}

		// Find the "Upgrade to Pro" preference and set a listener
		Preference upgradeProPreference = findPreference("settings_upgrade_pro");
		if (upgradeProPreference != null) {
			upgradeProPreference.setOnPreferenceClickListener(preference -> {
				Toast.makeText(getContext(), "Upgrade to Pro clicked", Toast.LENGTH_SHORT).show();
				return true;
			});
		}

		// Find the "Theme" ListPreference and update its summary
		ListPreference themeListPreference = findPreference("theme");
		if (themeListPreference != null) {
			themeListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
				// Update the summary to the new selected value
				themeListPreference.setSummary(newValue.toString());
				Toast.makeText(getContext(), "Theme set to: " + newValue, Toast.LENGTH_SHORT).show();
				return true;
			});
			// Set the initial summary
			themeListPreference.setSummary(themeListPreference.getEntry());
		}

		// Find the "Always-On Display" SwitchPreferenceCompat and set a listener
		SwitchPreferenceCompat alwaysOnDisplayPreference = findPreference("always_on_display");
		if (alwaysOnDisplayPreference != null) {
			alwaysOnDisplayPreference.setOnPreferenceChangeListener((preference, newValue) -> {
				boolean isChecked = (boolean) newValue;
				alwaysOnDisplayPreference
						.setSummary(isChecked ? "Always-on display is enabled, tap to unlock the screen"
								: "Always-on display is disabled, tap to wake the screen");
				Toast.makeText(getContext(), "Always-On Display: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT)
						.show();
				return true;
			});
			// Set the initial summary based on the saved state
			boolean isChecked = sharedPreferences.getBoolean("always_on_display", false);
			alwaysOnDisplayPreference.setSummary(isChecked ? "Always-on display is enabled, tap to unlock the screen"
					: "Always-on display is disabled, tap to wake the screen");
		}

		// Set up listeners for other SwitchPreferenceCompat items
		setupSwitchPreferenceListener("skip_unlock_screen", "Skip Unlock Screen");
		setupSwitchPreferenceListener("biometric_auth", "Biometric Auth");
		setupSwitchPreferenceListener("pocket_detection", "Pocket Detection");
		setupSwitchPreferenceListener("reduce_brightness", "Reduce Brightness");
		setupSwitchPreferenceListener("oled_burn_in_protection", "OLED Burn-In Protection");
		setupSwitchPreferenceListener("quick_tiles_instant_blackout", "Quick Tiles Instant Blackout");
		setupSwitchPreferenceListener("disable_battery_optimization", "Battery Optimization");
		setupSwitchPreferenceListener("hide_floating_button", "Hide Floating Button");

		// Set up listeners for other ListPreference items
		setupListPreferenceListener("floating_button_action", "Floating Button Action");
		setupListPreferenceListener("app_language", "App Language");
		setupListPreferenceListener("taps_to_wake", "Taps to Wake");

		// Find the "Notifications" preference and set a listener
		Preference notificationsPreference = findPreference("settings_notifications");
		if (notificationsPreference != null) {
			notificationsPreference.setOnPreferenceClickListener(preference -> {
				Toast.makeText(getContext(), "Notifications settings clicked", Toast.LENGTH_SHORT).show();
				return true;
			});
		}
	}

	private void setupSwitchPreferenceListener(String key, String toastMessage) {
		SwitchPreferenceCompat preference = findPreference(key);
		if (preference != null) {
			preference.setOnPreferenceChangeListener((pref, newValue) -> {
				boolean isChecked = (boolean) newValue;
				// You can add more specific summary logic here if needed, similar to "always_on_display"
				Toast.makeText(getContext(), toastMessage + ": " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT)
						.show();
				return true;
			});
		}
	}

	private void setupListPreferenceListener(String key, String toastMessage) {
		ListPreference preference = findPreference(key);
		if (preference != null) {
			// Set up a listener to update the summary and show a Toast
			preference.setOnPreferenceChangeListener((pref, newValue) -> {
				int index = preference.findIndexOfValue(newValue.toString());
				CharSequence newEntry = preference.getEntries()[index];
				preference.setSummary(newEntry);
				Toast.makeText(getContext(), toastMessage + " set to: " + newEntry, Toast.LENGTH_SHORT).show();
				return true;
			});
			// Set the initial summary based on the current value
			preference.setSummary(preference.getEntry());
		}
	}
}