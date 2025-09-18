package com.smarttechnologies.app.blackoverlay;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
* A utility class to manage a global time-keeping timer. It can update
* TextViews and/or push data to a SharedViewModel in a thread-safe manner.
*/
public final class ClockUtils {

	private final Handler handler;
	private boolean isTimerRunning = false;
	private String currentTime;
	private String currentDate;

	// Use thread-safe collections since updates happen on UI thread but modifications can happen from any thread
	private final List<TextView> timeTextViews = new CopyOnWriteArrayList<>();
	private final List<TextView> dateTextViews = new CopyOnWriteArrayList<>();

	@Nullable
	private final SharedViewModel sharedViewModel;

	public ClockUtils(@Nullable SharedViewModel viewModel) {
		handler = new Handler(Looper.getMainLooper());
		this.sharedViewModel = viewModel;
	}

	private final Runnable updateTimeRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				// Get current time and date strings
				SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
				String time = timeFormat.format(new Date());

				SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
				String date = dateFormat.format(new Date());

				// Update the class fields
				currentTime = time;
				currentDate = date;

				// Update the ViewModel if it exists
				if (sharedViewModel != null) {
					sharedViewModel.setCurrentTime(currentTime);
					sharedViewModel.setCurrentDate(currentDate);
				}
			} catch (Exception e) {
				Log.e("ClockUtils", "Error updating time: " + e.getMessage(), e);
			} finally {
				// Reschedule the next run if timer is still running
				if (isTimerRunning) {
					handler.postDelayed(this, 1000);
				}
			}
		}
	};

	/**
	* Starts the time update timer.
	*/
	public void startTimer() {
		if (!isTimerRunning) {
			isTimerRunning = true;
			// Update immediately and then schedule periodic updates
			handler.post(updateTimeRunnable);
		}
	}

	/**
	* Stops the time update timer and clears all registered TextViews.
	*/
	public void stopTimer() {
		if (isTimerRunning) {
			isTimerRunning = false;
			handler.removeCallbacks(updateTimeRunnable);

			currentTime = null;
			currentDate = null;

			// Clear ViewModel values if needed
			if (sharedViewModel != null) {
				sharedViewModel.setCurrentTime(null);
				sharedViewModel.setCurrentDate(null);
			}
		}
	}

	/**
	* Gets the current time string
	*/
	@Nullable
	public String getCurrentTime() {
		return currentTime;
	}

	/**
	* Gets the current date string
	*/
	@Nullable
	public String getCurrentDate() {
		return currentDate;
	}

	/**
	* Checks if the timer is currently running
	*/
	public boolean isTimerRunning() {
		return isTimerRunning;
	}

	/**
	* Force an immediate update of the time and date
	*/
	public void updateImmediately() {
		if (isTimerRunning) {
			handler.removeCallbacks(updateTimeRunnable);
			handler.post(updateTimeRunnable);
		}
	}
}