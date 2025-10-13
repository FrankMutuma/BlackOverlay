package com.smarttechnologies.app.blackoverlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class OverlayActivity extends AppCompatActivity {

	// --- Constants ---
	public static final String OVERLAY_ACTIVITY_KILLED = "com.smarttechnologies.app.blackoverlay.OVERLAY_ACTIVITY_KILLED";
	public static final String OVERLAY_ACTIVITY_STARTED = "com.smarttechnologies.app.blackoverlay.OVERLAY_ACTIVITY_STARTED";

	private static final String TAG = "OverlayActivity";
	private static final int TAP_COUNT_TO_UNLOCK = 3;
	private static final long TAP_TIMEOUT_MS = 500;

	// --- State Variables ---
	private long lastTapTime = 0;
	private int tapCount = 0;
	private boolean isFinishing = false;

	// --- UI Views ---
	private TextView timeTextView;
	private TextView dateDayTextView;
	private TextView unlockTextView;

	// --- Managers/Services ---
	private BroadcastReceiver finishReceiver;
	private BrightnessManager brightnessManager;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.black_overlay);
		Log.d(TAG, "Activity created");

		// Initialize BrightnessManager
		brightnessManager = new BrightnessManager(this);

		// Initialize views
		timeTextView = findViewById(R.id.overlay_time);
		dateDayTextView = findViewById(R.id.overlay_date_and_day);
		unlockTextView = findViewById(R.id.overlay_unlock);

		// Start observing the static LiveData
		observeLiveData();
		setupActivity();
	}

	// --------------------------------------------------------------------------
	// Lifecycle Overrides
	// --------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "Activity resumed");

		sendStartBroadcast();
		// CRITICAL: Ensure the clock is running for the service's lifetime by registering an observer.
		ClockUtils.registerObserver();
		// Apply brightness
		applyOptimalBrightness();

		// Re-hide system bars when resuming
		getWindow().getDecorView().postDelayed(this::hideSystemBars, 100);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Send the Broadcast that the overlay is closing
		sendKilledBroadcast();
		// Unregister clock observer (decrement ref count in ClockUtils)
		ClockUtils.unregisterObserver();
		// Restore brightness
		brightnessManager.restoreBrightness();
		Log.d(TAG, "Activity paused");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Activity destroyed");
		isFinishing = true;

		// Unregister broadcast receiver (using LocalBroadcastManager)
		if (finishReceiver != null) {
			try {
				LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver);
				Log.d(TAG, "Local Broadcast receiver unregistered");
			} catch (Exception e) {
				Log.e(TAG, "Error unregistering local receiver: " + e.getMessage());
			}
		}
	}

	@Override
	public void finish() {
		super.finish();
		// No animation for seamless transition
		overridePendingTransition(0, 0);
		Log.d(TAG, "Activity finish called");
	}

	@Override
	public void onBackPressed() {
		// Disable back button
		Log.d(TAG, "Back button pressed - ignored");
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		Log.d(TAG, "Window focus changed: " + hasFocus);

		if (hasFocus) {
			// Re-hide system bars when window gains focus
			hideSystemBars();
		}
	}

	// --------------------------------------------------------------------------
	// Core Logic
	// --------------------------------------------------------------------------

	private void observeLiveData() {
		Log.i(TAG, "Observe live data called");

		// Observe the static time LiveData from ClockUtils
		ClockUtils.getTimeLiveData().observe(this, newTime -> {
			//Log.d(TAG, "Time updated to: " + newTime);
			if (timeTextView != null) {
				timeTextView.setText(newTime);
				timeTextView.setContentDescription("Current time is " + newTime);
			} else {
				Log.e(TAG, "Time textview not found");
			}
		});

		// Observe the static date LiveData from ClockUtils
		ClockUtils.getDateDayLiveData().observe(this, newDate -> {
			if (dateDayTextView != null) {
				dateDayTextView.setText(newDate);
				dateDayTextView.setContentDescription("Today's date is " + newDate);
			} else {
				Log.e(TAG, "DateDay textview not found");
			}
		});
	}

	private void setupActivity() {
		Log.d(TAG, "Activity setup Starts");
		// Setup the activity window
		initializeFullscreenOverlayFlags();

		// Register broadcast receiver for remote finish
		registerFinishReceiver();

		// Setup touch listener
		setupTouchListener();

		Log.d(TAG, "Activity setup complete");
	}

	/**
	* Helper methods to create and send the local broadcast
	*/
	private void sendStartBroadcast() {
		Log.d(TAG, "Activity is being Started. Sending 'STARTED' broadcast.");
		Intent intent = new Intent(OVERLAY_ACTIVITY_STARTED);
		intent.putExtra("activity_name", getClass().getSimpleName());
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void sendKilledBroadcast() {
		Log.d(TAG, "Activity is being destroyed. Sending 'KILLED' broadcast.");
		Intent intent = new Intent(OVERLAY_ACTIVITY_KILLED);
		intent.putExtra("activity_name", getClass().getSimpleName());
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void finishSafely() {
		if (isFinishing || isFinishing())
			return;

		isFinishing = true;
		Log.d(TAG, "Finishing activity safely");
		finish();
	}

	// --------------------------------------------------------------------------
	// UI/Window Setup
	// --------------------------------------------------------------------------

	//may review to black
	private void initializeFullscreenOverlayFlags() {
		Window window = getWindow();

		// Make window transparent

		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		// Critical flags to not interrupt background apps
		window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
		window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		window.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

		// Fullscreen flags
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

		// Keep screen on but dim
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Hide system bars
		hideSystemBars();

		Log.d(TAG, "Window setup complete");
	}

	private void hideSystemBars() {
		View decorView = getWindow().getDecorView();

		// For Android R and above
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			getWindow().setDecorFitsSystemWindows(false);
			WindowInsetsController controller = decorView.getWindowInsetsController();
			if (controller != null) {
				controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
				controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
				Log.d(TAG, "Android R+ system bars hidden");
			}
		} else {
			// For older versions
			int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			decorView.setSystemUiVisibility(uiOptions);
			Log.d(TAG, "Pre-Android R system bars hidden: " + uiOptions);
		}

		// Set navigation bar color to transparent
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().setNavigationBarColor(Color.TRANSPARENT);
			getWindow().setStatusBarColor(Color.TRANSPARENT);
		}
	}

	private void applyOptimalBrightness() {
		Window window = getWindow();
		WindowManager.LayoutParams params = window.getAttributes();

		brightnessManager.setOverlayParams(params);
		if (brightnessManager.canWriteSystemSettings()) {
			brightnessManager.applyCombinedBrightness();
		} else {
			brightnessManager.applyInAppWindowBrightness();
		}
		window.setAttributes(params);

		Log.d(TAG, "Applied optimal brightness - System control: " + brightnessManager.canWriteSystemSettings());
	}

	// --------------------------------------------------------------------------
	// Event/Receiver Handling
	// --------------------------------------------------------------------------

	private void registerFinishReceiver() {
		finishReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if ("FINISH_OVERLAY".equals(intent.getAction())) {
					Log.d(TAG, "Received local finish broadcast");
					finishSafely();
				}
			}
		};

		IntentFilter filter = new IntentFilter("FINISH_OVERLAY");
		LocalBroadcastManager.getInstance(this).registerReceiver(finishReceiver, filter);
		Log.d(TAG, "Local Broadcast receiver registered");
	}

	private void setupTouchListener() {
		View rootView = findViewById(android.R.id.content);
		if (rootView != null) {
			rootView.setOnTouchListener((v, event) -> {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					long currentTime = System.currentTimeMillis();

					if (currentTime - lastTapTime < TAP_TIMEOUT_MS) {
						tapCount++;
					} else {
						tapCount = 1;
					}
					lastTapTime = currentTime;

					Log.d(TAG, "Tap detected - count: " + tapCount);

					// Update unlock text view
					if (unlockTextView != null) {
						int remaining = TAP_COUNT_TO_UNLOCK - tapCount;
						if (remaining > 0) {
							unlockTextView.setText("Tap " + remaining + " more times to unlock.");
						} else {
							// Reset text on successful unlock or if tap count resets
							if (tapCount == 0) {
								unlockTextView.setText(R.string.unlock); // Assume R.string.unlock is defined
							} else {
								unlockTextView.setText("");
							}
						}
					}

					if (tapCount >= TAP_COUNT_TO_UNLOCK) {
						Log.d(TAG, "Triple tap detected - unlocking");
						VibrationHelper.vibrateDefault(v.getContext());
						finishSafely();
						tapCount = 0;
						return true;
					}
				}
				return true; // Consume all touch events
			});
			Log.d(TAG, "Touch listener setup complete");
		} else {
			Log.e(TAG, "Root view is null - cannot setup touch listener");
		}
	}
}