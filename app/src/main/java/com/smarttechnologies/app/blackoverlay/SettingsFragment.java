package com.smarttechnologies.app.blackoverlay;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

	// CheckBoxes
	private CheckBox checkboxAlwaysOn;
	private CheckBox checkboxSkipUnlock;
	private CheckBox checkboxBiometricAuth;
	private CheckBox checkboxPocketDetection;
	private CheckBox checkboxReduceBrightness;
	private CheckBox checkboxOledBurnIn;
	private CheckBox checkboxQuickTiles;
	private CheckBox checkboxBatteryOptimization;
	private CheckBox checkboxHideFloatingButton;

	// Clickable sections
	private LinearLayout settingsUpgradePro;
	private LinearLayout settingsTheme;
	private LinearLayout settingsFloatingButtonAction;
	private LinearLayout settingsNotifications;
	private LinearLayout settingsAppLanguage;
	private LinearLayout settingsTapsToWake;

	public SettingsFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_settings, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Find all views
		findAllViews(view);

		// Set up all listeners
		setupListeners();
	}

	private void findAllViews(View view) {
		// CheckBoxes
		checkboxAlwaysOn = view.findViewById(R.id.checkbox_always_on);
		checkboxSkipUnlock = view.findViewById(R.id.checkbox_skip_unlock);
		checkboxBiometricAuth = view.findViewById(R.id.checkbox_biometric_auth);
		checkboxPocketDetection = view.findViewById(R.id.checkbox_pocket_detection);
		checkboxReduceBrightness = view.findViewById(R.id.checkbox_reduce_brightness);
		checkboxOledBurnIn = view.findViewById(R.id.checkbox_oled_burn_in);
		checkboxQuickTiles = view.findViewById(R.id.checkbox_quick_tiles);
		checkboxBatteryOptimization = view.findViewById(R.id.checkbox_battery_optimization);
		checkboxHideFloatingButton = view.findViewById(R.id.checkbox_hide_floating_button);

		// Clickable sections
		settingsUpgradePro = view.findViewById(R.id.settings_upgrade_pro);
		settingsTheme = view.findViewById(R.id.settings_theme);
		settingsFloatingButtonAction = view.findViewById(R.id.settings_floating_button_action);
		settingsNotifications = view.findViewById(R.id.settings_notifications);
		settingsAppLanguage = view.findViewById(R.id.settings_app_language);
		settingsTapsToWake = view.findViewById(R.id.settings_taps_to_wake);
	}

	private void setupListeners() {
		// Click listeners for sections that open a new activity or dialog
		settingsUpgradePro.setOnClickListener(
				v -> Toast.makeText(getContext(), "Upgrade to Pro clicked", Toast.LENGTH_SHORT).show());
		settingsTheme.setOnClickListener(v -> Toast.makeText(getContext(), "Theme clicked", Toast.LENGTH_SHORT).show());
		settingsFloatingButtonAction.setOnClickListener(
				v -> Toast.makeText(getContext(), "Floating Button Action clicked", Toast.LENGTH_SHORT).show());
		settingsNotifications.setOnClickListener(
				v -> Toast.makeText(getContext(), "Notifications clicked", Toast.LENGTH_SHORT).show());
		settingsAppLanguage.setOnClickListener(
				v -> Toast.makeText(getContext(), "App Language clicked", Toast.LENGTH_SHORT).show());
		settingsTapsToWake.setOnClickListener(
				v -> Toast.makeText(getContext(), "Taps to Wake clicked", Toast.LENGTH_SHORT).show());

		// Checkbox change listeners
		checkboxAlwaysOn.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Toast.makeText(getContext(), "Always-On Display: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
		});

		checkboxSkipUnlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Toast.makeText(getContext(), "Skip Unlock Screen: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT)
					.show();
		});

		checkboxBiometricAuth.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Toast.makeText(getContext(), "Biometric Auth: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
		});

		checkboxPocketDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Toast.makeText(getContext(), "Pocket Detection: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
		});

		checkboxReduceBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Toast.makeText(getContext(), "Reduce Brightness: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
		});

		checkboxOledBurnIn.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Toast.makeText(getContext(), "OLED Burn-In Protection: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT)
					.show();
		});

		checkboxQuickTiles.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Toast.makeText(getContext(), "Quick Tiles: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
		});

		checkboxBatteryOptimization.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Toast.makeText(getContext(), "Battery Optimization: " + (isChecked ? "Disabled" : "Enabled"),
					Toast.LENGTH_SHORT).show();
		});

		checkboxHideFloatingButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Toast.makeText(getContext(), "Floating Button: " + (isChecked ? "Hidden" : "Visible"), Toast.LENGTH_SHORT)
					.show();
		});
	}
}