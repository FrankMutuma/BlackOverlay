package com.smarttechnologies.app.blackoverlay;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.View;
import android.widget.ImageView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class FloatingButtonService extends Service {
	private WindowManager windowManager;
	private AppPreferencesManager appPreferencesManager;

	// Separated Receivers
	private BroadcastReceiver screenStateReceiver;
	private BroadcastReceiver sizeChangeReceiver;
	private BroadcastReceiver overlayStateReceiver;
	private BroadcastReceiver stateRequestReceiver; // For state synchronization with MainActivity

	private View floatingView;
	private View overlay; // Used for non-Activity based overlays

	// Constants
	public static final String ACTION_SERVICE_STATE_CHANGED = "com.smarttechnologies.app.blackoverlay.SERVICE_STATE_CHANGED";
	public static final String ACTION_REPORT_STATE_REQUEST = "com.smarttechnologies.app.blackoverlay.REPORT_STATE_REQUEST";
	private static final String CHANNEL_ID = "com.smarttechnologies.app.blackoverlay.FloatingButtonServiceChannel";
	private static final String TAG = "FloatingButtonService";
	private static final String ACTION_TOGGLE_OVERLAY = "ACTION_TOGGLE_OVERLAY";
	private static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
	private boolean isOverlayActive = false;
	private boolean isStopPending = false; // Tracks if a stop was requested while overlay was active
	private int floatingButtonSize;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// 1. Initialize the Singleton ClockUtils (CRITICAL)
		ClockUtils.initialize();

		// 2. Build and start the foreground service
		NotificationChannel channel = new NotificationChannel(CHANNEL_ID, R.string.app_name + " Updates",
				NotificationManager.IMPORTANCE_LOW);
		NotificationManager manager = getSystemService(NotificationManager.class);
		manager.createNotificationChannel(channel);

		startForeground(1, buildNotification());

		// 3. Get initial size and initialize window manager
		appPreferencesManager = AppPreferencesManager.getInstance(this);
		floatingButtonSize = appPreferencesManager.getFloatingLockSize();
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		// 4. Register Receivers
		registerReceivers();

		// 5. Create the floating button
		createFloatingButton();
		Log.i(TAG, "Floating button creation complete.");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Broadcast that the service has started
		SharedViewModel.setOverlayServiceRunningStatus(true);
		// Handle incoming actions from the notification.
		if (intent == null || intent.getAction() == null)
			//TODO: state restore implementation
			return START_STICKY;
		Log.d(TAG, "Action received from notification: " + intent.getAction());

		switch (intent.getAction()) {
		case ACTION_TOGGLE_OVERLAY:
			if (isOverlayActive) {

				hideOverlay();
			} else {
				startOverlay();
			}
			break;
		case ACTION_STOP_SERVICE:
			if (isOverlayActive) {
				Log.d(TAG, "Notification STOP received. Killing active overlay first.");
				isStopPending = true; // Set flag to proceed with shutdown after overlay is killed
				// Call existing method to dismiss the overlay
				hideOverlay();
			} else {
				Log.d(TAG, "Notification STOP received. Overlay inactive. Stopping self immediately.");
				stopSelf(); // Safe to stop immediately
			}
			break;
		}

		return START_STICKY;

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//unregister screenStateReceiver
		if (screenStateReceiver != null) {
			unregisterReceiver(screenStateReceiver);
			Log.d(TAG, "ScreenStateReceiver unregistered.");
		}
		// 1. Unregister the local receivers
		if (overlayStateReceiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(overlayStateReceiver);
		}
		if (sizeChangeReceiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(sizeChangeReceiver);
		}
		if (stateRequestReceiver != null) { // UNREGISTER the new receiver
			LocalBroadcastManager.getInstance(this).unregisterReceiver(stateRequestReceiver);
		}

		// 2. Remove views
		if (floatingView != null && windowManager != null) {
			windowManager.removeView(floatingView);
		}
		if (overlay != null) {
			try {
				windowManager.removeView(overlay);
			} catch (IllegalArgumentException e) {
				Log.w(TAG, "Overlay view was already removed or never attached.");
			}
		}

		// 3. Stop being a foreground service
		stopForeground(STOP_FOREGROUND_REMOVE);

		// 4. set that the service has stopped
		SharedViewModel.setOverlayServiceRunningStatus(false);
	}

	private void registerReceivers() {

		// --- Global Receiver Registration ---

		IntentFilter screenFilter = new IntentFilter();
		screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
		screenFilter.addAction(Intent.ACTION_USER_PRESENT);

		screenStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent == null || intent.getAction() == null) {
					return;
				}
				String action = intent.getAction();
				Log.d(TAG, "Received action: " + action);

				switch (action) {
				case Intent.ACTION_SCREEN_OFF:
					// 1. Screen is turning off (user pressed power button).
					// HIDE the overlay and save power.
					hideOverlay();
					break;
				case Intent.ACTION_USER_PRESENT:
					// 2. User has unlocked the device (Keyguard dismissed).
					// SHOW the overlay now that the user is active on the home screen/app.
					startOverlay();
					break;
				// Optional: You might not strictly need SCREEN_ON, but it can be useful for debugging.
				// case Intent.ACTION_SCREEN_ON:
				//    Log.d(TAG, "Screen ON (before unlock)");
				//    break;
				}
			}
		};

		registerReceiver(screenStateReceiver, screenFilter);
		Log.d(TAG, "ScreenStateReceiver registered (Global)");

		// --- Local Receiver Registration ---

		// 1. Overlay State Receiver (Combines the filters for a cleaner look)
		IntentFilter overlayFilter = new IntentFilter();
		overlayFilter.addAction(OverlayActivity.OVERLAY_ACTIVITY_STARTED);
		overlayFilter.addAction(OverlayActivity.OVERLAY_ACTIVITY_KILLED);

		overlayStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent == null || intent.getAction() == null) {
					return;
				}
				String action = intent.getAction();

				if (OverlayActivity.OVERLAY_ACTIVITY_STARTED.equals(action)) {
					Log.i(TAG, "OverlayActivity has been STARTED! Hiding floating button.");

					// Sync state
					isOverlayActive = true;

					// UI Updates
					if (floatingView != null)
						floatingView.setVisibility(View.GONE);
					updateNotification();
				} else if (OverlayActivity.OVERLAY_ACTIVITY_KILLED.equals(action)) {
					Log.i(TAG, "OverlayActivity has been destroyed! Showing floating button.");

					// Sync state
					isOverlayActive = false;

					// UI Updates
					if (floatingView != null)
						floatingView.setVisibility(View.VISIBLE);
					updateNotification();

					// <<< IMPROVEMENT: Check for pending service stop >>>
					if (isStopPending) {
						Log.i(TAG, "Overlay confirmed killed by service stop request. Initiating final shutdown.");
						isStopPending = false; // Reset the flag
						stopSelf(); // Now it's safe to destroy the service
					}

				}

			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(overlayStateReceiver, overlayFilter);
		Log.d(TAG, "Overlay State Receiver registered (Local)");

		// 2. Size Change Receiver
		sizeChangeReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent == null || intent.getAction() == null) {
					return;
				}
				if ("FLOATING_LOCK_SIZE_CHANGED".equals(intent.getAction())) {
					int newSize = intent.getIntExtra("size", floatingButtonSize);
					updateFloatingButtonSize(newSize);
				}
			}

		};
		LocalBroadcastManager.getInstance(this).registerReceiver(sizeChangeReceiver,
				new IntentFilter("FLOATING_LOCK_SIZE_CHANGED"));
		Log.d(TAG, "Size Change Receiver registered (Local)");

		// 3. State Request Receiver
		stateRequestReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (ACTION_REPORT_STATE_REQUEST.equals(intent.getAction())) {
					Log.d(TAG, "State report request received. Responding with current state.");

					// Send the current service running state back to MainActivity
					Intent stateIntent = new Intent(ACTION_SERVICE_STATE_CHANGED);
					// Since this code only runs if the service is alive, the answer is always 'true'.
					stateIntent.putExtra("is_running", true);
					LocalBroadcastManager.getInstance(context).sendBroadcast(stateIntent);
				}
			}

		};
		LocalBroadcastManager.getInstance(this).registerReceiver(stateRequestReceiver,
				new IntentFilter(ACTION_REPORT_STATE_REQUEST));
		Log.d(TAG, "State Request Receiver registered (Local)");

	}

	public void startOverlay() {
		String lockType = appPreferencesManager.getLockType();

		final String VALUE_BLACK_OVERLAY = "black_overlay";
		final String VALUE_PRIVACY_OVERLAY = "privacy_overlay";

		switch (lockType) {

		case VALUE_BLACK_OVERLAY:
			Log.d(TAG, "Applying Black Overlay.");
			showBlackOverlay();
			break;

		case VALUE_PRIVACY_OVERLAY:
			Log.d(TAG, "Applying Privacy Overlay (Transparent/Dimmed).");
			showPrivacyOverlay();
			break;

		default:
			Log.d(TAG, "Unknown lock type. Defaulting to Black Overlay.");
			showBlackOverlay();
			break;
		}
	}

	private void showBlackOverlay() {
		Log.d(TAG, "Starting BlackOverlay");

		try {
			Intent intent = new Intent(this, OverlayActivity.class);
			intent.addFlags(
					Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION
							| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

			startActivity(intent);

			Log.d(TAG, "OverlayActivity startRequest successfully Sent");

		} catch (Exception e) {
			Log.e(TAG, "OverlayActivity startRequest Failed: " + e.getMessage());
		} finally {
			// NOTE: Vibration helper is handled elsewhere
			VibrationHelper.vibrateDefault(this);
		}
	}

	private void showPrivacyOverlay() {
		Float brightness = 0.5f;
		Boolean preventTouch = false;
		Log.d(TAG, "PrivacyOverlay Started");
		// Inflate and show the touchable overlay
		if (overlay == null) {
			overlay = LayoutInflater.from(this).inflate(R.layout.privacy_overlay, null);

			// 1. Set the background color to Black
			// You'll need to import android.graphics.Color
			overlay.setBackgroundColor(Color.BLACK);

			// 2. Set the alpha (transparency) of the overlay View to 50% (0.5f)
			//overlay.setAlpha(brightness);
			WindowManager.LayoutParams params;
			if (!preventTouch) {
				// Set up window manager params and add view
				params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
						WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,

						// --- MODIFIED FLAGS FOR TOUCH PASS-THROUGH ---
						WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE // <-- THIS IS THE KEY CHANGE
								| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
								| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
						// ---------------------------------------------

						PixelFormat.TRANSLUCENT);
			} else {

				// Set up window manager params and add view
				params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
						WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
						WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
								| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
								| WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
								| WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
								| WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
								| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
						PixelFormat.TRANSLUCENT);
			}
			params.alpha = brightness;
			try {
				windowManager.addView(overlay, params);
			} catch (Exception e) {
				Log.w(TAG, "unable yo add overlay" + e);
			}
		} else

		{
			overlay.setVisibility(View.VISIBLE);
		}

		// NOTE: Vibration helper is handled elsewhere
		VibrationHelper.vibrateDefault(this);

		// Sync state and UI
		if (floatingView != null)
			floatingView.setVisibility(View.GONE);
		isOverlayActive = true;

		updateNotification();

	}

	private void hideOverlay() {
		Log.d(TAG, "Hiding LockScreen.");
		if (!isOverlayActive) {
			Log.d(TAG, "Hiding LockScreen - No Overlay is active");
			return;
		}
		if (overlay != null) {
			overlay.setVisibility(View.GONE);
			isOverlayActive = false;
			if (floatingView != null) {
				floatingView.setVisibility(View.VISIBLE);
			}
			updateNotification();

			// Check for pending stop even if KILLED broadcast might be missed
			if (isStopPending) {
				Log.i(TAG, "Total Kill Fallback executed. Initiating final shutdown immediately.");
				isStopPending = false;
				stopSelf();
			}

			Log.d(TAG, "Privacy Overlay Succesfully Hidden ");
		} else {
			hideBlackOverlay();
		}
	}

	private void hideBlackOverlay() {
		Log.d(TAG, "Hiding black Overlay - Attempting Local Broadcast first.");

		boolean broadcastSuccess = false;
		try {
			// --- 1. PRIMARY METHOD: Local Broadcast (Cleanest) ---
			Intent broadcastIntent = new Intent("FINISH_OVERLAY");
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
			broadcastSuccess = true;
			Log.d(TAG, "M1 BlackOverlay Kill Request Sent via Local Broadcast (FINISH_OVERLAY)");

		} catch (Exception e) {
			Log.e(TAG, "M1 BlackOverlay Kill Request (Broadcast) Failed: " + e.getMessage());
			// Fall through to the fallback method
		}

		if (!broadcastSuccess) {
			// --- 2. FALLBACK METHOD: Destructive Intent (Guaranteed Kill) ---
			Log.w(TAG, "M1 Broadcast failed. Executing M2 Total Kill Fallback.");
			try {
				Intent intent = new Intent(this, OverlayActivity.class);

				// Combination for total stack destruction:
				// CLEAR_TASK: Clears all activities in the task.
				// NEW_TASK: Required when starting from a Service.
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

				startActivity(intent);
				Log.d(TAG, "M2 BlackOverlay Total Kill Intent Sent (CLEAR_TASK).");

				// IMPORTANT NOTE for Fallback:
				// This flag combination might prevent the OverlayActivity from
				// sending the 'KILLED' broadcast. We must manually handle the state
				// transition as if 'KILLED' was received.

				// Since M2 guarantees destruction, we force the state change:
				// This is only done on failure of the primary method!
				isOverlayActive = false;
				if (floatingView != null) {
					floatingView.setVisibility(View.VISIBLE);
				}
				updateNotification();

				// Check for pending stop even if KILLED broadcast might be missed
				if (isStopPending) {
					Log.i(TAG, "Total Kill Fallback executed. Initiating final shutdown immediately.");
					isStopPending = false;
					stopSelf();
				}

			} catch (Exception e) {
				Log.e(TAG, "M2 BlackOverlay Total Kill Request Failed: " + e.getMessage());
			}
		}
		// NOTE: Vibration helper is handled elsewhere
		VibrationHelper.vibrateDefault(this);
	}

	private void createFloatingButton() {
		// 1. Inflate the floating button layout.
		floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null);

		// 2. Find the ImageView and set its initial size
		ImageView iconView = floatingView.findViewById(R.id.floating_button_icon);

		if (iconView != null) {
			ViewGroup.LayoutParams iconParams = iconView.getLayoutParams();
			// NOTE: Assuming DisplayUtils.dpToPx(this, size) is handled elsewhere
			int sizeInPx = DisplayUtils.dpToPx(this, floatingButtonSize);
			iconParams.width = sizeInPx;
			iconParams.height = sizeInPx;
			iconView.setLayoutParams(iconParams);
		} else {
			Log.e(TAG, "Icon view not found in layout!");
		}

		// 3. Set the layout parameters for the floating view.
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				DisplayUtils.dpToPx(this, floatingButtonSize), DisplayUtils.dpToPx(this, floatingButtonSize),
				WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);

		// Set position
		params.gravity = Gravity.TOP | Gravity.START;
		params.x = 0;
		params.y = 100;

		// 4. Add the view to the window manager.
		try {
			windowManager.addView(floatingView, params);
		} catch (Exception e) {
			Log.w(TAG, "unable to add floating view " + e);
		}

		// Add a combined touch and click listener to the view
		floatingView.setOnTouchListener(new View.OnTouchListener() {
			private int initialX;
			private int initialY;
			private float initialTouchX;
			private float initialTouchY;
			private long startClickTime;
			private final static int CLICK_ACTION_THRESHOLD = 200;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					initialX = params.x;
					initialY = params.y;
					initialTouchX = event.getRawX();
					initialTouchY = event.getRawY();
					startClickTime = System.currentTimeMillis();
					return true;

				case MotionEvent.ACTION_MOVE:
					params.x = initialX + (int) (event.getRawX() - initialTouchX);
					params.y = initialY + (int) (event.getRawY() - initialTouchY);
					windowManager.updateViewLayout(floatingView, params);
					return true;

				case MotionEvent.ACTION_UP:
					long clickDuration = System.currentTimeMillis() - startClickTime;
					if (clickDuration < CLICK_ACTION_THRESHOLD) {
						// This is a click event
						if (isOverlayActive) {
							hideBlackOverlay();
						} else {
							startOverlay();
						}
					}
					return true;
				}
				return false;
			}
		});
	}

	private void updateFloatingButtonSize(int newSize) {
		// Only update if the size actually changed
		if (floatingButtonSize == newSize) {
			return;
		}

		floatingButtonSize = newSize;

		if (floatingView != null && windowManager != null) {
			// Update the layout parameters
			WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
			// NOTE: Assuming DisplayUtils.dpToPx(this, size) is handled elsewhere
			int newSizePx = DisplayUtils.dpToPx(this, newSize);

			// Only update if the size actually changed
			if (params.width != newSizePx || params.height != newSizePx) {
				params.width = newSizePx;
				params.height = newSizePx;

				// Update the view
				windowManager.updateViewLayout(floatingView, params);

				// Also update the ImageView inside
				ImageView iconView = floatingView.findViewById(R.id.floating_button_icon);
				if (iconView != null) {
					ViewGroup.LayoutParams iconParams = iconView.getLayoutParams();
					iconParams.width = newSizePx;
					iconParams.height = newSizePx;
					iconView.setLayoutParams(iconParams);
				}
			}
		}
	}

	private Notification buildNotification() {
		// Intent to open the MainActivity when the notification body is clicked
		Intent openAppIntent = new Intent(this, MainActivity.class);
		openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent openAppPendingIntent = PendingIntent.getActivity(this, 101, openAppIntent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Create a notification builder
		Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
				.setContentTitle(getString(R.string.notification_title))
				.setSmallIcon(R.drawable.ic_play_arrow_white_24dp).setContentIntent(openAppPendingIntent) // Clicking notification opens app
				.setOngoing(true).setCategory(Notification.CATEGORY_SERVICE)
				.setVisibility(Notification.VISIBILITY_PUBLIC);

		// Set content text based on state
		String statusText = isOverlayActive ? getString(R.string.notification_status_active)
				: getString(R.string.notification_status_hidden);
		builder.setContentText(getString(R.string.tap_to_open) + "\n" + statusText);

		// Toggle Overlay Action
		Intent toggleIntent = new Intent(this, FloatingButtonService.class);
		toggleIntent.setAction(ACTION_TOGGLE_OVERLAY);
		PendingIntent togglePendingIntent = PendingIntent.getService(this, 102, toggleIntent,
				PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Stop Service Action
		Intent stopIntent = new Intent(this, FloatingButtonService.class);
		stopIntent.setAction(ACTION_STOP_SERVICE);
		PendingIntent stopPendingIntent = PendingIntent.getService(this, 103, stopIntent,
				PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Add actions
		int actionIcon = isOverlayActive ? R.drawable.ic_lock_open_white_24dp : R.drawable.ic_lock_white_24dp;
		String actionText = isOverlayActive ? getString(R.string.notification_action_unlock)
				: getString(R.string.notification_action_lock);

		builder.addAction(actionIcon, actionText, togglePendingIntent);
		builder.addAction(R.drawable.ic_stop_white_24dp, getString(R.string.notification_action_stop),
				stopPendingIntent);

		return builder.build();
	}

	private void updateNotification() {
		Notification notification = buildNotification();
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			notificationManager.notify(1, notification);
		}
	}
}