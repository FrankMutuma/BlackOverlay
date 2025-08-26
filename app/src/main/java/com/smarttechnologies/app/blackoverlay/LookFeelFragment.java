package com.smarttechnologies.app.blackoverlay;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class LookFeelFragment extends Fragment {

	public LookFeelFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_look_and_feel, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Find all the CardView previews from the layout
		CardView cardViewClock1 = view.findViewById(R.id.cardViewClock1);
		CardView cardViewClock2 = view.findViewById(R.id.cardViewClock2);
		CardView cardViewClock3 = view.findViewById(R.id.cardViewClock3);
		CardView cardViewLock1 = view.findViewById(R.id.cardViewLock1);
		CardView cardViewLock2 = view.findViewById(R.id.cardViewLock2);
		CardView cardViewLock3 = view.findViewById(R.id.cardViewLock3);

		// Find the new UI components
		SeekBar seekBarLockSize = view.findViewById(R.id.seekBarLockSize);
		Switch switchMediaControls = view.findViewById(R.id.switchMediaControls);
		Switch switchNotifications = view.findViewById(R.id.switchNotifications);
		CheckBox checkBoxBatteryPercentage = view.findViewById(R.id.checkBoxBatteryPercentage);

		// Set up click listeners for the CardView previews
		cardViewClock1.setOnClickListener(
				v -> Toast.makeText(getContext(), "Clock Style 1 selected", Toast.LENGTH_SHORT).show());
		cardViewClock2.setOnClickListener(
				v -> Toast.makeText(getContext(), "Clock Style 2 selected", Toast.LENGTH_SHORT).show());
		cardViewClock3.setOnClickListener(
				v -> Toast.makeText(getContext(), "Clock Style 3 selected", Toast.LENGTH_SHORT).show());
		cardViewLock1.setOnClickListener(
				v -> Toast.makeText(getContext(), "Lock Style 1 selected", Toast.LENGTH_SHORT).show());
		cardViewLock2.setOnClickListener(
				v -> Toast.makeText(getContext(), "Lock Style 2 selected", Toast.LENGTH_SHORT).show());
		cardViewLock3.setOnClickListener(
				v -> Toast.makeText(getContext(), "Lock Style 3 selected", Toast.LENGTH_SHORT).show());

		// Set up listener for the SeekBar
		seekBarLockSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// You can get the new size here
				Toast.makeText(getContext(), "Lock size set to: " + progress, Toast.LENGTH_SHORT).show();
				// TODO: Add logic to update the lock icon size
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		// Set up listeners for the Switches

		// 1. LOAD the saved state when the fragment starts
		boolean savedMediaControlsState = AppPreferencesManager.getInstance(requireContext()).getMediaControlsEnabled();
		switchMediaControls.setChecked(savedMediaControlsState);

		// 2. SAVE the state when the user changes it
		switchMediaControls.setOnCheckedChangeListener((buttonView, isChecked) -> {
			// Save the state
			AppPreferencesManager.getInstance(requireContext()).setMediaControlsEnabled(isChecked);
			// You can keep the toast for feedback
			String message = isChecked ? "Media controls ON" : "Media controls OFF";
			Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
		});

		switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
			String message = isChecked ? "Notifications ON" : "Notifications OFF";
			Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
			// TODO: Add logic to save the state
		});

		// Set up listener for the CheckBox
		checkBoxBatteryPercentage.setOnCheckedChangeListener((buttonView, isChecked) -> {
			String message = isChecked ? "Battery percentage enabled" : "Battery percentage disabled";
			Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
			// TODO: Add logic to save the state
		});
	}
}