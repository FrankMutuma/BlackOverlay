package com.smarttechnologies.app.blackoverlay;

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
import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;

public class FloatingButtonService extends Service {

	private WindowManager windowManager;
	private AppPreferencesManager appSettingsManager;
	private ClockUtils clockUtils;
	private BrightnessManager brightnessManager;
	private View floatingView;
	private View blackScreenOverlay;
	private TextView timeTextView;
	private TextView dateDayTextView;
	private static final String CHANNEL_ID = "FloatingButtonServiceChannel";
	private static final int MAX_CLICK_DURATION = 200; // Maximum duration for a click in milliseconds

	public FloatingButtonService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Create the notification channel
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Floating Button Service Channel",
					NotificationManager.IMPORTANCE_DEFAULT);
			NotificationManager manager = getSystemService(NotificationManager.class);
			manager.createNotificationChannel(channel);
		}

		// Create the notification
		Notification notification = new Notification.Builder(this, CHANNEL_ID).setContentTitle("Floating Button")
				.setContentText("Tap to activate black screen").setSmallIcon(R.drawable.ic_play_arrow_white_24dp)
				.build();

		// Start the service as a foreground service
		startForeground(1, notification);

		appSettingsManager = AppPreferencesManager.getInstance(this);

		clockUtils = new ClockUtils();
		brightnessManager=new BrightnessManager(this,getWindow());

		// Inflate the floating button layout
		floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null);

		// Set the layout parameters for the floating button
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

		// Specify the position of the floating button
		params.gravity = Gravity.TOP | Gravity.START;
		params.x = 0;
		params.y = 100;

		// Get the window manager and add the view
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		windowManager.addView(floatingView, params);

		// Find the icon view and set listeners
		Toast.makeText(FloatingButtonService.this, "floating button showing", Toast.LENGTH_LONG).show();

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
						| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

				/* WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS*/, PixelFormat.TRANSLUCENT);

		// Set the display cutout mode
		params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
		// Set to cover entire screen including system bars
		params.gravity = Gravity.TOP | Gravity.START;
		params.x = 0;
		params.y = 0;

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
	}

	private void hideBlackScreen() {
		if (blackScreenOverlay != null) {
			windowManager.removeView(blackScreenOverlay);
			blackScreenOverlay = null;
			floatingView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (floatingView != null) {
			windowManager.removeView(floatingView);
		}
		if (blackScreenOverlay != null) {
			windowManager.removeView(blackScreenOverlay);
		}
		if (clockUtils != null) {
			clockUtils.stopUpdatingTime();
		}
	}
}