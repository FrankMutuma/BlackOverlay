package com.smarttechnologies.app.blackoverlay;

import android.os.Build;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class FloatingButtonService extends Service {

	private WindowManager windowManager;
	private View floatingView;
	private static final String CHANNEL_ID = "FloatingButtonServiceChannel";

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

		// Add a touch listener to the view for dragging
		floatingView.setOnTouchListener(new View.OnTouchListener() {
			private int initialX;
			private int initialY;
			private float initialTouchX;
			private float initialTouchY;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					initialX = params.x;
					initialY = params.y;
					initialTouchX = event.getRawX();
					initialTouchY = event.getRawY();
					return true;
				case MotionEvent.ACTION_MOVE:
					params.x = initialX + (int) (event.getRawX() - initialTouchX);
					params.y = initialY + (int) (event.getRawY() - initialTouchY);
					windowManager.updateViewLayout(floatingView, params);
					return true;
				case MotionEvent.ACTION_UP:
					// Handle a tap-like action here
					return false; // Return false to allow the click listener to be called
				}
				return false;
			}

		});

		// Add a click listener for button action
		floatingView.setOnClickListener(v -> {
			Toast.makeText(FloatingButtonService.this, "Floating button clicked!", Toast.LENGTH_SHORT).show();
			// TODO: Add logic to activate the black screen here
		});

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
		}
	}
}