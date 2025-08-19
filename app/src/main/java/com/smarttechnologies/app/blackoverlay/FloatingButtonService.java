package com.smarttechnologies.app.blackoverlay;

import android.os.Build;
import android.os.Handler;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;

public class FloatingButtonService extends Service {

	private WindowManager windowManager;
	private AppPreferencesManager appSettingsManager;
	private View floatingView;
	private View blackScreenOverlay;
	private TextView timeTextView;
	private TextView dateDayTextView;
	private Handler handler = new Handler(Looper.getMainLooper());
	private Runnable updateTimeRunnable;
	private static final String CHANNEL_ID = "FloatingButtonServiceChannel";
	private long startClickTime;
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
		appSettingsManager = AppPreferencesManager.getInstance(this);

		// Create the notification channel
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Floating Button Service Channel",
					NotificationManager.IMPORTANCE_DEFAULT);
			NotificationManager manager = getSystemService(NotificationManager.class);
			manager.createNotificationChannel(channel);
		}

		// Create the notification
		Notification notification = new Notification.Builder(this, CHANNEL_ID).setContentTitle("Floating Button")
				.setContentText("Tap to activate black screen").setSmallIcon(R.drawable.ic_play_arrow_white_24dp) // Use your icon
				.build();

		// Start the service as a foreground service
		startForeground(1, notification);

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
		ImageView floatingButtonIcon = floatingView.findViewById(R.id.floating_button_icon);
		Toast.makeText(FloatingButtonService.this, "floating button showing", Toast.LENGTH_LONG).show();

		// Add a combined touch and click listener to the view
		floatingView.setOnTouchListener(new View.OnTouchListener() {
			private int initialX;
			private int initialY;
			private float initialTouchX;
			private float initialTouchY;
			private long startClickTime; // New variable to track tap duration
			private final static int CLICK_ACTION_THRESHOLD = 200; // Maximum duration for a click

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
					// Calculate the new position
					params.x = initialX + (int) (event.getRawX() - initialTouchX);
					params.y = initialY + (int) (event.getRawY() - initialTouchY);
					// Update the view's layout
					windowManager.updateViewLayout(floatingView, params);
					return true;

				case MotionEvent.ACTION_UP:
					long clickDuration = System.currentTimeMillis() - startClickTime;
					// Check if it was a quick tap, not a drag
					if (clickDuration < CLICK_ACTION_THRESHOLD) {
						// It was a tap, so we'll treat it as a click
						if (blackScreenOverlay == null) {

							// Check the user's setting to decide which overlay to show
							if (appSettingsManager.getPreventTouch()) {
								showUntouchableBlackScreen();
							} else {
								showTouchableBlackScreen();
							}

						} else {
							hideBlackScreen();
						}
					}
					// We've handled both drag and click, so we return true
					return true;
				}
				return false;
			}
		});
	}

	private void showUntouchableBlackScreen() {
		// Inflate the untouchable layout
		blackScreenOverlay = LayoutInflater.from(this).inflate(R.layout.black_screen_untouchable_layout, null);
		timeTextView = blackScreenOverlay.findViewById(R.id.overlay_time);
		dateDayTextView = blackScreenOverlay.findViewById(R.id.overlay_date_and_day);

		// Set the layout parameters for a full-screen, non-touchable overlay
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
						| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
						| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				PixelFormat.TRANSLUCENT);

		// Add the view to the window manager
		windowManager.addView(blackScreenOverlay, params);
		//start updating time
		startUpdatingTime();
	}

	private void showTouchableBlackScreen() {
		// Inflate the simple black screen layout
		blackScreenOverlay = LayoutInflater.from(this).inflate(R.layout.black_screen_touchable_layout, null);

		// Set the layout parameters for a full-screen, touchable overlay
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
						| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				PixelFormat.TRANSLUCENT);

		// Add the view to the window manager
		windowManager.addView(blackScreenOverlay, params);
	}

	private void hideBlackScreen() {
		if (blackScreenOverlay != null) {
			windowManager.removeView(blackScreenOverlay);
			blackScreenOverlay = null;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// You can use this method to handle commands if needed
		return START_STICKY; // Makes the service restart if it's killed by the system
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (floatingView != null) {
			windowManager.removeView(floatingView);
			blackScreenOverlay = null;
			//stop updating time
			stopUpdatingTime();
		}
	}

	// New methods to start and stop the clock updates
	private void startUpdatingTime() {
		updateTimeRunnable = new Runnable() {
			@Override
			public void run() {
				// Get the current time and format it
				SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
				String currentTime = timeFormat.format(new Date());

				// Get the current date and day and format it
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
				String currentDate = dateFormat.format(new Date());

				// Update the TextViews
				timeTextView.setText(currentTime);
				dateDayTextView.setText(currentDate);

				// Schedule the next update in one second
				handler.postDelayed(this, 1000);
			}
		};
		handler.post(updateTimeRunnable);
	}

	private void stopUpdatingTime() {
		if (handler != null && updateTimeRunnable != null) {
			handler.removeCallbacks(updateTimeRunnable);
		}
	}

}