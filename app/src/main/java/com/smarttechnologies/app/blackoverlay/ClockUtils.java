package com.smarttechnologies.app.blackoverlay;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockUtils {

	private Handler handler;
	private Runnable updateTimeRunnable;

	public ClockUtils() {
		handler = new Handler(Looper.getMainLooper());
	}

	public void startUpdatingTime(TextView timeTextView, TextView dateDayTextView) {
		updateTimeRunnable = new Runnable() {
			@Override
			public void run() {
				SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
				String currentTime = timeFormat.format(new Date());

				SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
				String currentDate = dateFormat.format(new Date());

				timeTextView.setText(currentTime);
				dateDayTextView.setText(currentDate);

				// Update content descriptions for accessibility
				timeTextView.setContentDescription("Current time is " + currentTime);
				dateDayTextView.setContentDescription("Today's date is " + currentDate);

				handler.postDelayed(this, 1000);
			}
		};
		handler.post(updateTimeRunnable);
	}

	public void stopUpdatingTime() {
		if (handler != null && updateTimeRunnable != null) {
			handler.removeCallbacks(updateTimeRunnable);
		}
	}
} 