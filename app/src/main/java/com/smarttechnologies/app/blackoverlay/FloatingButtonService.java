package com.smarttechnologies.app.blackoverlay;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.MotionEvent;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.ImageView;
import androidx.core.view.WindowCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;

public class FloatingButtonService extends Service {
	public static final String ACTION_SERVICE_STATE_CHANGED = "com.yourpackage.SERVICE_STATE_CHANGED";
	private WindowManager windowManager;
	private AppPreferencesManager appSettingsManager;
	private ClockUtils clockUtils;
	private BrightnessManager brightnessManager;
	private BroadcastReceiver sizeChangeReceiver;

	private View floatingView;
	private View blackScreenOverlay;
	private TextView timeTextView;
	private TextView dateDayTextView;
	private static final String CHANNEL_ID = "FloatingButtonServiceChannel";
	private static final int MAX_CLICK_DURATION = 200; // Maximum duration for a click in milliseconds
	// Notification Action Constants
	private static final String ACTION_TOGGLE_OVERLAY = "ACTION_TOGGLE_OVERLAY";
	private static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
	private boolean isOverlayActive = false;
	private int floatingButtonSize;

	public FloatingButtonService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// Create notification channel
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel("FloatingButtonServiceChannel",
					"Floating Button Service Channel", NotificationManager.IMPORTANCE_LOW);
			NotificationManager manager = getSystemService(NotificationManager.class);
			manager.createNotificationChannel(channel);
		}

		// Start as foreground service
		startForeground(1, buildNotification());

		// Get the initial size from preferences
		appSettingsManager = AppPreferencesManager.getInstance(this);
		// Get the initial size from preferences
		floatingButtonSize = appSettingsManager.getFloatingLockSize();
		// Initialize window manager
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		clockUtils = new ClockUtils();
		brightnessManager = new BrightnessManager(this);

		// Register receiver for size changes
		sizeChangeReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if ("FLOATING_LOCK_SIZE_CHANGED".equals(intent.getAction())) {
					int newSize = intent.getIntExtra("size", floatingButtonSize);
					updateFloatingButtonSize(newSize);
				}
			}
		};

		IntentFilter filter = new IntentFilter("FLOATING_LOCK_SIZE_CHANGED");
		registerReceiver(sizeChangeReceiver, filter);

		// Create the floating button
		createFloatingButton();
	}

	private void createFloatingButton() {
		floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null);

		// Set the layout parameters
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				DisplayUtils.dpToPx(this, floatingButtonSize), DisplayUtils.dpToPx(this, floatingButtonSize),
				WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);

		// Set position
		params.gravity = Gravity.TOP | Gravity.START;
		params.x = 0;
		params.y = 100;

		// Add the view to the window manager
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
						//This is a click event
						if (blackScreenOverlay == null) {
							if (appSettingsManager.getPreventTouch()) {
								showUntouchableBlackScreen();
							} else {
								showTouchableBlackScreen();
							}
						} else {
							hideBlackScreen();
						}
					}
					return true;
				}
				return false;
			}
		});
		// Also update the ImageView inside
		ImageView iconView = floatingView.findViewById(R.id.floating_button_icon);
		if (iconView != null) {
			ViewGroup.LayoutParams iconParams = iconView.getLayoutParams();
			iconParams.width = DisplayUtils.dpToPx(this, floatingButtonSize);
			iconParams.height = DisplayUtils.dpToPx(this, floatingButtonSize);
			iconView.setLayoutParams(iconParams);
		}
	}

	//update the size
	private void updateFloatingButtonSize(int newSize) {
		// Only update if the size actually changed
		if (floatingButtonSize == newSize) {
			return;
		}

		floatingButtonSize = newSize;

		if (floatingView != null && windowManager != null) {
			// Update the layout parameters
			WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
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
		PendingIntent openAppPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Create a notification builder
		Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID).setContentTitle("Black Overlay")
				.setSmallIcon(R.drawable.ic_play_arrow_white_24dp).setContentIntent(openAppPendingIntent) // Clicking notification opens app
				.setOngoing(true).setCategory(Notification.CATEGORY_SERVICE)
				.setVisibility(Notification.VISIBILITY_PUBLIC);

		// Set content text based on state
		if (isOverlayActive) {
			builder.setContentText("(Tap to Open)\nOverlay is currently visible");
		} else {
			builder.setContentText("(Tap to Open)\nOverlay is currently hidden");
		}

		// Toggle Overlay Action
		Intent toggleIntent = new Intent(this, FloatingButtonService.class);
		toggleIntent.setAction(ACTION_TOGGLE_OVERLAY);
		PendingIntent togglePendingIntent = PendingIntent.getService(this, 0, toggleIntent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Stop Service Action
		Intent stopIntent = new Intent(this, FloatingButtonService.class);
		stopIntent.setAction(ACTION_STOP_SERVICE);
		PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		// Add actions
		builder.addAction(isOverlayActive ? R.drawable.ic_lock_open_white_24dp : R.drawable.ic_lock_white_24dp,
				isOverlayActive ? "🔓Unlock" : "🔒  Lock", togglePendingIntent);

		builder.addAction(R.drawable.ic_stop_white_24dp, "🚫STOP", stopPendingIntent);

		return builder.build();
	}

	private void updateNotification() {
		Notification notification = buildNotification();
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			notificationManager.notify(1, notification);
		}
	}

	private void vibrate() {

		//... inside your Service or other Context
		Vibrator vibrator;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			// New method for Android 12 (API 31) and above
			VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
			vibrator = vibratorManager.getDefaultVibrator();
		} else {
			// Deprecated method for older versions
			vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		}

		if (vibrator != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
			} else {
				vibrator.vibrate(100);
			}
		}

	}

	private void showUntouchableBlackScreen() {
		floatingView.setVisibility(View.GONE);
		blackScreenOverlay = LayoutInflater.from(this).inflate(R.layout.black_screen_untouchable_layout, null);
		timeTextView = blackScreenOverlay.findViewById(R.id.overlay_time);
		dateDayTextView = blackScreenOverlay.findViewById(R.id.overlay_date_and_day);

		WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
						| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);

		// Set the display cutout mode
		params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
		// Set to cover entire screen including system bars
		params.gravity = Gravity.TOP | Gravity.START;
		params.x = 0;
		params.y = 0;

		brightnessManager.setOverlayParams(params);
		if (brightnessManager.canWriteSystemSettings()) {
			brightnessManager.applyCombinedBrightness();
		} else {
			brightnessManager.applyInAppWindowBrightness();
		}

		// Also set the view itself to be fullscreen
		blackScreenOverlay.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

		blackScreenOverlay.setOnTouchListener(new View.OnTouchListener() {
			private static final int TAP_COUNT_TO_UNLOCK = 3;
			private static final long TAP_TIMEOUT_MS = 300;
			private long lastTapTime = 0;
			private int tapCount = 0;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					long currentTime = System.currentTimeMillis();
					if (currentTime - lastTapTime < TAP_TIMEOUT_MS) {
						tapCount++;
					} else {
						tapCount = 1;
					}
					lastTapTime = currentTime;

					if (tapCount == TAP_COUNT_TO_UNLOCK) {
						vibrate();
						hideBlackScreen();
						tapCount = 0;
						return true;
					}
				}
				return true;
			}
		});

		windowManager.addView(blackScreenOverlay, params);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			blackScreenOverlay.getWindowInsetsController()
					.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

			// Hide system bars
			blackScreenOverlay.getWindowInsetsController().hide(WindowInsets.Type.systemBars());
		}

		clockUtils.startUpdatingTime(timeTextView, dateDayTextView);
		isOverlayActive = true;
		updateNotification();
	}

	private void showTouchableBlackScreen() {
		floatingView.setVisibility(View.GONE);
		blackScreenOverlay = LayoutInflater.from(this).inflate(R.layout.black_screen_touchable_layout, null);

		WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
						| WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
						| WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
						| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				PixelFormat.TRANSLUCENT);

		windowManager.addView(blackScreenOverlay, params);
		isOverlayActive = true;
		updateNotification();
	}

	private void hideBlackScreen() {
		if (blackScreenOverlay != null) {
			brightnessManager.restoreBrightness();
			windowManager.removeView(blackScreenOverlay);
			blackScreenOverlay = null;
			floatingView.setVisibility(View.VISIBLE);
			isOverlayActive = false;
			updateNotification();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// Broadcast that the service has started
		Intent stateIntent = new Intent(ACTION_SERVICE_STATE_CHANGED);
		stateIntent.putExtra("is_running", true);
		LocalBroadcastManager.getInstance(this).sendBroadcast(stateIntent);

		if (intent != null && intent.getAction() != null) {
			switch (intent.getAction()) {
			case ACTION_TOGGLE_OVERLAY:
				// Toggle the overlay state
				if (isOverlayActive) {
					hideBlackScreen();
					Toast.makeText(this, "Overlay hidden", Toast.LENGTH_SHORT).show();
				} else {
					if (appSettingsManager.getPreventTouch()) {
						showUntouchableBlackScreen();
					} else {
						showTouchableBlackScreen();
					}
					Toast.makeText(this, "Overlay shown", Toast.LENGTH_SHORT).show();
				}
				break;

			case ACTION_STOP_SERVICE:
				// User clicked "Stop Service" - stop everything
				stopSelf();
				break;
			}
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Broadcast that the service has stopped
		Intent stateIntent = new Intent(ACTION_SERVICE_STATE_CHANGED);
		stateIntent.putExtra("is_running", false);
		LocalBroadcastManager.getInstance(this).sendBroadcast(stateIntent);

		// Unregister the receiver
		if (sizeChangeReceiver != null) {
			unregisterReceiver(sizeChangeReceiver);
		}

		// Remove views
		if (floatingView != null && windowManager != null) {
			windowManager.removeView(floatingView);
		}

		// Stop being a foreground service
		stopForeground(STOP_FOREGROUND_REMOVE);

		Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
	}

}