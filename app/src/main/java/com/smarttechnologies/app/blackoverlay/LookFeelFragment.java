package com.smarttechnologies.app.blackoverlay;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // <-- NEW IMPORT

public class LookFeelFragment extends Fragment {
	private AppPreferencesManager appSettingsManager;
	private SeekBar seekBarLockSize;
	private Switch switchMediaControls;
	private ImageView previewLock1, previewLock2, previewLock3;
	private Handler debounceHandler = new Handler(Looper.getMainLooper());
	private Runnable debounceRunnable;
	private int lastSentSize = -1;
	//declare the textviews
	private TextView txtclock1, txtclock2, txtclock3;

	public LookFeelFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_look_and_feel, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		//initialize AppPreferencesManager
		appSettingsManager = AppPreferencesManager.getInstance(requireContext());

		// Find the new UI components
		//initialize the textviews
		txtclock1 = view.findViewById(R.id.TxtClock1);
		txtclock2 = view.findViewById(R.id.TxtClock2);
		txtclock3 = view.findViewById(R.id.TxtClock3);

		// Find all the CardView previews from the layout
		CardView cardViewClock1 = view.findViewById(R.id.cardViewClock1);
		CardView cardViewClock2 = view.findViewById(R.id.cardViewClock2);
		CardView cardViewClock3 = view.findViewById(R.id.cardViewClock3);
		CardView cardViewLock1 = view.findViewById(R.id.cardViewLock1);
		CardView cardViewLock2 = view.findViewById(R.id.cardViewLock2);
		CardView cardViewLock3 = view.findViewById(R.id.cardViewLock3);

		//seekbar
		seekBarLockSize = view.findViewById(R.id.seekBarLockSize);
		//preview imageviews
		previewLock1 = view.findViewById(R.id.cardViewLock1).findViewById(R.id.lock_icon_preview_1);
		previewLock2 = view.findViewById(R.id.cardViewLock2).findViewById(R.id.lock_icon_preview_2);
		previewLock3 = view.findViewById(R.id.cardViewLock3).findViewById(R.id.lock_icon_preview_3);

		switchMediaControls = view.findViewById(R.id.switchMediaControls);

		Switch switchNotifications = view.findViewById(R.id.switchNotifications);
		CheckBox checkBoxBatteryPercentage = view.findViewById(R.id.checkBoxBatteryPercentage);

		// Observe ClockUtils LiveData directly
		ObserveLiveData();

		// Set up listeners for the Switches
		// 1. LOAD the saved state when the fragment starts
		boolean savedMediaControlsState = appSettingsManager.getMediaControlsEnabled();
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
		//seekbar settings
		// Set seekbar range (10-100 dp)
		seekBarLockSize.setMax(
				AppPreferencesManager.getMaxFloatingLockSize() - AppPreferencesManager.getMinFloatingLockSize());
		// Set the current value from preferences
		int currentSize = appSettingsManager.getFloatingLockSize();
		seekBarLockSize.setProgress(currentSize - AppPreferencesManager.getMinFloatingLockSize());

		// Set up listener for the SeekBar
		seekBarLockSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					// Progress is 0-90, we want 10-100
					int actualSize = progress + AppPreferencesManager.getMinFloatingLockSize();

					// Update the previews in real-time
					updateLockSizePreviews(actualSize);

					// Debounce the floating button updates
					if (debounceRunnable != null) {
						debounceHandler.removeCallbacks(debounceRunnable);
					}

					debounceRunnable = new Runnable() {
						@Override
						public void run() {
							if (lastSentSize != actualSize) {
								notifyServiceOfSizeChange(actualSize);
								lastSentSize = actualSize;
							}
						}
					};

					// Update the floating button with a slight delay (50ms)
					debounceHandler.postDelayed(debounceRunnable, 50);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// Not needed
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// Save the value when user stops dragging
				int actualSize = seekBar.getProgress() + AppPreferencesManager.getMinFloatingLockSize();
				appSettingsManager.setFloatingLockSize(actualSize);

				// Final update to the floating button with the saved size
				notifyServiceOfSizeChange(actualSize);

				Toast.makeText(getContext(), "Lock size set to: " + actualSize + "dp", Toast.LENGTH_SHORT).show();
			}
		});

		// Initialize the previews with current size
		updateLockSizePreviews(currentSize);

		// ... rest of your UI setup code ...
	}

	private void ObserveLiveData() {
		// Observe the static time LiveData from ClockUtils
		ClockUtils.getTimeLiveData().observe(getViewLifecycleOwner(), newTime -> {
			// This code runs when the LiveData changes
			txtclock1.setText(newTime);
			txtclock2.setText(newTime);
			txtclock3.setText(newTime);
		});

	}

	private void updateLockSizePreviews(int sizeDp) {
		// Convert dp to pixels using the utility class
		int sizePx = DisplayUtils.dpToPx(requireContext(), sizeDp);

		// Update each preview if it exists
		if (previewLock1 != null) {
			ViewGroup.LayoutParams params = previewLock1.getLayoutParams();
			params.width = sizePx;
			params.height = sizePx;
			previewLock1.setLayoutParams(params);
			previewLock1.requestLayout();
		}

		if (previewLock2 != null) {
			ViewGroup.LayoutParams params = previewLock2.getLayoutParams();
			params.width = sizePx;
			params.height = sizePx;
			previewLock2.setLayoutParams(params);
			previewLock2.requestLayout();
		}

		if (previewLock3 != null) {
			ViewGroup.LayoutParams params = previewLock3.getLayoutParams();
			params.width = sizePx;
			params.height = sizePx;
			previewLock3.setLayoutParams(params);
			previewLock3.requestLayout();
		}
	}

	private void notifyServiceOfSizeChange(int newSize) {
		// Send a broadcast to the service to update the floating button size
		Intent intent = new Intent("FLOATING_LOCK_SIZE_CHANGED");
		intent.putExtra("size", newSize);
		// Use LocalBroadcastManager for secure, intra-application communication
		LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		// Clean up the handler to prevent memory leaks
		if (debounceHandler != null) {
			debounceHandler.removeCallbacksAndMessages(null);
		}
	}

}