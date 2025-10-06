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
	private BroadcastReceiver sizeChangeReceiver;
	private BroadcastReceiver overlayStateReceiver;
	private BroadcastReceiver stateRequestReceiver; // NEW: For state synchronization with MainActivity

	private View floatingView;
	private View overlay; // Used for non-Activity based overlays

	// Constants
	public static final String ACTION_SERVICE_STATE_CHANGED = "com.smarttechnologies.app.blackoverlay.SERVICE_STATE_CHANGED";
	public static final String ACTION_REPORT_STATE_REQUEST = "com.smarttechnologies.app.blackoverlay.REPORT_STATE_REQUEST"; // NEW Constant
	private static final String CHANNEL_ID = "FloatingButtonServiceChannel";
	private static final String TAG = "FloatingButtonService";
	private static final String ACTION_TOGGLE_OVERLAY = "ACTION_TOGGLE_OVERLAY";
	private static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
	private boolean isOverlayActive = false;
	private boolean isStopPending = false; // <<< NEW FLAG: Tracks if a stop was requested while overlay was active
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
		NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Floating Button Service Channel",
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
		if (intent != null && intent.getAction() != null) {
			Log.d(TAG, "Action received from notification: " + intent.getAction());

			switch (intent.getAction()) {
			case ACTION_TOGGLE_OVERLAY:
				if (isOverlayActive) {

					hideBlackOverlay();
				} else {
					startLockScreen();
				}
				break;
			case ACTION_STOP_SERVICE:
				// <<< IMPROVEMENT: Decouple Stop from Destruction >>>
				if (isOverlayActive) {
					Log.d(TAG, "Notification STOP received. Killing active overlay first.");
					isStopPending = true; // Set flag to proceed with shutdown after overlay is killed
					// Call existing method to dismiss the overlay
					hideBlackOverlay();
				} else {
					Log.d(TAG, "Notification STOP received. Overlay inactive. Stopping self immediately.");
					stopSelf(); // Safe to stop immediately
				}
				break;
			}
		} else
			Log.e("FloatingButtonService",
					"Received Intent but action is NULL. Cannot proceed with notification action.");

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

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
		// 1. Overlay State Receiver: Handles START/KILL broadcasts from OverlayActivity
		overlayStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (OverlayActivity.OVERLAY_ACTIVITY_STARTED.equals(intent.getAction())) {
					Log.i(TAG, "OverlayActivity has been STARTED! Hiding floating button.");

					// Sync state
					isOverlayActive = true;

					// UI Updates
					if (floatingView != null)
						floatingView.setVisibility(View.GONE);
					updateNotification();
				} else if (OverlayActivity.OVERLAY_ACTIVITY_KILLED.equals(intent.getAction())) {
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

		LocalBroadcastManager.getInstance(this).registerReceiver(overlayStateReceiver,
				new IntentFilter(OverlayActivity.OVERLAY_ACTIVITY_STARTED));
		LocalBroadcastManager.getInstance(this).registerReceiver(overlayStateReceiver,
				new IntentFilter(OverlayActivity.OVERLAY_ACTIVITY_KILLED));
		Log.d(TAG, "Overlay State Receiver registered");

		// 2. Size Change Receiver: Handles size updates from LookFeelFragment
		sizeChangeReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if ("FLOATING_LOCK_SIZE_CHANGED".equals(intent.getAction())) {
					int newSize = intent.getIntExtra("size", floatingButtonSize);
					updateFloatingButtonSize(newSize);
				}
			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(sizeChangeReceiver,
				new IntentFilter("FLOATING_LOCK_SIZE_CHANGED"));
		Log.d(TAG, "Size Change Receiver registered (Local)");

		// 3. State Request Receiver: Handles requests from MainActivity to report the service state. (NEW)
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
		Log.d(TAG, "State Request Receiver registered (Local)"); // NEW log
	}

	public void startLockScreen() {
		String lockType = appPreferencesManager.getLockType();

		final String VALUE_BLACK_OVERLAY = "black_overlay";
		final String VALUE_PRIVACY_OVERLAY = "privacy_overlay";

		switch (lockType) {

		case VALUE_BLACK_OVERLAY:
			Log.d(TAG, "Applying Full Black Overlay.");
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
	}

	private void hideBlackOverlay2() {
		Log.d(TAG, "Hiding black Overlay");

		// Method 1: Start activity with exit flag
		boolean method1Success = false;
		try {
			Intent intent = new Intent(this, OverlayActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("EXIT", true); // Intentional for compatibility
			startActivity(intent);
			method1Success = true;
			Log.d(TAG, "M1 BlackOverlay Kill Request Sent");
		} catch (Exception e) {
			Log.e(TAG, "M1 BlackOverlay Kill Request Sent Failed: " + e.getMessage());
		}

		// Method 2: Send broadcast ONLY if method 1 failed
		if (!method1Success) {
			try {
				Intent broadcastIntent = new Intent("FINISH_OVERLAY");
				LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
				Log.d(TAG, "M2 BlackOverlay Kill Request Sent");
			} catch (Exception e) {
				Log.e(TAG, "M2 BlackOverlay Kill Request Failed: " + e.getMessage());
			}
		}
	}

	private void showPrivacyOverlay() {
		Log.d(TAG, "PrivacyOverlay Started");
		// Inflate and show the touchable overlay
		if (overlay == null) {
			overlay = LayoutInflater.from(this).inflate(R.layout.privacy_overlay, null);
		}
		// Set up window manager params and add view
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
						| WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
						| WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
						| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				PixelFormat.TRANSLUCENT);
		windowManager.addView(overlay, params);
		// NOTE: Assuming VibrationHelper.vibrateDefault(this) is handled elsewhere
		// VibrationHelper.vibrateDefault(this);

		// Sync state and UI
		if (floatingView != null)
			floatingView.setVisibility(View.GONE);
		isOverlayActive = true;
		updateNotification();
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
		windowManager.addView(floatingView, params);

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
							startLockScreen();
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
				.setContentTitle(getString(R.string.app_name)).setSmallIcon(R.drawable.ic_play_arrow_white_24dp)
				.setContentIntent(openAppPendingIntent) // Clicking notification opens app
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
		PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
				PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Add actions
		// NOTE: Assuming ic_lock_open_white_24dp and ic_lock_white_24dp exist
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